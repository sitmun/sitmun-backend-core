package org.sitmun.infrastructure.persistence.type.tree;

import org.jetbrains.annotations.ApiStatus;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.tree.Tree;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Set;

import javax.persistence.Persistence;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

@Slf4j
@ApiStatus.Experimental
public class TreeValidator implements ConstraintValidator<TreeType, Tree> {

  @Override
  public boolean isValid(Tree tree, ConstraintValidatorContext constraintValidatorContext) {
    if (tree != null) {
      Set<Application> availableApplications = tree.getAvailableApplications();
      if (tree.getId() == null || availableApplications == null || !Persistence.getPersistenceUtil().isLoaded(availableApplications)) {
        return true;
      }
      List<Application> apps = List.copyOf(tree.getAvailableApplications());
      boolean valid = true;
      if ("tourist".equals(tree.getType())) {
        valid = validateTuristicTree(apps);
      } else {
        valid = validateNoTuristicTree(apps);
      }
      return valid;
    }
    return true;
  }

  private boolean validateTuristicTree(List<Application> apps) {
    boolean valid = false;
    if (apps.isEmpty() || (apps.size() == 1 && "T".equals(apps.get(0).getType()))) {
      valid = true;
    }
    return valid;
  }

  private boolean validateNoTuristicTree(List<Application> apps) {
    boolean valid = true;
    valid = apps.stream().allMatch(a -> validAppTrees(a));
    return valid;
  }

  private boolean validAppTrees(Application app) {
    boolean valid = !"T".equals(app.getType());
    if (!valid) {
      valid = validateTuristicApp(app);
    }
    return valid;
  }

  private boolean validateTuristicApp(Application app) {
    List<Tree> trees = List.copyOf(app.getTrees());
    boolean valid = trees.isEmpty();
    if (trees.size() == 1 && "tourist".equals(trees.get(0).getType())) {
      valid = true;
    }
    return valid;
  }
}
