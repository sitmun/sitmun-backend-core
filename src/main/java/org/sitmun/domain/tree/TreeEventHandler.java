package org.sitmun.domain.tree;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Set;
import org.sitmun.domain.DomainConstants;
import org.sitmun.domain.application.Application;
import org.sitmun.infrastructure.persistence.exception.RequirementException;
import org.sitmun.infrastructure.persistence.type.image.ImageTransformer;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.HandleBeforeLinkSave;
import org.springframework.data.rest.core.annotation.HandleBeforeSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RepositoryEventHandler
public class TreeEventHandler {

  final ImageTransformer imageTransformer;

  TreeEventHandler(ImageTransformer imageTransformer) {
    this.imageTransformer = imageTransformer;
  }

  @HandleBeforeSave
  @HandleBeforeCreate
  @Transactional(rollbackFor = RequirementException.class)
  public void handleTreeNodeCreate(@NotNull Tree tree) {
    tree.setImage(imageTransformer.scaleImage(tree.getImage(), tree.getType()));
  }

  @HandleBeforeLinkSave
  @Transactional(rollbackFor = RequirementException.class)
  public void handleTreeApplicationLink(@NotNull Tree tree, Set<Application> ignoredOldLinks) {
    List<Application> apps = List.copyOf(tree.getAvailableApplications());
    if (DomainConstants.Trees.isTouristicTree(tree)) {
      validateTouristicTree(apps);
    } else {
      validateNoTouristicTree(apps);
    }
  }

  private void validateTouristicTree(List<Application> apps) {
    if (apps.isEmpty()) {
      return; // No applications linked, valid case
    }
    if (apps.size() == 1 && DomainConstants.Applications.isTouristicApplication(apps.get(0))) {
      return; // Valid case with one touristic application
    }
    throw new RequirementException(
        "Touristic tree only can be linked with 0..1 tourist application");
  }

  private void validateNoTouristicTree(List<Application> apps) {
    boolean valid = apps.stream().allMatch(this::validAppTrees);

    if (!valid) {
      throw new RequirementException(
          "A non touristic tree can only be linked to a non tourist application or touristic application with only one touristic tree");
    }
  }

  private boolean validAppTrees(Application app) {
    if (DomainConstants.Applications.isTouristicApplication(app)) {
      return validateTouristicApp(app);
    }
    return true;
  }

  private boolean validateTouristicApp(Application app) {
    List<Tree> trees = List.copyOf(app.getTrees());
    return trees.size() == 1 && DomainConstants.Trees.isTouristicTree(trees.get(0));
  }
}
