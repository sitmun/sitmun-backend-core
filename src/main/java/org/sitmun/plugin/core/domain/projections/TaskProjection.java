package org.sitmun.plugin.core.domain.projections;


import org.sitmun.plugin.core.domain.Task;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

import java.util.Date;
import java.util.Map;

/**
 * Projections for REST views of a task.
 */
@Projection(name = "view", types = {Task.class})
public interface TaskProjection {

  @Value("#{target.id}")
  Long getId();

  @Value("#{target.name}")
  String getName();

  @Value("#{target.createdDate}")
  Date getCreatedDate();

  @Value("#{target.order}")
  Integer getOrder();

  @Value("#{target.group?.name }")
  String getGroupName();

  @Value("#{target.group?.id}")
  Integer getGroupId();

  @Value("#{target.ui?.id}")
  Integer getUiId();

  @Value("#{target.parameters}")
  Map<String, Object> getParameters();
}
