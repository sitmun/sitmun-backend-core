package org.sitmun.authorization.client.dto;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.sitmun.domain.DomainConstants;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.task.Task;
import org.sitmun.domain.territory.Territory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Service for handling moreInfo tasks in SITMUN. Maps moreInfo tasks to DTOs and manages their parameters.
 */
@Slf4j
@Component
public class TaskMoreInfoService implements TaskMapper {

  @Value("${sitmun.proxy-middleware.url:}")
  private String proxyUrl;

  /**
   * Determines if a task is a moreInfo task.
   *
   * @param task The task to check
   * @return true if the task is a moreInfo task
   */
  public boolean accept(Task task) {
    String taskType = task.getType() != null ? task.getType().getTitle() : null;
    return (taskType != null && taskType.equalsIgnoreCase("moreInfo"));
  }

  /**
   * Maps a moreInfo task to a TaskDto.
   *
   * @param task The moreInfo task
   * @param application The application context
   * @param territory The territory context
   * @return TaskDto containing task information and parameters
   */
  public TaskDto map(Task task, Application application, Territory territory) {
    String uiControl = null;
    String type = null;
    if (task.getUi() != null) {
      uiControl = task.getUi().getName();
      type = task.getUi().getType();
    }
    Map<String, Object> parameters = new HashMap<>();
    Map<String, Object> properties = task.getProperties();
    if (properties != null) {
      parameters = convertToJsonObject(properties);
    }
    String name = task.getName();
    String cartographyId = task.getCartography() != null ? String.valueOf(task.getCartography().getId()) : null;
    final Object scopeObj = properties.get("scope");
    final String scope = scopeObj != null ? scopeObj.toString() : null;
    String command = properties != null && properties.get("command") != null ? properties.get("command").toString() : null;

    final TaskDto.TaskDtoBuilder taskBuilder = TaskDto.builder()
      .id("task/" + task.getId())
      .name(name)
      .uiControl(uiControl)
      .type(type)
      .parameters(parameters)
      .cartographyId(cartographyId)
      .scope(scope)
      .command(command);

    if (StringUtils.hasText(scope) && task.getId() != null) {
      String id = String.valueOf(task.getId());
      taskBuilder.url(proxyUrl + "/proxy/" + application.getId() + "/" + territory.getId() + "/" + scope + "/" + id);
    }

    return taskBuilder.build();
  }

  /**
   * Converts task properties to a parameter map.
   *
   * @param properties The task properties to convert
   * @return Map of parameter names to their value, or null if no parameters
   */
  @Nullable
  private Map<String, Object> convertToJsonObject(Map<String, Object> properties) {
    Map<String, Object> parameters = new HashMap<>();
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> listOfParameters =
      (List<Map<String, Object>>) properties.getOrDefault(
        DomainConstants.Tasks.PROPERTY_PARAMETERS, Collections.emptyList());
    for (Map<String, Object> param : listOfParameters) {
      if (param.containsKey("label")) {
        String label = String.valueOf(param.get("label"));
        parameters.put(label, param);
      }
    }
    return parameters.isEmpty() ? null : parameters;
  }
}
