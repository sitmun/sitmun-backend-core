package org.sitmun.domain.tree;

import org.sitmun.domain.tree.node.TreeNodeRadioPolicy;
import org.sitmun.domain.tree.node.TreeNodeRepository;
import org.sitmun.infrastructure.persistence.exception.BusinessRuleException;
import org.sitmun.infrastructure.web.dto.ProblemTypes;

/** Validation rules for tree type changes that affect radio folder configuration. */
public final class TreeRadioTypePolicy {

  private TreeRadioTypePolicy() {}

  public static void validateRadioFoldersBeforeLeavingCartography(
      Tree tree, String newType, TreeNodeRepository treeNodeRepository) {
    if (tree == null || tree.getId() == null) {
      return;
    }
    validateRadioFoldersBeforeLeavingCartography(
        tree.getType(), newType, tree.getId(), treeNodeRepository);
  }

  public static void validateRadioFoldersBeforeLeavingCartography(
      String priorType, String newType, Integer treeId, TreeNodeRepository treeNodeRepository) {
    if (!TreeNodeRadioPolicy.isRadioAllowedTree(Tree.builder().type(priorType).build())
        || TreeNodeRadioPolicy.isRadioAllowedTree(Tree.builder().type(newType).build())) {
      return;
    }
    if (treeNodeRepository.existsRadioFolderInTree(treeId)) {
      throw new BusinessRuleException(
          ProblemTypes.TREE_TYPE_CHANGE_CONSTRAINT,
          "Cartography tree type cannot be changed while radio folders are configured");
    }
  }
}
