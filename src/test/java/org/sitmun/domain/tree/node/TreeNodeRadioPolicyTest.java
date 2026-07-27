package org.sitmun.domain.tree.node;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.domain.cartography.Cartography;
import org.sitmun.domain.task.Task;
import org.sitmun.domain.tree.Tree;

@DisplayName("TreeNodeRadioPolicy")
class TreeNodeRadioPolicyTest {

  @Test
  @DisplayName("isCartographyLeaf is true when cartography is set and task is null")
  void isCartographyLeafWhenCartographyOnly() {
    TreeNode node = new TreeNode();
    node.setCartography(new Cartography());
    node.setTask(null);

    assertThat(TreeNodeRadioPolicy.isCartographyLeaf(node)).isTrue();
  }

  @Test
  @DisplayName("isCartographyLeaf is false for folders")
  void isCartographyLeafFalseForFolder() {
    TreeNode node = new TreeNode();
    node.setCartography(null);
    node.setTask(null);

    assertThat(TreeNodeRadioPolicy.isCartographyLeaf(node)).isFalse();
  }

  @Test
  @DisplayName("isCartographyLeaf is false for task nodes")
  void isCartographyLeafFalseForTaskNode() {
    TreeNode node = new TreeNode();
    node.setCartography(null);
    node.setTask(new Task());

    assertThat(TreeNodeRadioPolicy.isCartographyLeaf(node)).isFalse();
  }

  @Test
  @DisplayName("isCartographyLeaf is false when both cartography and task are set")
  void isCartographyLeafFalseWhenBothSet() {
    TreeNode node = new TreeNode();
    node.setCartography(new Cartography());
    node.setTask(new Task());

    assertThat(TreeNodeRadioPolicy.isCartographyLeaf(node)).isFalse();
  }

  @Test
  @DisplayName("canExposeRadio is true only for cartography tree folders")
  void canExposeRadioOnlyOnCartographyTreeFolders() {
    Tree cartographyTree = Tree.builder().type("cartography").build();
    Tree touristicTree = Tree.builder().type("touristic").build();

    TreeNode folder = new TreeNode();
    folder.setTree(cartographyTree);
    folder.setCartography(null);
    folder.setTask(null);

    TreeNode leaf = new TreeNode();
    leaf.setTree(cartographyTree);
    leaf.setCartography(new Cartography());

    TreeNode touristicFolder = new TreeNode();
    touristicFolder.setTree(touristicTree);
    touristicFolder.setCartography(null);
    touristicFolder.setTask(null);

    assertThat(TreeNodeRadioPolicy.canExposeRadio(folder)).isTrue();
    assertThat(TreeNodeRadioPolicy.canExposeRadio(leaf)).isFalse();
    assertThat(TreeNodeRadioPolicy.canExposeRadio(touristicFolder)).isFalse();
  }
}
