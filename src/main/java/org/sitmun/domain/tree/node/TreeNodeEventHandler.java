package org.sitmun.domain.tree.node;

import com.google.common.base.Strings;
import jakarta.validation.constraints.NotNull;
import org.sitmun.domain.cartography.Cartography;
import org.sitmun.domain.cartography.style.CartographyStyleRepository;
import org.sitmun.infrastructure.persistence.exception.BusinessRuleException;
import org.sitmun.infrastructure.persistence.exception.RequirementException;
import org.sitmun.infrastructure.persistence.type.image.ImageTransformer;
import org.sitmun.infrastructure.web.dto.ProblemTypes;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.HandleBeforeLinkSave;
import org.springframework.data.rest.core.annotation.HandleBeforeSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RepositoryEventHandler
public class TreeNodeEventHandler {

  private final ImageTransformer imageTransformer;
  private final CartographyStyleRepository cartographyStyleRepository;
  private final TreeNodeRepository treeNodeRepository;

  TreeNodeEventHandler(
      ImageTransformer imageTransformer,
      CartographyStyleRepository cartographyStyleRepository,
      TreeNodeRepository treeNodeRepository) {
    this.imageTransformer = imageTransformer;
    this.cartographyStyleRepository = cartographyStyleRepository;
    this.treeNodeRepository = treeNodeRepository;
  }

  @HandleBeforeLinkSave
  @Transactional(rollbackFor = RequirementException.class)
  public void handleTreeNodeLinkSave(
      @NotNull TreeNode treeNode, @SuppressWarnings("unused") Object linked) {
    normalizeActive(treeNode);
    normalizeLoadData(treeNode);
    normalizeQueryableActive(treeNode);
    validateRadio(treeNode);
  }

  @HandleBeforeSave
  @HandleBeforeCreate
  @Transactional(rollbackFor = RequirementException.class)
  public void handleTreeNodeCreate(@NotNull TreeNode treeNode) {
    String type = treeNode.getParent() != null ? treeNode.getParent().getType() : "";
    treeNode.setImage(imageTransformer.scaleImage(treeNode.getImage(), type));
    normalizeActive(treeNode);
    normalizeLoadData(treeNode);
    normalizeQueryableActive(treeNode);
    validateRadio(treeNode);

    Cartography cartography = treeNode.getCartography();
    String style = treeNode.getStyle();
    if (!Strings.isNullOrEmpty(style)) {
      if (cartography != null) {
        String trimmedStyle = style.trim();

        boolean styleExists =
            cartographyStyleRepository.existsByCartographyIdAndName(
                cartography.getId(), trimmedStyle);

        if (styleExists) {
          treeNode.setStyle(trimmedStyle);
        } else {
          throw new BusinessRuleException(
              ProblemTypes.TREE_NODE_STYLE_NOT_FOUND,
              "Tree node style not found in the tree node cartography's styles");
        }
      } else {
        throw new BusinessRuleException(
            ProblemTypes.TREE_NODE_STYLE_REQUIRES_CARTOGRAPHY,
            "Tree node style requires a tree node with cartography");
      }
    }
  }

  private static void normalizeActive(TreeNode treeNode) {
    boolean visible = treeNode.getVisible() == null || treeNode.getVisible();
    if (!visible || !TreeNodeRadioPolicy.isCartographyLeaf(treeNode)) {
      treeNode.setActive(false);
    }
  }

  private static void normalizeLoadData(TreeNode treeNode) {
    if (!TreeNodeRadioPolicy.isFolder(treeNode)) {
      treeNode.setLoadData(false);
    } else if (treeNode.getLoadData() == null) {
      treeNode.setLoadData(false);
    }
  }

  private static void normalizeQueryableActive(TreeNode treeNode) {
    if (!TreeNodeRadioPolicy.isCartographyLeaf(treeNode)) {
      treeNode.setQueryableActive(false);
    } else if (treeNode.getQueryableActive() == null) {
      treeNode.setQueryableActive(false);
    }
  }

  private void validateRadio(TreeNode treeNode) {
    if (Boolean.TRUE.equals(treeNode.getRadio())) {
      if (!TreeNodeRadioPolicy.isFolder(treeNode)) {
        throw new BusinessRuleException(
            ProblemTypes.TREE_NODE_RADIO_SCOPE,
            "Radio is only allowed on cartography tree folder nodes");
      }
      if (!TreeNodeRadioPolicy.isRadioAllowedTree(treeNode.getTree())) {
        throw new BusinessRuleException(
            ProblemTypes.TREE_NODE_RADIO_SCOPE,
            "Radio is only allowed on cartography tree folder nodes");
      }
      if (treeNode.getId() != null
          && treeNodeRepository.existsDirectNonCartographyLeafChild(treeNode.getId())) {
        throw new BusinessRuleException(
            ProblemTypes.TREE_NODE_RADIO_STRUCTURE,
            "Radio folders may only contain cartography leaf nodes");
      }
    }

    TreeNode parent = treeNode.getParent();
    if (parent != null && Boolean.TRUE.equals(parent.getRadio())) {
      if (!TreeNodeRadioPolicy.isCartographyLeaf(treeNode)) {
        throw new BusinessRuleException(
            ProblemTypes.TREE_NODE_RADIO_STRUCTURE,
            "Radio folders may only contain cartography leaf nodes");
      }
      if (Boolean.TRUE.equals(treeNode.getActive())
          && treeNodeRepository.countActiveDirectChildren(parent.getId(), treeNode.getId()) > 0) {
        throw new BusinessRuleException(
            ProblemTypes.TREE_NODE_RADIO_DEFAULT_CONFLICT,
            "Radio folders may only have one active direct child");
      }
    }
  }
}
