package org.sitmun.authorization.client.dto;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.sitmun.authorization.client.AuthorizationConstants;
import org.sitmun.domain.DomainConstants;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.task.Task;
import org.sitmun.domain.territory.Territory;
import org.sitmun.infrastructure.util.ParameterValidator;
import org.sitmun.infrastructure.util.TaskParameterUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Service for handling SQL query tasks in SITMUN. Maps SQL query tasks to DTOs and manages their
 * parameters.
 */
@Slf4j
@Component
public class TaskQuerySqlService implements TaskMapper {

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
    Map<String, Object> properties = task.getProperties();
    ParameterValidator.validateProvidedFlag(properties);

    Map<String, Object> parameters = new HashMap<>();
    if (properties != null) {
      parameters = convertToJsonObject(properties);
    }

    String url = ProxyUrlBuilder.forSqlTask(proxyUrl, application, territory, task);

    return TaskDto.builder()
        .id("task/" + task.getId())
        .type(AuthorizationConstants.TaskDto.SIMPLE)
        .parameters(parameters)
        .url(url)
        .build();
  }

  /**
   * Converts task properties to a parameter map.
   *
   * @param properties The task properties to convert
   * @return Map of parameter names to their type and required status, or null if no parameters
   */
  @Nullable
  private Map<String, Object> convertToJsonObject(Map<String, Object> properties) {
    Map<String, Object> parameters = new HashMap<>();

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> listOfParameters =
        (List<Map<String, Object>>)
            properties.getOrDefault(
                DomainConstants.Tasks.PROPERTY_PARAMETERS, Collections.emptyList());

    for (Map<String, Object> param : listOfParameters) {
      Object provided = param.get(DomainConstants.Tasks.PARAMETERS_PROVIDED);
      boolean isProvided =
          Boolean.TRUE.equals(provided) || "true".equalsIgnoreCase(String.valueOf(provided));

      if (isProvided) {
        continue;
      }

      String name = TaskParameterUtil.getParameterVariable(param);

      if (name != null
          && param.containsKey(DomainConstants.Tasks.PARAMETERS_TYPE)
          && param.containsKey(DomainConstants.Tasks.PARAMETERS_REQUIRED)) {
        String type = String.valueOf(param.get(DomainConstants.Tasks.PARAMETERS_TYPE));
        Boolean required =
            Boolean.valueOf(String.valueOf(param.get(DomainConstants.Tasks.PARAMETERS_REQUIRED)));

        Map<String, Object> values = new HashMap<>();
        values.put(AuthorizationConstants.TaskDto.PARAMETER_TYPE, type);
        values.put(AuthorizationConstants.TaskDto.PARAMETER_REQUIRED, required);
        parameters.put(name, values);
      }
    }

    return parameters.isEmpty() ? null : parameters;
  }
}
