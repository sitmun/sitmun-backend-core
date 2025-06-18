package org.sitmun.authorization.dto;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.sitmun.authorization.AuthorizationConstants;
import org.sitmun.domain.DomainConstants;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.task.Task;
import org.sitmun.domain.territory.Territory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps web service query tasks to DTOs.
 * Handles conversion of task properties and parameters for web API queries.
 */
@Slf4j
@Component
public class TaskQueryWebService implements TaskMapper {

  /**
   * Checks if the task is a web API query task.
   * @param task Task to check
   * @return true if task is a web API query
   */
  public boolean accept(Task task) {
    return DomainConstants.Tasks.isWebApiQuery(task);
  }

  /**
   * Maps a web service query task to a TaskDto.
   * @param task Task to map
   * @param application Application context
   * @param territory Territory context
   * @return Mapped TaskDto with parameters and URL
   */
  public TaskDto map(Task task, Application application, Territory territory) {
    String url = null;
    Map<String, Object> parameters = new HashMap<>();
    Map<String, Object> properties = task.getProperties();
    if (properties != null) {
      parameters = convertToJsonObject(properties);
      if (properties.get(DomainConstants.Tasks.PROPERTY_COMMAND) != null) {
        url = properties.get(DomainConstants.Tasks.PROPERTY_COMMAND).toString();
      }
    }
    return TaskDto.builder()
      .id("task/" + task.getId())
      .type(AuthorizationConstants.TaskDto.SIMPLE)
      .parameters(parameters)
      .url(url)
      .build();
  }

  /**
   * Converts task properties to a parameter map.
   * @param properties Task properties to convert
   * @return Map of parameter names to their type and required status, or null if empty
   */
  @Nullable
  private Map<String, Object> convertToJsonObject(Map<String, Object> properties) {
    Map<String, Object> parameters = new HashMap<>();

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> listOfParameters = (List<Map<String, Object>>) properties.getOrDefault(DomainConstants.Tasks.PROPERTY_PARAMETERS, Collections.emptyList());

    for (Map<String, Object> param : listOfParameters) {
      if (param.containsKey(DomainConstants.Tasks.PARAMETERS_NAME) &&
        param.containsKey(DomainConstants.Tasks.PARAMETERS_TYPE) &&
        param.containsKey(DomainConstants.Tasks.PARAMETERS_REQUIRED)) {
        String name = String.valueOf(param.get(DomainConstants.Tasks.PARAMETERS_NAME));
        String type = String.valueOf(param.get(DomainConstants.Tasks.PARAMETERS_TYPE));
        Boolean required = Boolean.valueOf(String.valueOf(param.get(DomainConstants.Tasks.PARAMETERS_REQUIRED)));

        Map<String, Object> values = new HashMap<>();
        values.put(AuthorizationConstants.TaskDto.PARAMETER_TYPE, type);
        values.put(AuthorizationConstants.TaskDto.PARAMETER_REQUIRED, required);
        parameters.put(name, values);
      }
    }

    return parameters.isEmpty() ? null : parameters;
  }
}
