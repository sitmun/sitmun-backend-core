package org.sitmun.domain.tree.node;

import com.google.common.base.Strings;
import jakarta.validation.constraints.NotNull;
import org.sitmun.domain.cartography.Cartography;
import org.sitmun.infrastructure.persistence.exception.RequirementException;
import org.sitmun.infrastructure.persistence.type.image.ImageTransformer;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.HandleBeforeSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RepositoryEventHandler
public class TreeNodeEventHandler {

  final ImageTransformer imageTransformer;

  TreeNodeEventHandler(ImageTransformer imageTransformer) {
    this.imageTransformer = imageTransformer;
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
        if (cartography.getStyles().stream().anyMatch(it -> trimmedStyle.equals(it.getName()))) {
          treeNode.setStyle(trimmedStyle);
        } else {
          throw new RequirementException(
              "Tree node style not found in the tree node cartography's styles");
        }
      } else {
        throw new RequirementException("Tree node style requires a tree node with cartography");
      }
    }
  }
}
