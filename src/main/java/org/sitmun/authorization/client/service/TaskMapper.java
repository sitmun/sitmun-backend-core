package org.sitmun.authorization.client.service;

import org.sitmun.authorization.client.dto.TaskDto;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.task.Task;
import org.sitmun.domain.territory.Territory;

public interface TaskMapper {

  boolean accept(Task task);

  TaskDto map(Task task, Application application, Territory territory);
}
