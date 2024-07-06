package org.sitmun.authorization.dto;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import org.sitmun.authorization.service.Profile;
import org.sitmun.domain.application.background.ApplicationBackground;
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

import java.util.*;
import java.util.stream.Collectors;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProfileMapper {
  ProfileDto map(Profile profile);

  default CartographyDto map(Cartography cartography) {
    return CartographyDto.builder()
      .id("layer/" + cartography.getId())
      .title(cartography.getName())
      .layers(cartography.getLayers())
      .service("service/" + cartography.getService().getId())
      .build();
  }

  default CartographyPermissionDto map(CartographyPermission cartographyPermission) {
    return CartographyPermissionDto.builder()
      .id("group/" + cartographyPermission.getId())
      .title(cartographyPermission.getName())
      .layers(cartographyPermission.getMembers().stream().map(it -> "layer/" + it.getId()).collect(java.util.stream.Collectors.toList()))
      .build();
  }

  default ServiceDto map(Service service) {
    return ServiceDto.builder()
      .id("service/" + service.getId())
      .url(service.getServiceURL())
      .type(service.getType())
      .parameters(service.getParameters().stream()
        .filter(it -> Objects.equals(it.getType(), service.getType()))
        .map(it -> new String[]{it.getName(), it.getValue()}).collect(java.util.stream.Collectors.toMap(it -> it[0], it -> it[1])))
      .build();
  }

  default BackgroundDto map(Background background) {
    return BackgroundDto.builder()
      .id("group/" + background.getCartographyGroup().getId())
      .title(background.getName())
      .thumbnail(background.getImage())
      .build();
  }

  default TaskDto map(Task task) {
    String control = null;
    if (task.getUi() != null) {
      control = task.getUi().getName();
    }
    return TaskDto.builder()
      .id("task/" + task.getId())
      .uiControl(control)
      .parameters(task.getProperties())
      .build();
  }


  default TreeDto map(Tree tree) {
    Set<TreeNode> allNodes = tree.getAllNodes();

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

    Map<String, NodeDto> nodes = new HashMap<>();

    final String rootNode = "node/tree/" + tree.getId();

    List<String> rootChildren = allNodes.stream()
      .filter(it -> it.getParent() == null)
      .map(it -> "node/" + it.getId())
      .collect(Collectors.toList());

    NodeDto rootNodeDto = NodeDto.builder()
      .title(tree.getName())
      .children(rootChildren)
      .build();

    nodes.put(rootNode, rootNodeDto);

    allNodes.forEach(it -> {
      String id = "node/" + it.getId();
      String resource = null;
      List<String> children = null;
      if (it.getCartographyId() != null) {
        resource = "layer/" + it.getCartographyId();
      } else {
        children = listNodes.get(id).stream().map(node -> "node/" + node.getId()).collect(Collectors.toList());
      }
      NodeDto nodeDto = NodeDto.builder()
        .title(it.getName())
        .resource(resource)
        .children(children)
        .isRadio(it.getRadio())
        .build();
      nodes.put(id, nodeDto);
    });

    return TreeDto.builder()
      .id("tree/" + tree.getId())
      .title(tree.getName())
      .image(tree.getImage())
      .rootNode(rootNode)
      .nodes(nodes)
      .build();
  }

  default Map<String,String> map(List<ConfigurationParameter> global) {
    return global.stream().collect(Collectors.toMap(ConfigurationParameter::getName, ConfigurationParameter::getValue));
  }

  @AfterMapping
  default void completeProfile(Profile profile, @MappingTarget ProfileDto.ProfileDtoBuilder builder) {
    List<BackgroundDto> backgrounds = profile.getApplication().getBackgrounds().stream()
      .peek(it -> {
        // TODO Provide a default value to order
        if (it.getOrder() == null) {
          it.setOrder(0);
        }
      })
      .sorted(Comparator.comparingInt(ApplicationBackground::getOrder))
      .map(ApplicationBackground::getBackground)
      .filter(Background::getActive)
      .map(this::map)
      .collect(Collectors.toList());
    builder.backgrounds(backgrounds);

    computeApplicationExtent(profile, builder);

    Set<Tree> trees = profile.getApplication().getTrees();

    if (trees != null) {
      builder.trees(trees.stream().map(this::map).collect(Collectors.toList()));
    }
  }

  private static void computeApplicationExtent(Profile profile, ProfileDto.ProfileDtoBuilder builder) {
    Optional<ApplicationTerritory> applicationTerritory = profile.getApplication().getTerritories().stream().filter(it -> Objects.equals(it.getTerritory().getId(), profile.getTerritory().getId()))
      .findFirst();

    Envelope extentCandidate = applicationTerritory.map(ApplicationTerritory::getInitialExtent).orElse(null);

    if (extentCandidate == null) {
      extentCandidate = profile.getTerritory().getExtent();
    }

    if (extentCandidate != null) {
      ApplicationDto application = builder.build().getApplication();
      application.setInitialExtent(new Double[]{
          extentCandidate.getMinX(),
          extentCandidate.getMinY(),
          extentCandidate.getMaxX(),
          extentCandidate.getMaxY()
        }
      );
      builder.application(application);
    }
  }



  default String mapCartographyPermissionToString(CartographyPermission value) {
    if (value == null) {
      return null;
    }
    return "group/" + value.getId();
  }

}
