package org.sitmun.authorization.client.service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.sitmun.domain.tree.node.TreeNode;

/**
 * Client-profile visibility for {@link TreeNode#getVisible()} (catalog visibility / TNO_ACTIVE).
 */
final class TreeNodeVisibilityPolicy {

  private TreeNodeVisibilityPolicy() {}

  static boolean isVisible(TreeNode node) {
    Boolean visible = node.getVisible();
    return visible == null || visible;
  }

  static boolean isVisibleInClientProfile(TreeNode node, Map<Integer, TreeNode> nodesById) {
    if (!isVisible(node)) {
      return false;
    }
    Integer parentId = node.getParentId();
    if (parentId == null) {
      return true;
    }
    Set<Integer> visited = new HashSet<>();
    visited.add(node.getId());
    while (parentId != null) {
      if (!visited.add(parentId)) {
        return false;
      }
      TreeNode parent = nodesById.get(parentId);
      if (parent == null) {
        return true;
      }
      if (!isVisible(parent)) {
        return false;
      }
      parentId = parent.getParentId();
    }
    return true;
  }

  static List<TreeNode> filterVisibleInClientProfile(List<TreeNode> nodes) {
    if (nodes == null || nodes.isEmpty()) {
      return List.of();
    }
    Map<Integer, TreeNode> nodesById =
        nodes.stream()
            .filter(Objects::nonNull)
            .filter(n -> n.getId() != null)
            .collect(Collectors.toMap(TreeNode::getId, Function.identity(), (a, b) -> a));
    return nodes.stream()
        .filter(Objects::nonNull)
        .filter(node -> isVisibleInClientProfile(node, nodesById))
        .toList();
  }
}
