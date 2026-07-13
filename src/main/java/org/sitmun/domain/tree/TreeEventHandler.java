package org.sitmun.domain.tree;

import jakarta.persistence.EntityManager;
import jakarta.persistence.FlushModeType;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Set;
import org.sitmun.domain.DomainConstants;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.tree.node.TreeNodeRepository;
import org.sitmun.infrastructure.persistence.exception.BusinessRuleException;
import org.sitmun.infrastructure.persistence.exception.RequirementException;
import org.sitmun.infrastructure.persistence.type.image.ImageTransformer;
import org.sitmun.infrastructure.web.dto.ProblemTypes;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.HandleBeforeLinkSave;
import org.springframework.data.rest.core.annotation.HandleBeforeSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RepositoryEventHandler
public class TreeEventHandler {

  private final ImageTransformer imageTransformer;
  private final TreeRepository treeRepository;
  private final TreeNodeRepository treeNodeRepository;
  private final EntityManager entityManager;

  TreeEventHandler(
      ImageTransformer imageTransformer,
      TreeRepository treeRepository,
      TreeNodeRepository treeNodeRepository,
      EntityManager entityManager) {
    this.imageTransformer = imageTransformer;
    this.treeRepository = treeRepository;
    this.treeNodeRepository = treeNodeRepository;
    this.entityManager = entityManager;
  }

  @HandleBeforeSave
  @HandleBeforeCreate
  @Transactional(rollbackFor = RequirementException.class)
  public void handleTreeSave(@NotNull Tree tree) {
    if (tree.getId() != null) {
      validateTypeChange(tree);
    }
    tree.setImage(imageTransformer.scaleImage(tree.getImage(), tree.getType()));
  }

  private void validateTypeChange(Tree tree) {
    FlushModeType previousFlushMode = entityManager.getFlushMode();
    try {
      entityManager.setFlushMode(FlushModeType.COMMIT);
      treeRepository
          .findPersistedTypeById(tree.getId())
          .ifPresent(
              priorType ->
                  TreeRadioTypePolicy.validateRadioFoldersBeforeLeavingCartography(
                      priorType, tree.getType(), tree.getId(), treeNodeRepository));
    } finally {
      entityManager.setFlushMode(previousFlushMode);
    }
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
      return;
    }
    if (apps.size() == 1 && DomainConstants.Applications.isTouristicApplication(apps.get(0))) {
      return;
    }
    throw new BusinessRuleException(
        ProblemTypes.TOURISTIC_TREE_CONSTRAINT,
        "Touristic tree can only be linked with 0 or 1 tourist application");
  }

  private void validateNoTouristicTree(List<Application> apps) {
    boolean valid = apps.stream().allMatch(this::validAppTrees);

    if (!valid) {
      throw new BusinessRuleException(
          ProblemTypes.NON_TOURISTIC_TREE_CONSTRAINT,
          "A non-touristic tree can only be linked to a non-tourist application or touristic application with only one touristic tree");
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
