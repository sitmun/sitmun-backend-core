package org.sitmun.authorization.service;

import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.application.ApplicationRepository;
import org.sitmun.domain.background.Background;
import org.sitmun.domain.background.BackgroundRepository;
import org.sitmun.domain.cartography.Cartography;
import org.sitmun.domain.cartography.CartographyRepository;
import org.sitmun.domain.cartography.permission.CartographyPermission;
import org.sitmun.domain.cartography.permission.CartographyPermissionRepository;
import org.sitmun.domain.configuration.ConfigurationParameter;
import org.sitmun.domain.configuration.ConfigurationParameterRepository;
import org.sitmun.domain.role.Role;
import org.sitmun.domain.role.RoleRepository;
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
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AuthorizationService {

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

  public AuthorizationService(ApplicationRepository applicationRepository,
                              TerritoryRepository territoryRepository, RoleRepository roleRepository,
                              ConfigurationParameterRepository configurationParameterRepository,
                              CartographyPermissionRepository cartographyPermissionRepository,
                              CartographyRepository cartographyRepository,
                              TaskRepository taskRepository,
                              BackgroundRepository backgroundRepository, TreeRepository treeRepository,
                              TreeNodeRepository treeNodeRepository,
                              TranslationService translationService) {
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
  }

  /**
   * The list of applications for a user.
   * The logic is as follows:
   * <ul>
   *   <li>From the application, we can discover the roles (ROLE).</li>
   *   <li>We match all user configuration where (USER, *, ROLE, *)</li>
   * </ul>
   */
  public Page<Application> findApplicationsByUser(String username, Pageable pageable) {
    return applicationRepository.findByUser(username, pageable);
  }

  /**
   * The list of territories for a user.
   * The logic is as follows:
   * <ul>
   *   <li>We match all user configuration where (USER, *, ROLE, *)</li>
   *   <li>From the territory, we can discover the roles (USER, TERRITORY, *, FALSE).</li>
   *   <li>When a user configuration is (USER, TERRITORY, *, TRUE) we consider included the application if {@link Application#getAccessParentTerritory()} is `true`</li>
   *   <li>When a parent territory matches (USER, TERRITORY-PARENT, *, TRUE) we consider included the application if {@link Application#getAccessChildrenTerritory()} ()} is `true`</li>
   *   <li>And ensure that the territory included is related to the application of the role</li>
   * </ul>
   */
  public Page<Territory> findTerritoriesByUser(String username, Pageable pageable) {
    return territoryRepository.findByUser(username, pageable);
  }


  /**
   * Get the list of territories for the user in a given application.
   * The logic is as follows:
   * <ul>
   *   <li>From the application, we can discover the roles (ROLE).</li>
   *   <li>We match all user configuration where (USER, territory, ROLE, *)</li>
   *   <li>When a user configuration is (USER, territory, ROLE, TRUE) we consider included the territory if {@link Application#getAccessParentTerritory()} is `true`</li>
   * </ul>
   */
  public Page<Territory> findTerritoriesByUserAndApplication(String username, Integer appId, Pageable pageable) {
    return territoryRepository.findByUserAndApplication(username, appId, pageable);
  }

  /**
   * Get the list of applications for the user in a given territory.
   * The logic is as follows:
   * <ul>
   *   <li>We match all user configurations where (USER, TERRITORY, *, false)</li>
   *   <li>When a user configuration is (USER, TERRITORY, *, TRUE) we consider included the territory if {@link Application#getAccessParentTerritory()} is `true`</li>
   * </ul>
   */
  public Page<Application> findApplicationsByUserAndTerritory(String username, Integer territoryId, Pageable pageable) {
    return applicationRepository.findByUserAndTerritory(username, territoryId, pageable);
  }

  /**
   * Refina la lista de aplicaciones restringiendo a una única aplicación.
   */
  public Optional<Application> findApplicationByIdAndUserAndTerritory(String username, Integer appId, Integer territoryId) {
    return applicationRepository.findByIdAndUserAndTerritory(username, appId, territoryId);
  }

  public List<Role> findRolesByApplicationAndUserAndTerritory(String username, Integer appId, Integer territoryId) {
    return roleRepository.findRolesByApplicationAndUserAndTerritory(username, appId, territoryId);
  }

  public Optional<Profile> createProfile(ProfileContext context) {
    return buildProfile(context).map(this::pruneProfile);
  }

  @NotNull
  private Optional<Profile> buildProfile(ProfileContext context) {
    Optional<Application> application = findApplicationByIdAndUserAndTerritory(context.getUsername(), context.getAppId(), context.getTerritoryId());
    if (application.isEmpty()) {
      return Optional.empty();
    }
    Optional<Territory> territory = territoryRepository.findById(context.getTerritoryId());
    if (territory.isEmpty()) {
      return Optional.empty();
    }

    List<Role> roles = roleRepository.findRolesByApplicationAndUserAndTerritory(context.getUsername(), context.getAppId(), context.getTerritoryId());
    roles.forEach(translationService::updateInternationalization);

    List<Background> backgrounds = backgroundRepository.findActiveByApplication(context.getAppId()).stream().map(objects -> (Background) objects[1]).collect(Collectors.toList());
    backgrounds.forEach(translationService::updateInternationalization);

    List<CartographyPermission> cartographyPermissions = cartographyPermissionRepository.findByRolesAndTerritory(roles, context.getTerritoryId());
    cartographyPermissions.forEach(translationService::updateInternationalization);

    List<Cartography> layers = cartographyRepository.findByRolesAndTerritory(roles, context.getTerritoryId());
    layers.forEach(translationService::updateInternationalization);

    List<Task> tasks = taskRepository.findByRolesAndTerritory(roles, context.getTerritoryId());
    tasks.forEach(translationService::updateInternationalization);

    List<Tree> trees = treeRepository.findByAppAndRoles(context.getAppId(), roles);
    trees.forEach(translationService::updateInternationalization);

    List<TreeNode> nodes = treeNodeRepository.findByTrees(trees);
    nodes.forEach(translationService::updateInternationalization);

    Map<Tree, List<TreeNode>> treeNodes = nodes.stream().collect(Collectors.groupingBy(TreeNode::getTree));

    List<ConfigurationParameter> global = ImmutableList.copyOf(configurationParameterRepository.findAll());

    List<org.sitmun.domain.service.Service> services = new ArrayList<>();
    layers.forEach(layer -> services.add(layer.getService()));
    tasks.forEach(task -> services.add(task.getService()));
    List<org.sitmun.domain.service.Service> filteredServices = services.stream().filter(Objects::nonNull).filter(distinctByKey(org.sitmun.domain.service.Service::getId)).collect(Collectors.toList());

    return Optional.of(Profile.builder()
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

  private Profile pruneProfile(Profile profile) {

    if (profile.getContext().getNodeSectionBehaviour().nodePageMode()) {

      Integer pivotNode = profile.getContext().getNodeId();

      profile.getTreeNodes().forEach((tree, nodes) -> {
        Integer size = nodes.size();
        List<TreeNode> prunedNodes = nodes.stream()
          .filter(node -> node != null && (Objects.equals(node.getId(), pivotNode) || Objects.equals(node.getParentId(), pivotNode)))
          .collect(Collectors.toList());
        log.info("Pruned {} nodes to {} nodes using as pivot {}", size, prunedNodes.size(), pivotNode);
        profile.getTreeNodes().put(tree, prunedNodes);
      });
    }

    profile.setTrees(profile.getTrees().stream()
      .filter(tree -> !profile.getTreeNodes().get(tree).isEmpty())
      .collect(Collectors.toList()));

    // Prune cartography layers that either:
    // - Do not belong to a node
    // - Do not belong to a task
    // - Do not belong to a background

    profile.getLayers().removeIf(layer -> {
      boolean belongsToNode = profile.getTreeNodes().values().stream()
        .flatMap(Collection::stream)
        .map(TreeNode::getCartography)
        .filter(Objects::nonNull)
        .map(Cartography::getId)
        .anyMatch(cartographyId -> Objects.equals(layer.getId(), cartographyId));

      boolean belongsToTask = profile.getTasks().stream()
        .map(Task::getCartography)
        .filter(Objects::nonNull)
        .map(Cartography::getId)
        .anyMatch(cartographyId -> Objects.equals(cartographyId, layer.getId()));

      boolean belongsToBackground = profile.getBackgrounds().stream()
        .flatMap(background -> Optional.ofNullable(background.getCartographyGroup())
          .map(CartographyPermission::getMembers)
          .orElseGet(Collections::emptySet).stream())
        .anyMatch(member -> Objects.equals(member.getId(), layer.getId()));

      log.info("Layer {} belongs to node: {}, task: {}, background: {} => remove: {}", layer.getId(), belongsToNode, belongsToTask, belongsToBackground, !belongsToNode && !belongsToTask && !belongsToBackground);
      return !belongsToNode && !belongsToTask && !belongsToBackground;
    });

    // Prune groups that are not related to backgrounds
    profile.getGroups().removeIf(permission -> {
      boolean belongsToBackground = profile.getBackgrounds().stream()
        .flatMap(background -> Optional.ofNullable(background.getCartographyGroup()).stream())
        .anyMatch(group -> Objects.equals(group.getId(), permission.getId()));

      log.info("Group {} belongs to background: {} => remove: {}", permission.getId(), belongsToBackground, !belongsToBackground);
      return !belongsToBackground;
    });

    // Prune services that either:
    // - Do not belong to a task
    // - Do not belong to a node
    // - Do not belong to a layer
    profile.getServices().removeIf(service -> {
      boolean belongsToTask = profile.getTasks().stream()
        .map(Task::getService)
        .filter(Objects::nonNull)
        .map(org.sitmun.domain.service.Service::getId)
        .anyMatch(serviceId -> Objects.equals(serviceId, service.getId()));

      boolean belongsToNode = profile.getTreeNodes().values().stream()
        .flatMap(Collection::stream)
        .map(TreeNode::getCartography)
        .filter(Objects::nonNull)
        .map(Cartography::getService)
        .filter(Objects::nonNull)
        .map(org.sitmun.domain.service.Service::getId)
        .anyMatch(serviceId -> Objects.equals(serviceId, service.getId()));

      boolean belongsToLayer = profile.getLayers().stream()
        .map(Cartography::getService)
        .filter(Objects::nonNull)
        .map(org.sitmun.domain.service.Service::getId)
        .anyMatch(serviceId -> Objects.equals(serviceId, service.getId()));

      log.info("Service {} belongs to task: {}, node: {}, layer: {} => remove: {}", service.getId(), belongsToTask, belongsToNode, belongsToLayer, !belongsToTask && !belongsToNode && !belongsToLayer);
      return !belongsToTask && !belongsToNode && !belongsToLayer;
    });
    return profile;
  }

  public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
    Set<Object> seen = ConcurrentHashMap.newKeySet();
    return t -> seen.add(keyExtractor.apply(t));
  }
}
