package org.sitmun.domain.tree;

import org.sitmun.infrastructure.persistence.exception.RequirementException;
import org.sitmun.infrastructure.persistence.type.image.ImageTransformer;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.HandleBeforeSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
}