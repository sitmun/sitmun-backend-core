package org.sitmun.authorization.dto;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import org.sitmun.authorization.service.Profile;
import org.sitmun.domain.application.background.ApplicationBackground;
import org.sitmun.domain.background.Background;
import org.sitmun.domain.cartography.Cartography;
import org.sitmun.domain.cartography.permission.CartographyPermission;
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

  default BackgroundDto mapCartographyPermissionToBackground(CartographyPermission background) {
    return BackgroundDto.builder()
      .id("group/" + background.getId())
      .title(background.getName())
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

    final String[] rootNode = new String[1];
    allNodes.stream().filter(it -> it.getParent() == null).findFirst().ifPresent(root -> rootNode[0] = "node/" + root.getId());

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
      .rootNode(rootNode[0])
      .nodes(nodes)
      .build();
  }

  @AfterMapping
  default void completeProfile(Profile profile, @MappingTarget ProfileDto.ProfileDtoBuilder builder) {
    Comparator<ApplicationBackground> order = Comparator.nullsLast(Comparator.comparing(ApplicationBackground::getOrder));
    List<BackgroundDto> backgrounds = profile.getApplication().getBackgrounds().stream()
      .sorted(order)
      .map(ApplicationBackground::getBackground)
      .filter(Background::getActive)
      .map(Background::getCartographyGroup)
      .map(this::mapCartographyPermissionToBackground)
      .collect(Collectors.toList());
    builder.backgrounds(backgrounds);

    profile.getApplication().getTerritories()
      .stream().filter(it -> Objects.equals(it.getTerritory().getId(), profile.getTerritory().getId()))
      .findFirst()
      .ifPresent(applicationTerritory -> {
        Envelope extent = applicationTerritory.getInitialExtent();
        if (extent == null) {
          extent = applicationTerritory.getTerritory().getExtent();
        }
        if (extent != null) {
          ApplicationDto application = builder.build().getApplication();
          application.setInitialExtent(new Double[]{
              extent.getMinX(),
              extent.getMinY(),
              extent.getMaxX(),
              extent.getMaxY()
            }
          );
          builder.application(application);
        }
      });

    Set<Tree> trees = profile.getApplication().getTrees();

    if (trees != null) {
      builder.trees(trees.stream().map(this::map).collect(Collectors.toList()));
    }
  }

  default String mapCartographyPermissionToString(CartographyPermission value) {
    if (value == null) return null;
    return "group/" + value.getId();
  }

}
