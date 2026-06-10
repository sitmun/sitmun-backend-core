package org.sitmun.authorization.client.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.domain.tree.node.TreeNode;

@DisplayName("TreeNodeVisibilityPolicy")
class TreeNodeVisibilityPolicyTest {

  @Test
  @DisplayName("null active is visible")
  void nullActiveNodeIsVisible() {
    TreeNode node = TreeNode.builder().id(1).active(null).build();

    assertThat(TreeNodeVisibilityPolicy.isActive(node)).isTrue();
    assertThat(TreeNodeVisibilityPolicy.isVisibleInClientProfile(node, Map.of(1, node))).isTrue();
  }

  @Test
  @DisplayName("true active is visible")
  void trueActiveNodeIsVisible() {
    TreeNode node = TreeNode.builder().id(1).active(true).build();

    assertThat(TreeNodeVisibilityPolicy.isActive(node)).isTrue();
    assertThat(TreeNodeVisibilityPolicy.isVisibleInClientProfile(node, Map.of(1, node))).isTrue();
  }

  @Test
  @DisplayName("false active is hidden")
  void falseActiveNodeIsHidden() {
    TreeNode node = TreeNode.builder().id(1).active(false).build();

    assertThat(TreeNodeVisibilityPolicy.isActive(node)).isFalse();
    assertThat(TreeNodeVisibilityPolicy.isVisibleInClientProfile(node, Map.of(1, node))).isFalse();
  }

  @Test
  @DisplayName("active child under inactive parent is hidden")
  void childOfInactiveParentIsHiddenEvenWhenChildActive() {
    TreeNode parent = TreeNode.builder().id(1).active(false).build();
    TreeNode child = TreeNode.builder().id(2).parent(parent).active(true).build();
    Map<Integer, TreeNode> byId = Map.of(1, parent, 2, child);

    assertThat(TreeNodeVisibilityPolicy.isVisibleInClientProfile(child, byId)).isFalse();
  }

  @Test
  @DisplayName("cycle in parent chain does not loop and treats node as hidden")
  void cycleDoesNotLoopForeverAndTreatsCycleConservatively() {
    TreeNode a = TreeNode.builder().id(1).active(true).build();
    TreeNode b = TreeNode.builder().id(2).parent(a).active(true).build();
    a.setParent(b);
    Map<Integer, TreeNode> byId = Map.of(1, a, 2, b);

    assertThat(TreeNodeVisibilityPolicy.isVisibleInClientProfile(a, byId)).isFalse();
    assertThat(TreeNodeVisibilityPolicy.isVisibleInClientProfile(b, byId)).isFalse();
  }

  @Test
  @DisplayName("missing parent id does not hide visible node")
  void missingParentDoesNotHideNode() {
    TreeNode parent = TreeNode.builder().id(1).active(true).build();
    TreeNode child = TreeNode.builder().id(2).parent(parent).active(true).build();
    Map<Integer, TreeNode> byId = Map.of(2, child);

    assertThat(TreeNodeVisibilityPolicy.isVisibleInClientProfile(child, byId)).isTrue();
  }

  @Test
  @DisplayName("filter keeps only visible nodes")
  void filterVisibleInClientProfile() {
    TreeNode visible = TreeNode.builder().id(1).active(true).build();
    TreeNode hidden = TreeNode.builder().id(2).active(false).build();
    TreeNode hiddenChild = TreeNode.builder().id(3).parent(hidden).active(true).build();

    List<TreeNode> filtered =
        TreeNodeVisibilityPolicy.filterVisibleInClientProfile(
            List.of(visible, hidden, hiddenChild));

    assertThat(filtered).containsExactly(visible);
  }
}
