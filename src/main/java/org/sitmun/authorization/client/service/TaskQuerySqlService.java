package org.sitmun.authorization.client.service;

import static org.sitmun.authorization.client.AuthorizationConstants.TaskDto.SIMPLE;
import static org.sitmun.domain.DomainConstants.Tasks.SCOPE_SQL;
import static org.sitmun.domain.DomainConstants.Tasks.TASK_PROFILE_ID_PREFIX;
import static org.sitmun.domain.DomainConstants.Tasks.isSqlQueryTask;
import static org.sitmun.domain.task.parameter.TaskParameterProcessor.ProfileParameterShape.SIMPLE_STRING_DEFAULT;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sitmun.authorization.client.dto.TaskDto;
import org.sitmun.authorization.client.support.ProxyUrlBuilder;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.task.Task;
import org.sitmun.domain.task.parameter.TaskParameter;
import org.sitmun.domain.task.parameter.TaskParameterProcessor;
import org.sitmun.domain.territory.Territory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Service for handling SQL query tasks in SITMUN. Maps SQL query tasks to DTOs and manages their
 * parameters.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskQuerySqlService implements TaskMapper {

  private final TaskParameterProcessor taskParameterProcessor;

  @Value("${sitmun.proxy-middleware.url:}")
  private String proxyUrl;

  /**
   * Determines if a task is a SQL query task.
   *
   * @param task The task to check
   * @return true if the task is a SQL query task
   */
  public boolean accept(Task task) {
    return isSqlQueryTask(task);
  }

  /**
   * Maps a SQL query task to a TaskDto.
   *
   * @param task The SQL query task
   * @param application The application context
   * @param territory The territory context
   * @return TaskDto containing task information and parameters
   */
  public TaskDto map(Task task, Application application, Territory territory) {
    List<TaskParameter> parameters = taskParameterProcessor.parse(task);
    Map<String, Object> parametersDto =
        taskParameterProcessor.toProfileParameterMap(
            parameters, SIMPLE_STRING_DEFAULT, /* omitUriTemplatePlaceholders */ false);

    String url = ProxyUrlBuilder.forSqlTask(proxyUrl, application, territory, task);

    return TaskDto.builder()
        .id(TASK_PROFILE_ID_PREFIX + task.getId())
        .type(SIMPLE)
        .scope(SCOPE_SQL)
        .parameters(parametersDto)
        .url(url)
        .build();
  }
}
