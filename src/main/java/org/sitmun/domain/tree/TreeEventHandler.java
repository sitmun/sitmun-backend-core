package org.sitmun.domain.tree;

import jakarta.persistence.EntityManager;
import jakarta.persistence.FlushModeType;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.application.tree.ApplicationTree;
import org.sitmun.domain.tree.node.TreeNodeRepository;
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

  // linked is Object: SDR calls this for every association, not only availableApplications
  @HandleBeforeLinkSave
  @Transactional(rollbackFor = RequirementException.class)
  public void handleTreeApplicationLink(
      @NotNull Tree tree, @SuppressWarnings("unused") Object linked) {
    List<Application> apps =
        tree.getAvailableApplications().stream().map(ApplicationTree::getApplication).toList();
    TreeApplicationLinkPolicy.validateTreeApplications(tree, apps);
  }
}
