package org.sitmun.plugin.core.repository.handlers;

import com.google.common.base.Strings;
import org.sitmun.plugin.core.domain.Cartography;
import org.sitmun.plugin.core.domain.TreeNode;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.HandleBeforeSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;

@Component
@RepositoryEventHandler
public class TreeNodeEventHandler {

  @HandleBeforeSave
  @HandleBeforeCreate
  @Transactional(rollbackFor = RequirementException.class)
  public void handleTreeNodeCreate(@NotNull TreeNode treeNode) {
    Cartography cartography = treeNode.getCartography();
    String style = treeNode.getStyle();
    if (!Strings.isNullOrEmpty(style)) {
      if (cartography != null) {
        String trimmedStyle = style.trim();
        if (cartography.getStyles().stream().anyMatch(it -> trimmedStyle.equals(it.getName()))) {
          treeNode.setStyle(trimmedStyle);
        } else {
          throw new RequirementException("Tree node style not found in the tree node cartography's styles");
        }
      } else {
        throw new RequirementException("Tree node style requires a tree node with cartography");
      }
    }
  }
}