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
import org.springframework.data.rest.core.annotation.HandleBeforeSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RepositoryEventHandler
public class TreeNodeEventHandler {

  private final ImageTransformer imageTransformer;
  private final CartographyStyleRepository cartographyStyleRepository;

  TreeNodeEventHandler(
      ImageTransformer imageTransformer, CartographyStyleRepository cartographyStyleRepository) {
    this.imageTransformer = imageTransformer;
    this.cartographyStyleRepository = cartographyStyleRepository;
  }

  @HandleBeforeSave
  @HandleBeforeCreate
  @Transactional(rollbackFor = RequirementException.class)
  public void handleTreeNodeCreate(@NotNull TreeNode treeNode) {
    String type = treeNode.getParent() != null ? treeNode.getParent().getType() : "";
    treeNode.setImage(imageTransformer.scaleImage(treeNode.getImage(), type));

    Cartography cartography = treeNode.getCartography();
    String style = treeNode.getStyle();
    if (!Strings.isNullOrEmpty(style)) {
      if (cartography != null) {
        String trimmedStyle = style.trim();

        // Use repository query instead of accessing lazy collection
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
}
