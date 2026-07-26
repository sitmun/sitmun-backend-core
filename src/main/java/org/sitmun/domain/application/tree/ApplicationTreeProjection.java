package org.sitmun.domain.application.tree;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

@Projection(name = "view", types = ApplicationTree.class)
public interface ApplicationTreeProjection {

  @Value("#{target.id}")
  Integer getId();

  @Value("#{target.order}")
  Integer getOrder();

  @Value("#{target.tree?.name}")
  String getTreeName();

  @Value("#{target.tree?.id}")
  Integer getTreeId();

  @Value("#{target.tree?.description}")
  String getTreeDescription();

  @Value("#{target.tree?.type}")
  String getTreeType();

  @Value("#{target.application?.name}")
  String getApplicationName();

  @Value("#{target.application?.id}")
  Integer getApplicationId();
}
