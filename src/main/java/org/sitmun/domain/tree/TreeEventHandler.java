package org.sitmun.domain.tree;

import org.sitmun.domain.application.Application;
import org.sitmun.infrastructure.persistence.exception.RequirementException;
import org.sitmun.infrastructure.persistence.type.image.ImageTransformer;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.HandleBeforeLinkSave;
import org.springframework.data.rest.core.annotation.HandleBeforeSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

import javax.validation.constraints.NotNull;

@Component
@RepositoryEventHandler
public class TreeEventHandler {

  ImageTransformer imageTransformer;

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
  public void handleTreeApplicationLink(@NotNull Tree tree, Set<Application> ignoredLink) {
    List<Application> apps = List.copyOf(tree.getAvailableApplications());
    if ("touristic".equals(tree.getType())) {
      validateTouristicTree(apps);
    } else {
      validateNoTouristicTree(apps);
    }
  }

  private void validateTouristicTree(List<Application> apps) {
    if (!(apps.isEmpty() || (apps.size() == 1 && "T".equals(apps.get(0).getType())))) {
      throw new RequirementException("Touristic tree only can be linked with 0..1 tourist application");
    }
  }

  private void validateNoTouristicTree(List<Application> apps) {
    boolean valid = apps.stream().allMatch(this::validAppTrees);
    
    if (!valid) {
      throw new RequirementException("A non touristic tree can only be linked to a non tourist application or touristic application with only one touristic tree");
    }
  }

  private boolean validAppTrees(Application app) {
    boolean valid = !"T".equals(app.getType());
    if (!valid) {
      valid = validateTouristicApp(app);
    }
    return valid;
  }

  private boolean validateTouristicApp(Application app) {
    List<Tree> trees = List.copyOf(app.getTrees());
    return trees.size() == 1 && "touristic".equals(trees.get(0).getType());
  }
}