package org.sitmun.authorization.client.dto;

import org.sitmun.domain.application.Application;
import org.sitmun.domain.task.Task;
import org.sitmun.domain.territory.Territory;

public interface TaskMapper {

  boolean accept(Task task);

  TaskDto map(Task task, Application application, Territory territory);
}
