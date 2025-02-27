package org.sitmun.authorization.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import org.sitmun.authorization.service.Profile;
import org.sitmun.domain.application.territory.ApplicationTerritory;
import org.sitmun.domain.background.Background;
import org.sitmun.domain.cartography.Cartography;
import org.sitmun.domain.cartography.permission.CartographyPermission;
import org.sitmun.domain.configuration.ConfigurationParameter;
import org.sitmun.domain.service.Service;
import org.sitmun.domain.task.Task;
import org.sitmun.domain.tree.Tree;
import org.sitmun.domain.tree.node.TreeNode;
import org.sitmun.infrastructure.persistence.type.envelope.Envelope;
import org.sitmun.infrastructure.persistence.type.point.Point;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class ProfileMapper {

  public abstract ProfileDto map(Profile profile);

  /**
   * Maps a Background entity to a BackgroundDto.
   *
   * @param background the Background entity to map
   * @return the mapped BackgroundDto
   */
  BackgroundDto map(Background background) {
    return BackgroundDto.builder()
      .id("group/" + background.getCartographyGroup().getId())
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
      .id("layer/" + cartography.getId())
      .title(cartography.getName())
      .layers(cartography.getLayers())
      .service("service/" + cartography.getService().getId())
      .build();
  }

  /**
   * Maps a CartographyPermission entity to a CartographyPermissionDto.
   *
   * @param cartographyPermission the CartographyPermission entity to map
   * @return the mapped CartographyPermissionDto
   */
  CartographyPermissionDto map(CartographyPermission cartographyPermission) {
    return CartographyPermissionDto.builder()
      .id("group/" + cartographyPermission.getId())
      .title(cartographyPermission.getName())
      .layers(cartographyPermission.getMembers().stream().map(it -> "layer/" + it.getId()).collect(Collectors.toList()))
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
      .id("service/" + service.getId())
      .url(service.getServiceURL())
      .type(service.getType())
      .isProxied(service.getIsProxied())
      .parameters(service.getParameters().stream()
        .filter(it -> Objects.equals(it.getType(), service.getType())) // TODO Public parameters should be filtered using a predictable key name.
        .map(it -> new String[]{it.getName(), it.getValue()}).collect(Collectors.toMap(it -> it[0], it -> it[1])))
      .build();
  }

  /**
   * Maps a Task entity to a TaskDto.
   *
   * @param task the Task entity to map
   * @return the mapped TaskDto
   */
  TaskDto map(Task task) {
    String control = null;
    if (task.getUi() != null) {
      control = task.getUi().getName();
    }

    Map<String, Object> parameters = new HashMap<>();
    Map<String, Object> properties = task.getProperties();
    if (properties != null) {
      parameters = convertToJsonObject(properties);
    }
    return TaskDto.builder()
      .id("task/" + task.getId())
      .uiControl(control)
      .parameters(parameters)
      .build();
  }

  TreeDto map(Tree tree) {
    return TreeDto.builder()
      .id("tree/" + tree.getId())
      .title(tree.getName())
      .image(tree.getImage())
      .build();
  }

  final void completeTreeDto(Profile profile, ProfileDto.ProfileDtoBuilder builder) {
    List<TreeDto> treeDtos = builder.build().getTrees();
    profile.getTreeNodes().forEach((tree, allNodes) -> {
        Map<String, List<TreeNode>> listNodes = new HashMap<>();
        allNodes.forEach(it -> {
          String id = "node/" + it.getId();
          if (!listNodes.containsKey(id)) {
            listNodes.put(id, new ArrayList<>());
          }

          if (it.getParentId() != null) {
            String parent = "node/" + it.getParentId();
            if (listNodes.containsKey(parent)) {
              listNodes.get(parent).add(it);
            } else {
              listNodes.put(parent, new ArrayList<>(List.of(it)));
            }
          }
        });
        listNodes.forEach((key, value) -> value.sort(Comparator.comparing(TreeNode::getOrder)));

        String rootNodeCandidate = null;

        Map<String, NodeDto> nodes = new HashMap<>();

        switch(profile.getContext().getNodeSectionBehaviour()) {
          case VIRTUAL_ROOT_ALL_NODES:
          case VIRTUAL_ROOT_NODE_PAGE:
            rootNodeCandidate = "node/tree/" + tree.getId();
            nodes.put(rootNodeCandidate, createRootNode(tree, allNodes));
            break;
          case ANY_NODE_PAGE:
            rootNodeCandidate = "node/" + profile.getContext().getNodeId();
        }

        allNodes.forEach(it -> {
          String id = "node/" + it.getId();
          NodeDto node = createNode(it, listNodes, id);
          nodes.put(id, node);
        });

        final String rootNode = rootNodeCandidate;
        treeDtos.stream()
          .filter(it -> it.getId().equals("tree/" + tree.getId()))
          .findFirst()
          .ifPresent(treeDto -> {
            treeDto.setRootNode(rootNode);
            treeDto.setNodes(nodes);
          });
      });
    builder.trees(treeDtos);
  }

  private NodeDto createNode(TreeNode it, Map<String, List<TreeNode>> listNodes, String id) {
    NodeDto.NodeDtoBuilder nodeDtoBuilder = NodeDto.builder()
      .title(it.getName())
      .description(it.getDescription())
      .isRadio(it.getRadio())
      .loadData(it.getLoadData())
      .type(it.getType())
      .image(it.getImage())
      .order(it.getOrder());
    if (it.getCartographyId() != null) {
      nodeDtoBuilder = nodeDtoBuilder.resource("layer/" + it.getCartographyId());
    } else if (it.getTaskId() != null) {
      nodeDtoBuilder = nodeDtoBuilder.action("task/" + it.getTaskId());
      nodeDtoBuilder = nodeDtoBuilder.viewMode(it.getViewMode());
    } else {
      List<String> nodeChildren = listNodes.get(id).stream().map(node -> "node/" + node.getId()).collect(Collectors.toList());
      nodeDtoBuilder = nodeDtoBuilder.children(nodeChildren);
    }
    return nodeDtoBuilder.build();
  }

  private NodeDto createRootNode(Tree tree, List<TreeNode> allNodes) {
    return NodeDto.builder()
      .title(tree.getName())
      .loadData(false)
      .children(allNodes.stream()
        .filter(it1 -> it1.getParent() == null)
        .map(it1 -> "node/" + it1.getId())
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
    builder.application(applicationDto);
  }

  @NotNull
  private Map<String, Object> convertToJsonObject(Map<String, Object> properties) {
    Map<String, Object> parameters;
    parameters = new HashMap<>();
    //noinspection unchecked
    List<Map<String, String>> listOfParameters = (List<Map<String, String>>) properties.getOrDefault("parameters", Collections.emptyList());
    for (Map<String, String> param : listOfParameters) {
      if (param.containsKey("name") && param.containsKey("type") && param.containsKey("value")) {
        String name = param.get("name");
        String type = param.get("type");
        String value = param.get("value");
        typeBasedConversion(type, value, parameters, name);
      }
    }
    return parameters;
  }

  private void typeBasedConversion(String type, String value, Map<String, Object> parameters, String name) {
    switch (type) {
      case "string":
        if (value == null) {
          value = "";
        }
        parameters.put(name, value);
        break;
      case "number":
        parameters.put(name, Double.parseDouble(value));
        break;
      case "array":
        try {
          parameters.put(name, new ObjectMapper().readValue(value, List.class));
        } catch (JsonProcessingException e) {
          log.error("Error processing array", e);
        }
        break;
      case "object":
        try {
          parameters.put(name, new ObjectMapper().readValue(value, Map.class));
        } catch (JsonProcessingException e) {
          log.error("Error processing object", e);
        }
        break;
      case "boolean":
        parameters.put(name, Boolean.parseBoolean(value));
        break;
      case "null":
        parameters.put(name, null);
        break;
      default:
        break;
    }
  }


  Map<String, String> map(List<ConfigurationParameter> global) {
    return global.stream().collect(Collectors.toMap(ConfigurationParameter::getName, ConfigurationParameter::getValue));
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
    return "group/" + value.getId();
  }
}
