package org.sitmun.authorization.client.service;

import static org.sitmun.infrastructure.security.core.SecurityConstants.*;
import static org.sitmun.infrastructure.security.core.SecurityRole.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.sitmun.authorization.access.UserApplicationAccessPolicy;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.application.ApplicationRepository;
import org.sitmun.domain.background.Background;
import org.sitmun.domain.background.BackgroundRepository;
import org.sitmun.domain.cartography.Cartography;
import org.sitmun.domain.cartography.CartographyBlockPolicy;
import org.sitmun.domain.cartography.CartographyRepository;
import org.sitmun.domain.cartography.permission.CartographyPermission;
import org.sitmun.domain.cartography.permission.CartographyPermissionRepository;
import org.sitmun.domain.configuration.ConfigurationParameter;
import org.sitmun.domain.configuration.ConfigurationParameterRepository;
import org.sitmun.domain.role.Role;
import org.sitmun.domain.role.RoleRepository;
import org.sitmun.domain.service.ServiceBlockPolicy;
import org.sitmun.domain.task.Task;
import org.sitmun.domain.task.TaskRepository;
import org.sitmun.domain.territory.Territory;
import org.sitmun.domain.territory.TerritoryRepository;
import org.sitmun.domain.tree.Tree;
import org.sitmun.domain.tree.TreeRepository;
import org.sitmun.domain.tree.node.TreeNode;
import org.sitmun.domain.tree.node.TreeNodeRepository;
import org.sitmun.infrastructure.persistence.type.i18n.TranslationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class AuthorizationService {

  // TODO: Fix cartesian product via @EntityGraph on multiple collections.
  //   Fixed in 29cdef6f, 6dcb5bbd: replaced @EntityGraph with @BatchSize in
  //   CartographyPermission and CartographyPermissionRepository.
  //   Other locations are still potentially affected, ex.:
  //   - TreeRepository.findByAppAndRoles          (availableRoles + availableApplications)
  //   - CartographyRepository.findById            (permissions, availabilities, styles, filters…)
  //   - CartographyRepository.findAll             (service, styles…)
  //   - TaskRepository.findByRolesAndTerritory    (roles, ui, type)

  private final TerritoryRepository territoryRepository;
  private final ApplicationRepository applicationRepository;
  private final BackgroundRepository backgroundRepository;
  private final RoleRepository roleRepository;
  private final CartographyPermissionRepository cartographyPermissionRepository;
  private final CartographyRepository cartographyRepository;
  private final ConfigurationParameterRepository configurationParameterRepository;
  private final TaskRepository taskRepository;
  private final TreeRepository treeRepository;
  private final TreeNodeRepository treeNodeRepository;
  private final TranslationService translationService;
  private final UserApplicationAccessPolicy userApplicationAccessPolicy;

  public AuthorizationService(
      ApplicationRepository applicationRepository,
      TerritoryRepository territoryRepository,
      RoleRepository roleRepository,
      ConfigurationParameterRepository configurationParameterRepository,
      CartographyPermissionRepository cartographyPermissionRepository,
      CartographyRepository cartographyRepository,
      TaskRepository taskRepository,
      BackgroundRepository backgroundRepository,
      TreeRepository treeRepository,
      TreeNodeRepository treeNodeRepository,
      TranslationService translationService,
      UserApplicationAccessPolicy userApplicationAccessPolicy) {
    this.applicationRepository = applicationRepository;
    this.territoryRepository = territoryRepository;
    this.roleRepository = roleRepository;
    this.configurationParameterRepository = configurationParameterRepository;
    this.cartographyPermissionRepository = cartographyPermissionRepository;
    this.cartographyRepository = cartographyRepository;
    this.taskRepository = taskRepository;
    this.backgroundRepository = backgroundRepository;
    this.treeRepository = treeRepository;
    this.treeNodeRepository = treeNodeRepository;
    this.translationService = translationService;
    this.userApplicationAccessPolicy = userApplicationAccessPolicy;
  }

  /**
   * Account-level gate for list/dashboard client config endpoints. Throws when the user account is
   * blocked ({@link UserApplicationAccessPolicy#mayUseClientConfigEndpoints}).
   */
  public void ensureMayUseClientConfigEndpoints(String username) {
    if (!userApplicationAccessPolicy.mayUseClientConfigEndpoints(username)) {
      throw new AccessDeniedException("Access denied: user account is blocked");
    }
  }

  /**
   * App-level gate for territories, profile, and other app-scoped client config endpoints. Throws
   * when the public principal tries to access a private application. Must be called after {@link
   * #ensureMayUseClientConfigEndpoints} so that blocked accounts are already rejected.
   */
  public void ensureMayAccessApplication(Integer appId, String username) {
    if (!userApplicationAccessPolicy.mayAccessApplication(appId, username)) {
      throw new AccessDeniedException("Access denied to application");
    }
  }

  /**
   * The list of applications for a user. The logic is as follows:
   *
   * <ul>
   *   <li>From the application, we can discover the roles (ROLE).
   *   <li>We match all user configuration where (USER, *, ROLE, *)
   *   <li>For public users, filter out private applications
   * </ul>
   */
  public Page<Application> findApplicationsByUser(String username, Pageable pageable) {
    Page<Application> page;
    if (isPublic() || isPublicPrincipal(username)) {
      page = applicationRepository.findByPublicUser(username, pageable);
    } else {
      page = applicationRepository.findByUser(username, pageable);
    }
    return page;
  }

  public Page<Application> findApplicationsByUser(
      String username, String keywords, Pageable pageable) {
    if (keywords == null || keywords.trim().length() < 2) {
      return findApplicationsByUser(username, pageable);
    }
    String normalizedKeywords = keywords.trim();
    if (isPublic() || isPublicPrincipal(username)) {
      return applicationRepository.findByPublicUserAndKeywords(
          username, normalizedKeywords, pageable);
    }
    return applicationRepository.findByUserAndKeywords(username, normalizedKeywords, pageable);
  }

  /**
   * The list of territories for a user. The logic is as follows:
   *
   * <ul>
   *   <li>We match all user configuration where (USER, *, ROLE, *)
   *   <li>From the territory, we can discover the roles (USER, TERRITORY, *, FALSE).
   *   <li>When a user configuration is (USER, TERRITORY, *, TRUE) we consider included the
   *       application if {@link Application#getAccessParentTerritory()} is `true`
   *   <li>When a parent territory matches (USER, TERRITORY-PARENT, *, TRUE) we consider included
   *       the application if {@link Application#getAccessChildrenTerritory()} is `true`
   *   <li>And ensure that the territory included is related to the application of the role
   * </ul>
   */
  public Page<Territory> findTerritoriesByUser(String username, Pageable pageable) {
    Page<Territory> page;

    if (isPublic() || isPublicPrincipal(username)) {
      page = territoryRepository.findByPublicUser(username, pageable);
    } else {
      page = territoryRepository.findByRestrictedUser(username, pageable);
    }
    return page;
  }

  public Page<Territory> findTerritoriesByUser(
      String username, String keywords, Pageable pageable) {
    if (keywords == null || keywords.trim().length() < 2) {
      return findTerritoriesByUser(username, pageable);
    }
    String normalizedKeywords = keywords.trim();
    if (isPublic() || isPublicPrincipal(username)) {
      return territoryRepository.findByPublicUserAndKeywords(
          username, normalizedKeywords, pageable);
    }
    return territoryRepository.findByRestrictedUserAndKeywords(
        username, normalizedKeywords, pageable);
  }

  /**
   * Get the list of territories for the user in a given application. The logic is as follows:
   *
   * <ul>
   *   <li>From the application, we can discover the roles (ROLE).
   *   <li>We match all user configuration where (USER, territory, ROLE, *)
   *   <li>When a user configuration is (USER, territory, ROLE, TRUE) we consider included the
   *       territory if {@link Application#getAccessParentTerritory()} is `true`
   * </ul>
   */
  public Page<Territory> findTerritoriesByUserAndApplication(
      String username, Integer appId, Pageable pageable) {
    Page<Territory> page;
    if (isPublic() || isPublicPrincipal(username)) {
      page = territoryRepository.findByPublicUserAndApplication(username, appId, pageable);
    } else {
      page = territoryRepository.findByRestrictedUserAndApplication(username, appId, pageable);
    }
    return page;
  }

  /**
   * Get the list of applications for the user in a given territory. The logic is as follows:
   *
   * <ul>
   *   <li>We match all user configurations where (USER, TERRITORY, *, false)
   *   <li>When a user configuration is (USER, TERRITORY, *, TRUE) we consider included the
   *       territory if {@link Application#getAccessParentTerritory()} is `true`
   *   <li>For public users, filter out private applications
   * </ul>
   */
  public Page<Application> findApplicationsByUserAndTerritory(
      String username, Integer territoryId, Pageable pageable) {
    Page<Application> page;
    if (isPublic() || isPublicPrincipal(username)) {
      page = applicationRepository.findByPublicUserAndTerritory(username, territoryId, pageable);
    } else {
      page =
          applicationRepository.findByRestrictedUserAndTerritory(username, territoryId, pageable);
    }
    return page;
  }

  /** Finds a single application accessible to {@code username} in the given app/territory pair. */
  public Optional<Application> findApplicationByUserApplicationAndTerritory(
      String username, Integer appId, Integer territoryId) {
    Optional<Application> application;
    if (isPublic() || isPublicPrincipal(username)) {
      application =
          applicationRepository.findByPublicUserApplicationAndTerritory(
              username, appId, territoryId);
    } else {
      application =
          applicationRepository.findByRestrictedUserApplicationAndTerritory(
              username, appId, territoryId);
    }
    return application;
  }

  /**
   * Enrich applications with territory count information in bulk.
   *
   * @param applications the applications to enrich
   * @param username the current user
   * @return map of application ID to territory count
   */
  public Map<Integer, Integer> getTerritoryCountsByApplications(
      List<Application> applications, String username) {
    Map<Integer, Integer> counts = new java.util.HashMap<>();
    for (Application app : applications) {
      Pageable unpaged = Pageable.unpaged();
      Page<Territory> territories =
          findTerritoriesByUserAndApplication(username, app.getId(), unpaged);
      counts.put(app.getId(), (int) territories.getTotalElements());
    }
    return counts;
  }

  /**
   * Find dashboard suggestions (applications and territories) matching keywords.
   *
   * @param username the username
   * @param keywords search keywords
   * @param maxResults maximum results per category
   * @return map with "applications" and "territories" lists
   */
  public Map<String, List<?>> findDashboardSuggestions(
      String username, String keywords, int maxResults) {
    Map<String, List<?>> result = new java.util.HashMap<>();

    if (keywords == null || keywords.trim().length() < 2) {
      result.put("applications", List.of());
      result.put("territories", List.of());
      return result;
    }

    String normalizedKeywords = keywords.trim();

    Pageable appPageable = PageRequest.of(0, maxResults);
    Page<Application> apps = findApplicationsByUser(username, normalizedKeywords, appPageable);
    List<Application> filteredApps = apps.getContent();

    Pageable terrPageable = PageRequest.of(0, maxResults);
    Page<Territory> terrs = findTerritoriesByUser(username, normalizedKeywords, terrPageable);
    List<Territory> filteredTerrs = terrs.getContent();

    result.put("applications", filteredApps);
    result.put("territories", filteredTerrs);
    return result;
  }

  @Transactional(readOnly = true)
  public Optional<Profile> createProfile(ProfileContext context) {
    return buildProfile(context).map(this::pruneProfile);
  }

  @NotNull
  private Optional<Profile> buildProfile(ProfileContext context) {
    Optional<Application> application =
        findApplicationByUserApplicationAndTerritory(
            context.getUsername(), context.getAppId(), context.getTerritoryId());
    if (application.isEmpty()) {
      return Optional.empty();
    }
    application.ifPresent(translationService::updateInternationalization);

    Optional<Territory> territory = territoryRepository.findById(context.getTerritoryId());
    if (territory.isEmpty()) {
      return Optional.empty();
    }
    territory.ifPresent(translationService::updateInternationalization);

    List<Role> roles =
        roleRepository.findRolesByApplicationAndUserAndTerritory(
            context.getUsername(), context.getAppId(), context.getTerritoryId());
    roles.forEach(translationService::updateInternationalization);

    List<Background> backgrounds =
        backgroundRepository.findActiveByApplication(context.getAppId()).stream()
            .map(objects -> (Background) objects[1])
            .toList();
    backgrounds.forEach(translationService::updateInternationalization);

    List<CartographyPermission> cartographyPermissions =
        new ArrayList<>(
            cartographyPermissionRepository
                .findByRolesAndTerritory(roles, context.getTerritoryId())
                .stream()
                .filter(cp -> cp.getMembers() != null)
                .filter(cp -> cp.getRoles() != null)
                .toList());
    cartographyPermissions.forEach(translationService::updateInternationalization);

    List<Cartography> layers =
        new ArrayList<>(
            cartographyRepository.findByRolesAndTerritory(roles, context.getTerritoryId()).stream()
                .filter(CartographyBlockPolicy::isNotDirectlyBlocked)
                .filter(l -> ServiceBlockPolicy.isAccessibleInClientProfile(l.getService()))
                .toList());
    layers.forEach(translationService::updateInternationalization);

    List<Task> tasks = taskRepository.findByRolesAndTerritory(roles, context.getTerritoryId());
    tasks =
        tasks.stream()
            .filter(t -> t.getRoles() != null)
            .filter(t -> ServiceBlockPolicy.isAccessibleInClientProfileOrNull(t.getService()))
            .toList();
    tasks.forEach(translationService::updateInternationalization);

    List<Tree> trees = treeRepository.findByAppAndRoles(context.getAppId(), roles);
    trees =
        trees.stream()
            .filter(t -> t.getAvailableRoles() != null)
            .filter(t -> t.getAvailableApplications() != null)
            .toList();
    trees.forEach(translationService::updateInternationalization);

    List<TreeNode> nodes = treeNodeRepository.findByTrees(trees);
    nodes.forEach(translationService::updateInternationalization);

    Map<Tree, List<TreeNode>> treeNodes =
        nodes.stream().collect(Collectors.groupingBy(TreeNode::getTree));

    List<ConfigurationParameter> global = List.copyOf(configurationParameterRepository.findAll());

    List<org.sitmun.domain.service.Service> services = new ArrayList<>();
    layers.forEach(layer -> services.add(layer.getService()));
    tasks.forEach(task -> services.add(task.getService()));

    // Add situation-map group, layers, and services if application has one
    CartographyPermission situationMap = application.get().getSituationMap();
    if (situationMap != null) {
      translationService.updateInternationalization(situationMap);
      // Add situation-map group if not already present
      if (!cartographyPermissions.contains(situationMap)) {
        cartographyPermissions.add(situationMap);
      }
      // Add situation-map layers (members) if not already present
      if (situationMap.getMembers() != null) {
        Set<Integer> existingLayerIds =
            layers.stream().map(Cartography::getId).collect(Collectors.toUnmodifiableSet());
        List<Cartography> situationMapLayers =
            situationMap.getMembers().stream()
                .filter(member -> situationMapMemberAddsToProfileLayers(member, existingLayerIds))
                .toList();
        situationMapLayers.forEach(translationService::updateInternationalization);
        layers.addAll(situationMapLayers);
        // Add services from situation-map layers
        situationMapLayers.stream()
            .map(Cartography::getService)
            .filter(Objects::nonNull)
            .forEach(services::add);
      }
    }

    List<org.sitmun.domain.service.Service> filteredServices =
        services.stream()
            .filter(Objects::nonNull)
            .filter(ServiceBlockPolicy::isAccessibleInClientProfile)
            .filter(distinctByKey(org.sitmun.domain.service.Service::getId))
            .toList();

    return Optional.of(
        Profile.builder()
            .application(application.get())
            .territory(territory.get())
            .backgrounds(backgrounds)
            .groups(cartographyPermissions)
            .layers(layers)
            .tasks(tasks)
            .services(filteredServices)
            .trees(trees)
            .treeNodes(treeNodes)
            .context(context)
            .global(global)
            .build());
  }

  private List<TreeNode> pruneNodes(List<TreeNode> nodes, Integer pivotNode) {
    return nodes.stream()
        .filter(Objects::nonNull)
        .filter(
            node ->
                Objects.equals(node.getId(), pivotNode)
                    || Objects.equals(node.getParentId(), pivotNode))
        .toList();
  }

  /**
   * Situation-map cartographies merged into the profile layer list (not blocked, not duplicate).
   */
  private static boolean situationMapMemberAddsToProfileLayers(
      Cartography member, Set<Integer> existingLayerIds) {
    return CartographyBlockPolicy.isNotDirectlyBlocked(member)
        && !existingLayerIds.contains(member.getId())
        && ServiceBlockPolicy.isAccessibleInClientProfile(member.getService());
  }

  private static boolean treeHasAnyNodes(Tree tree, Map<Tree, List<TreeNode>> treeNodes) {
    return !treeNodes.getOrDefault(tree, List.of()).isEmpty();
  }

  private boolean layerRetainedInPrunedProfile(
      Cartography layer,
      Set<Integer> nodeLayerIds,
      Set<Integer> taskLayerIds,
      Set<Integer> backgroundLayerIds,
      Set<Integer> situationMapLayerIds) {
    boolean belongsToNode = nodeLayerIds.contains(layer.getId());
    boolean belongsToTask = taskLayerIds.contains(layer.getId());
    boolean belongsToBackground = backgroundLayerIds.contains(layer.getId());
    boolean belongsToSituationMap = situationMapLayerIds.contains(layer.getId());
    boolean retained =
        belongsToNode || belongsToTask || belongsToBackground || belongsToSituationMap;
    log.info(
        "Layer {} belongs to node: {}, task: {}, background: {}, situation-map: {} => remove: {}",
        layer.getId(),
        belongsToNode,
        belongsToTask,
        belongsToBackground,
        belongsToSituationMap,
        !retained);
    return retained;
  }

  private static boolean treeNodeCartographyStillInProfile(
      TreeNode node, Set<Integer> remainingLayerIds) {
    return node.getCartography() == null
        || remainingLayerIds.contains(node.getCartography().getId());
  }

  private CartographyPermission groupWithMembersRestrictedToLayerIds(
      CartographyPermission group, Set<Integer> remainingLayerIds) {
    if (group.getMembers() == null) {
      return group;
    }
    Set<Cartography> filteredMembers =
        group.getMembers().stream()
            .filter(member -> remainingLayerIds.contains(member.getId()))
            .collect(Collectors.toCollection(LinkedHashSet::new));
    return group.toBuilder().members(filteredMembers).build();
  }

  private boolean groupRetainedInClientProfile(
      CartographyPermission permission, List<Background> backgrounds, Integer situationMapId) {
    boolean belongsToBackground =
        backgrounds.stream()
            .flatMap(background -> Optional.ofNullable(background.getCartographyGroup()).stream())
            .anyMatch(group -> Objects.equals(group.getId(), permission.getId()));
    boolean isSituationMap = Objects.equals(permission.getId(), situationMapId);
    boolean retained = belongsToBackground || isSituationMap;
    log.info(
        "Group {} belongs to background: {}, is situation-map: {} => remove: {}",
        permission.getId(),
        belongsToBackground,
        isSituationMap,
        !retained);
    return retained;
  }

  private boolean serviceRetainedInPrunedProfile(
      org.sitmun.domain.service.Service service,
      Set<Integer> taskServiceIds,
      Set<Integer> nodeServiceIds,
      Set<Integer> layerServiceIds) {
    boolean belongsToTask = taskServiceIds.contains(service.getId());
    boolean belongsToNode = nodeServiceIds.contains(service.getId());
    boolean belongsToLayer = layerServiceIds.contains(service.getId());
    boolean retained = belongsToTask || belongsToNode || belongsToLayer;
    log.info(
        "Service {} belongs to task: {}, node: {}, layer: {} => remove: {}",
        service.getId(),
        belongsToTask,
        belongsToNode,
        belongsToLayer,
        !retained);
    return retained;
  }

  /**
   * Replaces pruned layers, trees, tree-node map, groups, and services on {@code profile}. Input
   * collections on the profile are treated as read-only snapshots (no {@code removeIf}, {@code
   * setMembers}, or in-place map edits).
   */
  private Profile pruneProfile(Profile profile) {

    Map<Tree, List<TreeNode>> treeNodes = new LinkedHashMap<>();
    profile.getTreeNodes().forEach((tree, nodes) -> treeNodes.put(tree, List.copyOf(nodes)));

    treeNodes.replaceAll(
        (tree, nodes) -> TreeNodeVisibilityPolicy.filterVisibleInClientProfile(nodes));

    if (profile.getContext().getNodeSectionBehaviour().nodePageMode()) {

      Integer pivotNode = profile.getContext().getNodeId();

      for (Map.Entry<Tree, List<TreeNode>> entry : new ArrayList<>(treeNodes.entrySet())) {
        Tree tree = entry.getKey();
        List<TreeNode> nodes = entry.getValue();
        Integer size = nodes.size();
        List<TreeNode> prunedNodes = pruneNodes(nodes, pivotNode);
        if (pivotNode == null && prunedNodes.size() == 1) {
          prunedNodes = pruneNodes(nodes, prunedNodes.get(0).getId());
        }
        log.info(
            "Pruned {} nodes to {} nodes using as pivot {}", size, prunedNodes.size(), pivotNode);
        treeNodes.put(tree, prunedNodes);
      }
    }

    List<Tree> treesAfterPivot =
        profile.getTrees().stream().filter(tree -> treeHasAnyNodes(tree, treeNodes)).toList();

    // Prune cartography layers that either:
    // - Do not belong to a node
    // - Do not belong to a task
    // - Do not belong to a background
    // - Do not belong to the situation-map

    CartographyPermission situationMap = profile.getApplication().getSituationMap();
    Set<Integer> situationMapLayerIds =
        situationMap != null && situationMap.getMembers() != null
            ? situationMap.getMembers().stream()
                .map(Cartography::getId)
                .collect(Collectors.toUnmodifiableSet())
            : Collections.emptySet();

    Set<Integer> nodeLayerIds =
        treeNodes.values().stream()
            .flatMap(Collection::stream)
            .map(TreeNode::getCartography)
            .filter(Objects::nonNull)
            .map(Cartography::getId)
            .collect(Collectors.toUnmodifiableSet());

    Set<Integer> taskLayerIds =
        profile.getTasks().stream()
            .map(Task::getCartography)
            .filter(Objects::nonNull)
            .map(Cartography::getId)
            .collect(Collectors.toUnmodifiableSet());

    Set<Integer> backgroundLayerIds =
        profile.getBackgrounds().stream()
            .flatMap(
                background ->
                    Optional.ofNullable(background.getCartographyGroup())
                        .map(CartographyPermission::getMembers)
                        .orElseGet(Collections::emptySet)
                        .stream())
            .map(Cartography::getId)
            .collect(Collectors.toUnmodifiableSet());

    List<Cartography> layersFiltered =
        profile.getLayers().stream()
            .filter(
                layer ->
                    layerRetainedInPrunedProfile(
                        layer,
                        nodeLayerIds,
                        taskLayerIds,
                        backgroundLayerIds,
                        situationMapLayerIds))
            .toList();

    // Prune groups that are not related to backgrounds or situation-map
    Integer situationMapId = situationMap != null ? situationMap.getId() : null;

    Set<Integer> remainingLayerIds =
        layersFiltered.stream().map(Cartography::getId).collect(Collectors.toUnmodifiableSet());

    Map<Tree, List<TreeNode>> treeNodesFiltered = new LinkedHashMap<>();
    treeNodes.forEach(
        (tree, nodes) ->
            treeNodesFiltered.put(
                tree,
                nodes.stream()
                    .filter(node -> treeNodeCartographyStillInProfile(node, remainingLayerIds))
                    .toList()));

    List<Tree> treesFiltered =
        treesAfterPivot.stream().filter(tree -> treeHasAnyNodes(tree, treeNodesFiltered)).toList();

    List<CartographyPermission> groupsWithFilteredMembers =
        profile.getGroups().stream()
            .map(group -> groupWithMembersRestrictedToLayerIds(group, remainingLayerIds))
            .toList();

    List<CartographyPermission> groupsFiltered =
        groupsWithFilteredMembers.stream()
            .filter(
                permission ->
                    groupRetainedInClientProfile(
                        permission, profile.getBackgrounds(), situationMapId))
            .toList();

    Set<Integer> taskServiceIds =
        profile.getTasks().stream()
            .map(Task::getService)
            .filter(Objects::nonNull)
            .map(org.sitmun.domain.service.Service::getId)
            .collect(Collectors.toUnmodifiableSet());

    Set<Integer> nodeServiceIds =
        treeNodesFiltered.values().stream()
            .flatMap(Collection::stream)
            .map(TreeNode::getCartography)
            .filter(Objects::nonNull)
            .map(Cartography::getService)
            .filter(Objects::nonNull)
            .map(org.sitmun.domain.service.Service::getId)
            .collect(Collectors.toUnmodifiableSet());

    Set<Integer> layerServiceIds =
        layersFiltered.stream()
            .map(Cartography::getService)
            .filter(Objects::nonNull)
            .map(org.sitmun.domain.service.Service::getId)
            .collect(Collectors.toUnmodifiableSet());

    List<org.sitmun.domain.service.Service> servicesFiltered =
        profile.getServices().stream()
            .filter(
                service ->
                    serviceRetainedInPrunedProfile(
                        service, taskServiceIds, nodeServiceIds, layerServiceIds))
            .toList();

    profile.setTreeNodes(Map.copyOf(treeNodesFiltered));
    profile.setTrees(treesFiltered);
    profile.setLayers(layersFiltered);
    profile.setGroups(groupsFiltered);
    profile.setServices(servicesFiltered);
    return profile;
  }

  public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
    Set<Object> seen = ConcurrentHashMap.newKeySet();
    return t -> seen.add(keyExtractor.apply(t));
  }
}
