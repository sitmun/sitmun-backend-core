package org.sitmun.plugin.core.domain.projections;


import org.sitmun.plugin.core.domain.Task;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

import java.util.Date;

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

  @Value("#{target.group != null? target.group.name : null }")
  String getGroupName();

  @Value("#{target.group != null? target.group.id : null }")
  Integer getGroupId();

  @Value("#{target.ui != null? target.ui.id : null }")
  Integer getUiId();

}
