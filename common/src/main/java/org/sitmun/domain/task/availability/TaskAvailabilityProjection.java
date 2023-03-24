package org.sitmun.domain.task.availability;

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
  @Value("#{target.territory?.id}")
  Integer getTerritoryId();

  /**
   * Name of the territory allowed to access to the task.
   */
  @Value("#{target.territory?.name}")
  String getTerritoryName();

  /**
   * Id of the task allowed to the territory.
   */
  @Value("#{target.task?.id}")
  Integer getTaskId();

  /**
   * Name of the group of the task allowed to the territory.
   */
  @Value("#{target.task?.group?.name}")
  String getTaskGroupName();
}
