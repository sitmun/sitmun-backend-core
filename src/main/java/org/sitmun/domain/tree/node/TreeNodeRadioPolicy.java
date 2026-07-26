package org.sitmun.domain.tree.node;

import java.util.Set;
import org.sitmun.domain.tree.Tree;

/** Validation rules for {@link TreeNode#getRadio()} on cartography tree folders. */
public final class TreeNodeRadioPolicy {

  private static final Set<String> RADIO_TREE_TYPES = Set.of("cartography");

  private TreeNodeRadioPolicy() {}

  public static boolean isFolder(TreeNode node) {
    return node.getCartography() == null && node.getTask() == null;
  }

  public static boolean isCartographyLeaf(TreeNode node) {
    return node.getCartography() != null && node.getTask() == null;
  }

  public static boolean isRadioAllowedTree(Tree tree) {
    return tree != null && RADIO_TREE_TYPES.contains(tree.getType());
  }

  public static boolean canExposeRadio(TreeNode node) {
    return isFolder(node) && isRadioAllowedTree(node.getTree());
  }
}
