package org.sitmun.authorization.client.mapper;

import static org.sitmun.domain.DomainConstants.Tasks.PROFILE_GROUP_ID_PREFIX;
import static org.sitmun.domain.DomainConstants.Tasks.PROFILE_LAYER_ID_PREFIX;
import static org.sitmun.domain.DomainConstants.Tasks.PROFILE_NODE_ID_PREFIX;
import static org.sitmun.domain.DomainConstants.Tasks.PROFILE_SERVICE_ID_PREFIX;
import static org.sitmun.domain.DomainConstants.Tasks.PROFILE_TREE_ID_PREFIX;
import static org.sitmun.domain.DomainConstants.Tasks.TASK_PROFILE_ID_PREFIX;

import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.*;
import org.sitmun.authorization.client.dto.*;
import org.sitmun.authorization.client.service.Profile;
import org.sitmun.authorization.client.service.TaskMapper;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.application.territory.ApplicationTerritory;
import org.sitmun.domain.background.Background;
import org.sitmun.domain.cartography.Cartography;
import org.sitmun.domain.cartography.permission.CartographyPermission;
import org.sitmun.domain.configuration.ConfigurationParameter;
import org.sitmun.domain.service.Service;
import org.sitmun.domain.task.Task;
import org.sitmun.domain.territory.Territory;
import org.sitmun.domain.tree.Tree;
import org.sitmun.domain.tree.node.TreeNode;
import org.sitmun.domain.user.User;
import org.sitmun.infrastructure.persistence.type.envelope.Envelope;
import org.sitmun.infrastructure.persistence.type.point.Point;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class ProfileMapper {

  @Autowired private List<TaskMapper> taskMappers;

  public abstract ProfileDto map(
      Profile profile, @Context Application application, @Context Territory territory);

  String map(User user) {
    if (user == null) {
      return null;
    }
    return user.getUsername();
  }

  /**
   * Maps a Background entity to a BackgroundDto.
   *
   * @param background the Background entity to map
   * @return the mapped BackgroundDto
   */
  BackgroundDto map(Background background) {
    return BackgroundDto.builder()
        .id(PROFILE_GROUP_ID_PREFIX + background.getCartographyGroup().getId())
        .title(background.getName())
        .thumbnail(background.getImage())
        .build();
  }

  /**
   * Maps a Cartography entity to a CartographyDto.
   *
   * @param cartography the Cartography entity to map
   * @return the mapped CartographyDto
   */
  CartographyDto map(Cartography cartography) {
    return CartographyDto.builder()
        .id(PROFILE_LAYER_ID_PREFIX + cartography.getId())
        .title(cartography.getName())
        .description(cartography.getDescription())
        .layers(cartography.getLayers())
        .service(PROFILE_SERVICE_ID_PREFIX + cartography.getService().getId())
        .minScaleDenominator(positiveOrNull(cartography.getMinimumScale()))
        .maxScaleDenominator(positiveOrNull(cartography.getMaximumScale()))
        .transparency(cartography.getTransparency())
        .order(cartography.getOrder())
        .metadataURL(cartography.getMetadataURL())
        .datasetURL(cartography.getDatasetURL())
        .queryableFeatureEnabled(cartography.getQueryableFeatureEnabled())
        .build();
  }

  /**
   * Maps scale to profile JSON only when positive. Zero and negatives are normalized to null so
   * they are omitted (NON_NULL) and SITNA never sees a zero max denominator.
   */
  private static Integer positiveOrNull(Integer v) {
    if (v == null || v <= 0) {
      return null;
    }
    return v;
  }

  /**
   * Maps a CartographyPermission entity to a CartographyPermissionDto.
   *
   * @param cartographyPermission the CartographyPermission entity to map
   * @return the mapped CartographyPermissionDto
   */
  CartographyPermissionDto map(CartographyPermission cartographyPermission) {
    return CartographyPermissionDto.builder()
        .id(PROFILE_GROUP_ID_PREFIX + cartographyPermission.getId())
        .title(cartographyPermission.getName())
        .layers(
            cartographyPermission.getMembers().stream()
                .map(it -> PROFILE_LAYER_ID_PREFIX + it.getId())
                .collect(Collectors.toList()))
        .build();
  }

  /**
   * Maps a Service entity to a ServiceDto.
   *
   * @param service the Service entity to map
   * @return the mapped ServiceDto
   */
  ServiceDto map(Service service) {
    return ServiceDto.builder()
        .id(PROFILE_SERVICE_ID_PREFIX + service.getId())
        .url(service.getServiceURL())
        .type(service.getType())
        .isProxied(service.getIsProxied())
        .parameters(
            service.getParameters().stream()
                .filter(it -> Objects.equals(it.getType(), service.getType()))
                .map(it -> new String[] {it.getName(), it.getValue()})
                .collect(Collectors.toMap(it -> it[0], it -> it[1])))
        .crs(service.getSupportedSRS())
        .build();
  }

  /**
   * Maps a Task entity to a TaskDto.
   *
   * @param task the Task entity to map
   * @return the mapped TaskDto
   */
  TaskDto map(Task task, @Context Application application, @Context Territory territory) {
    Optional<TaskDto> taskDto =
        taskMappers.stream()
            .filter(taskMapper -> taskMapper.accept(task))
            .findFirst()
            .map(taskMapper -> taskMapper.map(task, application, territory));
    if (taskDto.isPresent()) {
      return taskDto.get();
    } else {
      log.warn("No task mapper found for task id: {}", task.getId());
      return TaskDto.builder().id(TASK_PROFILE_ID_PREFIX + task.getId()).build();
    }
  }

  TreeDto map(Tree tree) {
    return TreeDto.builder()
        .id(PROFILE_TREE_ID_PREFIX + tree.getId())
        .title(tree.getName())
        .type(tree.getType())
        .image(tree.getImage())
        .build();
  }

  final void completeTreeDto(Profile profile, ProfileDto.ProfileDtoBuilder builder) {
    List<TreeDto> treeDtos = builder.build().getTrees();
    profile
        .getTreeNodes()
        .forEach((tree, allNodes) -> completeTreeSection(profile, tree, allNodes, treeDtos));
    builder.trees(treeDtos);
  }

  /** Builds parent→children index keyed by profile node id ({@code node/…}). */
  private static Map<String, List<TreeNode>> buildListNodesIndex(List<TreeNode> allNodes) {
    Map<String, List<TreeNode>> listNodes = new HashMap<>();
    allNodes.forEach(
        it -> {
          String id = PROFILE_NODE_ID_PREFIX + it.getId();
          listNodes.putIfAbsent(id, new ArrayList<>());
          if (it.getParentId() != null) {
            String parent = PROFILE_NODE_ID_PREFIX + it.getParentId();
            if (listNodes.containsKey(parent)) {
              listNodes.get(parent).add(it);
            } else {
              listNodes.put(parent, new ArrayList<>(List.of(it)));
            }
          }
        });
    listNodes.forEach((key, children) -> children.sort(Comparator.comparing(TreeNode::getOrder)));
    return listNodes;
  }

  private void completeTreeSection(
      Profile profile, Tree tree, List<TreeNode> allNodes, List<TreeDto> treeDtos) {
    Map<String, List<TreeNode>> listNodes = buildListNodesIndex(allNodes);
    Map<String, NodeDto> nodes = new HashMap<>();
    String rootNodeId = resolveRootNodeId(profile, tree, allNodes, nodes);
    fillNodeDtos(allNodes, listNodes, nodes);
    applyRootAndNodesToTreeDto(tree, treeDtos, rootNodeId, nodes);
  }

  private String resolveRootNodeId(
      Profile profile, Tree tree, List<TreeNode> allNodes, Map<String, NodeDto> nodes) {
    return switch (profile.getContext().getNodeSectionBehaviour()) {
      case VIRTUAL_ROOT_ALL_NODES, VIRTUAL_ROOT_NODE_PAGE ->
          resolveVirtualTreeRootId(tree, allNodes, nodes);
      case ANY_NODE_PAGE -> PROFILE_NODE_ID_PREFIX + profile.getContext().getNodeId();
    };
  }

  /**
   * Virtual tree root: either collapse to the single top-level child id or keep a synthetic root
   * node in {@code nodes}.
   */
  private String resolveVirtualTreeRootId(
      Tree tree, List<TreeNode> allNodes, Map<String, NodeDto> nodes) {
    String syntheticRootId = PROFILE_NODE_ID_PREFIX + PROFILE_TREE_ID_PREFIX + tree.getId();
    NodeDto rootNode = createRootNode(tree, allNodes);
    if (rootNode.getChildren().size() == 1) {
      return rootNode.getChildren().get(0);
    }
    nodes.put(syntheticRootId, rootNode);
    return syntheticRootId;
  }

  private void fillNodeDtos(
      List<TreeNode> allNodes, Map<String, List<TreeNode>> listNodes, Map<String, NodeDto> nodes) {
    allNodes.forEach(
        it -> {
          String id = PROFILE_NODE_ID_PREFIX + it.getId();
          nodes.put(id, createNode(it, listNodes, id));
        });
  }

  private static void applyRootAndNodesToTreeDto(
      Tree tree, List<TreeDto> treeDtos, String rootNodeId, Map<String, NodeDto> nodes) {
    treeDtos.stream()
        .filter(it -> it.getId().equals(PROFILE_TREE_ID_PREFIX + tree.getId()))
        .findFirst()
        .ifPresent(
            treeDto -> {
              treeDto.setRootNode(rootNodeId);
              treeDto.setNodes(nodes);
            });
  }

  private NodeDto createNode(TreeNode it, Map<String, List<TreeNode>> listNodes, String id) {
    NodeDto.NodeDtoBuilder nodeDtoBuilder =
        NodeDto.builder()
            .title(it.getName())
            .description(it.getDescription())
            .isRadio(it.getRadio())
            .loadData(it.getLoadData())
            .type(it.getType())
            .image(it.getImage())
            .order(it.getOrder())
            .mapping(it.getMapping())
            .metadataURL(it.getMetadataURL())
            .datasetURL(it.getDatasetURL());
    if (it.getCartographyId() != null) {
      nodeDtoBuilder = nodeDtoBuilder.resource(PROFILE_LAYER_ID_PREFIX + it.getCartographyId());
    }
    if (it.getTaskId() != null) {
      nodeDtoBuilder = nodeDtoBuilder.action(TASK_PROFILE_ID_PREFIX + it.getTaskId());
      nodeDtoBuilder = nodeDtoBuilder.viewMode(it.getViewMode());
    }
    List<String> nodeChildren =
        listNodes.get(id).stream()
            .map(node -> PROFILE_NODE_ID_PREFIX + node.getId())
            .collect(Collectors.toList());
    if (!nodeChildren.isEmpty()) {
      nodeDtoBuilder = nodeDtoBuilder.children(nodeChildren);
    }

    return nodeDtoBuilder.build();
  }

  private NodeDto createRootNode(Tree tree, List<TreeNode> allNodes) {
    return NodeDto.builder()
        .title(tree.getName())
        .loadData(false)
        .children(
            allNodes.stream()
                .filter(it1 -> it1.getParent() == null)
                .map(it1 -> PROFILE_NODE_ID_PREFIX + it1.getId())
                .collect(Collectors.toList()))
        .build();
  }

  final void copyDefaultZoomLevelFromTerritory(ApplicationDto applicationDto, Profile profile) {
    Integer defaultZoomLevel = profile.getTerritory().getDefaultZoomLevel();
    applicationDto.setDefaultZoomLevel(defaultZoomLevel);
  }

  final void copyInitialExtentFromTerritory(ApplicationDto applicationDto, Profile profile) {
    Integer selectedTerritory = profile.getTerritory().getId();
    Envelope defaultEnvelope = profile.getTerritory().getExtent();
    applicationDto.setInitialExtentFromEnvelope(defaultEnvelope);
    profile.getApplication().getTerritories().stream()
        .filter(it -> Objects.equals(it.getTerritory().getId(), selectedTerritory))
        .findFirst()
        .map(ApplicationTerritory::getInitialExtent)
        .ifPresent(applicationDto::setInitialExtentFromEnvelope);
  }

  final void copyPointFromTerritory(ApplicationDto applicationDto, Profile profile) {
    Point point = profile.getTerritory().getCenter();
    if (point != null) {
      PointOfInterestDto poi = PointOfInterestDto.builder().x(point.getX()).y(point.getY()).build();
      applicationDto.setPointOfInterest(poi);
    }
  }

  final void copySrsFromTerritory(ApplicationDto applicationDto, Profile profile) {
    if (profile.getTerritory().getSrs() != null) {
      applicationDto.setSrs(profile.getTerritory().getSrs());
    }
  }

  final void completeApplicationDto(Profile profile, ProfileDto.ProfileDtoBuilder builder) {
    ApplicationDto applicationDto = builder.build().getApplication();
    copyInitialExtentFromTerritory(applicationDto, profile);
    copyDefaultZoomLevelFromTerritory(applicationDto, profile);
    copyPointFromTerritory(applicationDto, profile);
    copySrsFromTerritory(applicationDto, profile);
    copySituationMap(applicationDto, profile);
    builder.application(applicationDto);
  }

  /** Maps situation-map from Application to ApplicationDto. */
  final void copySituationMap(ApplicationDto applicationDto, Profile profile) {
    CartographyPermission situationMap = profile.getApplication().getSituationMap();
    if (situationMap != null) {
      applicationDto.setSituationMap(mapCartographyPermissionToString(situationMap));
    }
  }

  Map<String, String> map(List<ConfigurationParameter> global) {
    return global.stream()
        .collect(
            Collectors.toMap(ConfigurationParameter::getName, ConfigurationParameter::getValue));
  }

  @AfterMapping
  void completeProfile(Profile profile, @MappingTarget ProfileDto.ProfileDtoBuilder builder) {
    completeApplicationDto(profile, builder);
    completeTreeDto(profile, builder);
  }

  String mapCartographyPermissionToString(CartographyPermission value) {
    if (value == null) {
      return null;
    }
    return PROFILE_GROUP_ID_PREFIX + value.getId();
  }
}
