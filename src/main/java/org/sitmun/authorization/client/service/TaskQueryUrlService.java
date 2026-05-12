package org.sitmun.authorization.client.service;

import static org.sitmun.authorization.client.AuthorizationConstants.TaskDto.SIMPLE;
import static org.sitmun.domain.DomainConstants.Tasks.PROPERTY_COMMAND;
import static org.sitmun.domain.DomainConstants.Tasks.SCOPE_URL;
import static org.sitmun.domain.DomainConstants.Tasks.TASK_PROFILE_ID_PREFIX;
import static org.sitmun.domain.DomainConstants.Tasks.isUrlQueryTask;
import static org.sitmun.domain.task.parameter.TaskParameterProcessor.ProfileParameterShape.EXTERNAL_LINK_VIEWER;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sitmun.authorization.client.dto.TaskDto;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.task.Task;
import org.sitmun.domain.task.parameter.TaskParameter;
import org.sitmun.domain.task.parameter.TaskParameterProcessor;
import org.sitmun.domain.territory.Territory;
import org.springframework.stereotype.Component;

/**
 * Maps external link (URL) query tasks to DTOs. Handles conversion of task properties and
 * parameters for external URL tasks that open in the viewer without backend proxying.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskQueryUrlService implements TaskMapper {

  private final TaskParameterProcessor taskParameterProcessor;

  /**
   * Checks if the task is an external link (URL) query task.
   *
   * @param task Task to check
   * @return true if task is an external link query
   */
  public boolean accept(Task task) {
    return isUrlQueryTask(task);
  }

  /**
   * Maps an external link query task to a TaskDto.
   *
   * @param task Task to map
   * @param application Application context
   * @param territory Territory context
   * @return Mapped TaskDto with URL and parameters
   */
  public TaskDto map(Task task, Application application, Territory territory) {
    Map<String, Object> properties = task.getProperties();

    String url = null;
    Map<String, Object> parametersDto = new HashMap<>();

    if (properties != null) {
      if (properties.get(PROPERTY_COMMAND) != null) {
        url = properties.get(PROPERTY_COMMAND).toString();
      }

      List<TaskParameter> parameters = taskParameterProcessor.parse(task);
      parametersDto =
          taskParameterProcessor.toProfileParameterMap(
              parameters, EXTERNAL_LINK_VIEWER, /* omitUriTemplatePlaceholders */ false);
    }

    return TaskDto.builder()
        .id(TASK_PROFILE_ID_PREFIX + task.getId())
        .type(SIMPLE)
        .typeId(task.getType() != null ? task.getType().getId() : null)
        .scope(SCOPE_URL)
        .parameters(parametersDto)
        .url(url)
        .mimeType(null)
        .filename(null)
        .build();
  }
}
