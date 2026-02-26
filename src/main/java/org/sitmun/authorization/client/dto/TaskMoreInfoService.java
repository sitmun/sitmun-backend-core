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
import org.sitmun.infrastructure.util.ParameterValidator;
import org.sitmun.infrastructure.util.TaskParameterUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Service for handling moreInfo tasks in SITMUN. Maps moreInfo tasks to DTOs and manages their
 * parameters.
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
    return DomainConstants.Tasks.isMoreInfoTask(task);
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
    Map<String, Object> properties = task.getProperties();
    ParameterValidator.validateProvidedFlag(properties);

    String uiControl = null;
    String type = null;
    if (task.getUi() != null) {
      uiControl = task.getUi().getName();
      type = task.getUi().getType();
    }
    Map<String, Object> parameters = new HashMap<>();
    if (properties != null) {
      parameters = convertToJsonObject(properties);
    }
    // More info: command is never exposed to client (URL/API/SQL may contain secrets)

    String name = task.getName();
    String cartographyId =
        task.getCartography() != null ? String.valueOf(task.getCartography().getId()) : null;
    final Object scopeObj =
        properties != null ? properties.get(DomainConstants.Tasks.PROPERTY_SCOPE) : null;
    final String scope = scopeObj != null ? scopeObj.toString() : null;

    final TaskDto.TaskDtoBuilder taskBuilder =
        TaskDto.builder()
            .id("task/" + task.getId())
            .name(name)
            .uiControl(uiControl)
            .type(type)
            .parameters(parameters)
            .cartographyId(cartographyId)
            .scope(scope)
            .command(null);

    if (StringUtils.hasText(scope) && task.getId() != null) {
      // URL scope: use command directly (external redirect, no proxy)
      // API/SQL scopes: route through proxy middleware
      if (DomainConstants.Tasks.SCOPE_URL.equalsIgnoreCase(scope)) {
        String command =
            properties != null
                ? (String) properties.get(DomainConstants.Tasks.PROPERTY_COMMAND)
                : null;
        taskBuilder.url(command);
      } else {
        String id = String.valueOf(task.getId());
        taskBuilder.url(
            ProxyUrlBuilder.forScopedResource(proxyUrl, application, territory, scope, id));
      }
    }

    return taskBuilder.build();
  }

  /**
   * Converts task properties to a parameter map with backward-compatible structure. Filters out
   * provided (backend-only) variables for security.
   *
   * @param properties The task properties to convert
   * @return Map of parameter names to their value, or null if no parameters
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
      // SECURITY: Filter out provided variables (backend-only secrets)
      Object provided = param.get(DomainConstants.Tasks.PARAMETERS_PROVIDED);
      boolean isProvided =
          Boolean.TRUE.equals(provided) || "true".equalsIgnoreCase(String.valueOf(provided));
      if (isProvided) {
        continue; // Skip secrets - never expose to client
      }

      // MIGRATION COMPATIBILITY: Support both old (name/label) and new (variable) keys
      String variable = TaskParameterUtil.getParameterVariable(param);
      if (variable == null) {
        // Additional fallback to label for legacy data
        variable = (String) param.get(DomainConstants.Tasks.PARAMETERS_LABEL);
      }

      String field =
          (String)
              param.getOrDefault(
                  DomainConstants.Tasks.PARAMETERS_FIELD,
                  param.get(DomainConstants.Tasks.PARAMETERS_VALUE));

      if (variable != null) {
        // Build backward-compatible DTO entry for Viewer
        Map<String, Object> dtoParam = new HashMap<>();
        dtoParam.put(DomainConstants.Tasks.PARAMETERS_LABEL, variable); // Viewer expects "label"
        dtoParam.put(DomainConstants.Tasks.PARAMETERS_VALUE, field); // Viewer expects "value"
        dtoParam.put(DomainConstants.Tasks.PARAMETERS_NAME, variable); // New standard key

        // Preserve type and required if present
        if (param.containsKey(DomainConstants.Tasks.PARAMETERS_TYPE)) {
          dtoParam.put(
              DomainConstants.Tasks.PARAMETERS_TYPE,
              param.get(DomainConstants.Tasks.PARAMETERS_TYPE));
        }
        if (param.containsKey(DomainConstants.Tasks.PARAMETERS_REQUIRED)) {
          dtoParam.put(
              DomainConstants.Tasks.PARAMETERS_REQUIRED,
              param.get(DomainConstants.Tasks.PARAMETERS_REQUIRED));
        }

        // Key by variable name (NOT by label)
        parameters.put(variable, dtoParam);
      }
    }

    return parameters.isEmpty() ? null : parameters;
  }
}
