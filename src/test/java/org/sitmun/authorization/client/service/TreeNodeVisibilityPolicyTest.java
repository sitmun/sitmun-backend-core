package org.sitmun.authorization.client.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.domain.tree.node.TreeNode;

@DisplayName("TreeNodeVisibilityPolicy")
class TreeNodeVisibilityPolicyTest {

  @Test
  @DisplayName("includes visible nodes with visible ancestors")
  void includesVisibleNodesWithVisibleAncestors() {
    TreeNode root = node(1, null, true);
    TreeNode child = node(2, root, true);

    List<TreeNode> filtered =
        TreeNodeVisibilityPolicy.filterVisibleInClientProfile(List.of(root, child));

    assertThat(filtered).containsExactly(root, child);
  }

  @Test
  @DisplayName("excludes nodes when visible is false regardless of active")
  void excludesNodesWhenVisibleIsFalse() {
    TreeNode hiddenLeaf = node(12, null, false);
    hiddenLeaf.setActive(true);

    List<TreeNode> filtered =
        TreeNodeVisibilityPolicy.filterVisibleInClientProfile(List.of(hiddenLeaf));

    assertThat(filtered).isEmpty();
  }

  @Test
  @DisplayName("includes visible inactive leaves in the catalog")
  void includesVisibleInactiveLeaves() {
    TreeNode visibleInactiveLeaf = node(3, null, true);
    visibleInactiveLeaf.setActive(false);

    List<TreeNode> filtered =
        TreeNodeVisibilityPolicy.filterVisibleInClientProfile(List.of(visibleInactiveLeaf));

    assertThat(filtered).containsExactly(visibleInactiveLeaf);
  }

  @Test
  @DisplayName("excludes descendants of invisible folders")
  void excludesDescendantsOfInvisibleFolders() {
    TreeNode invisibleFolder = node(13, null, false);
    TreeNode child = node(14, invisibleFolder, true);
    child.setActive(true);

    List<TreeNode> filtered =
        TreeNodeVisibilityPolicy.filterVisibleInClientProfile(List.of(invisibleFolder, child));

    assertThat(filtered).isEmpty();
  }

  @Test
  @DisplayName("treats null visible as visible")
  void treatsNullVisibleAsVisible() {
    TreeNode node = node(5, null, null);

    assertThat(TreeNodeVisibilityPolicy.isVisible(node)).isTrue();
    assertThat(TreeNodeVisibilityPolicy.filterVisibleInClientProfile(List.of(node)))
        .containsExactly(node);
  }

  @Test
  @DisplayName("missing parent in lookup is permissive")
  void missingParentIsPermissive() {
    TreeNode child = node(20, null, true);
    child.setParent(node(21, null, true));

    assertThat(TreeNodeVisibilityPolicy.filterVisibleInClientProfile(List.of(child)))
        .containsExactly(child);
  }

  @Test
  @DisplayName("parent cycle is hidden")
  void parentCycleIsHidden() {
    TreeNode a = node(30, null, true);
    TreeNode b = node(31, a, true);
    a.setParent(b);

    assertThat(TreeNodeVisibilityPolicy.filterVisibleInClientProfile(List.of(a, b))).isEmpty();
  }

  private static TreeNode node(int id, TreeNode parent, Boolean visible) {
    TreeNode node = new TreeNode();
    node.setId(id);
    node.setParent(parent);
    node.setVisible(visible);
    node.setName("node-" + id);
    return node;
  }
}
