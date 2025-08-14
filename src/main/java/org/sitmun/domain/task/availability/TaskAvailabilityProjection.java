package org.sitmun.domain.task.availability;

import java.util.Date;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

@Projection(name = "view", types = TaskAvailability.class)
public interface TaskAvailabilityProjection {

  @Value("#{target.id}")
  Integer getId();

  /** Created date. */
  @Value("#{target.createdDate}")
  Date getCreatedDate();

  /** Identifier of the territory allowed to access to the task. */
  @Value("#{target.territory?.id}")
  Integer getTerritoryId();

  /** Name of the territory allowed to access to the task. */
  @Value("#{target.territory?.name}")
  String getTerritoryName();

  /** Code of the territory allowed to access to the task. */
  @Value("#{target.territory?.code}")
  String getTerritoryCode();

  /** Name of the territory type allowed to the territory. */
  @Value("#{target.territory?.type?.name}")
  String getTerritoryTypeName();

  /** Identifier of the task allowed to the territory. */
  @Value("#{target.task?.id}")
  Integer getTaskId();

  @Value("#{target.task?.name}")
  String getTaskName();

  @Value("#{target.task?.type?.name}")
  String getTaskTypeName();

  /** Name of the group of the task allowed to the territory. */
  @Value("#{target.task?.group?.name}")
  String getTaskGroupName();
}
