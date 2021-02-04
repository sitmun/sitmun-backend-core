package org.sitmun.plugin.core.domain.projections;

import org.sitmun.plugin.core.domain.TaskAvailability;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

import java.util.Date;

@Projection(name = "view", types = {TaskAvailability.class})
public interface TaskAvailabilityProjection {

  @Value("#{target.id}")
  Integer getId();

  /**
   * Created date.
   */
  @Value("#{target.createdDate}")
  Date getCreatedDate();

  /**
   * Id of the territory allowed to access to the task.
   */
  @Value("#{target.territory != null? target.territory.id : null}")
  Integer getTerritoryId();

  /**
   * Name of the territory allowed to access to the task.
   */
  @Value("#{target.territory != null? target.territory.name : null}")
  String getTerritoryName();

  /**
   * Id of the task allowed to the territory.
   */
  @Value("#{target.task != null? target.task.id : null}")
  Integer getTaskId();

  /**
   * Name of the group of the task allowed to the territory.
   */
  @Value("#{target.task != null? (target.task.group != null? target.task.group.name : null) : null}")
  String getTaskGroupName();
}
