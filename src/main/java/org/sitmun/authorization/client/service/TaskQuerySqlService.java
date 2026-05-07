package org.sitmun.authorization.client.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.sitmun.authorization.client.AuthorizationConstants;
import org.sitmun.authorization.client.dto.TaskDto;
import org.sitmun.authorization.client.support.ProxyUrlBuilder;
import org.sitmun.domain.DomainConstants;
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
    return DomainConstants.Tasks.isSqlQueryTask(task);
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
    Map<String, Object> parametersDto = convertToClientProfile(parameters);

    String url = ProxyUrlBuilder.forSqlTask(proxyUrl, application, territory, task);

    return TaskDto.builder()
        .id("task/" + task.getId())
        .type(AuthorizationConstants.TaskDto.SIMPLE)
        .parameters(parametersDto)
        .url(url)
        .build();
  }

  /**
   * Converts parsed task parameters to client profile DTO format. Only includes client-allowed
   * parameters (excludes backend-only LOCKED and PROVIDED parameters).
   *
   * <p>Output format: {@code {name -> {type, required}}}
   *
   * @param parameters The parsed task parameters
   * @return Map of parameter names to their type and required status, or null if no parameters
   */
  @Nullable
  private Map<String, Object> convertToClientProfile(List<TaskParameter> parameters) {
    Map<String, Object> result = new HashMap<>();

    for (TaskParameter param : parameters) {
      if (taskParameterProcessor.classify(param).isBackendOnly()) {
        continue;
      }
      result.put(param.name(), taskParameterProcessor.toSimpleParameterDto(param, "string"));
    }

    return result.isEmpty() ? null : result;
  }
}
