package org.sitmun.domain.tree;

import java.util.List;
import org.sitmun.domain.DomainConstants;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.application.tree.ApplicationTree;
import org.sitmun.infrastructure.persistence.exception.BusinessRuleException;
import org.sitmun.infrastructure.web.dto.ProblemTypes;

public final class TreeApplicationLinkPolicy {

  private TreeApplicationLinkPolicy() {}

  public static void validateTreeApplications(Tree tree, List<Application> applications) {
    if (DomainConstants.Trees.isTouristicTree(tree)) {
      validateTouristicTree(applications);
    } else {
      validateNoTouristicTree(applications);
    }
  }

  public static void validateApplicationTrees(Application application, List<Tree> trees) {
    trees.forEach(tree -> validateTreeApplications(tree, List.of(application)));
  }

  private static void validateTouristicTree(List<Application> apps) {
    if (apps.isEmpty()) {
      return;
    }
    if (apps.size() == 1 && DomainConstants.Applications.isTouristicApplication(apps.get(0))) {
      return;
    }
    throw new BusinessRuleException(
        ProblemTypes.TOURISTIC_TREE_CONSTRAINT,
        "Touristic tree can only be linked with 0 or 1 tourist application");
  }

  private static void validateNoTouristicTree(List<Application> apps) {
    boolean valid = apps.stream().allMatch(TreeApplicationLinkPolicy::validAppTrees);
    if (!valid) {
      throw new BusinessRuleException(
          ProblemTypes.NON_TOURISTIC_TREE_CONSTRAINT,
          "A non-touristic tree can only be linked to a non-tourist application or touristic application with only one touristic tree");
    }
  }

  private static boolean validAppTrees(Application app) {
    if (DomainConstants.Applications.isTouristicApplication(app)) {
      return validateTouristicApp(app);
    }
    return true;
  }

  private static boolean validateTouristicApp(Application app) {
    List<Tree> trees = app.getTrees().stream().map(ApplicationTree::getTree).toList();
    return trees.size() == 1 && DomainConstants.Trees.isTouristicTree(trees.get(0));
  }
}
