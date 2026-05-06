package org.sitmun.authorization.client.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.sitmun.authorization.client.dto.TaskDto;
import org.sitmun.authorization.client.support.ProxyUrlBuilder;
import org.sitmun.domain.DomainConstants;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.task.MoreInfoTaskResolver;
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

  private final MoreInfoTaskResolver moreInfoTaskResolver;

  public TaskMoreInfoService(MoreInfoTaskResolver moreInfoTaskResolver) {
    this.moreInfoTaskResolver = moreInfoTaskResolver;
  }

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
    Task executionTask = moreInfoTaskResolver.findRelatedQueryTask(task).orElse(task);
    Map<String, Object> executionProperties = executionTask.getProperties();

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

    String name = task.getName();
    String cartographyId =
        task.getCartography() != null ? String.valueOf(task.getCartography().getId()) : null;
    final String scope = normalizeExecutionScope(executionProperties);
    final String mimeType =
        extractStringProperty(executionProperties, DomainConstants.Tasks.PROPERTY_MIME_TYPE);
    final String filename =
        extractStringProperty(executionProperties, DomainConstants.Tasks.PROPERTY_FILENAME);
    final String url = resolveUrl(scope, task, executionProperties, application, territory);

    return TaskDto.builder()
        .id("task/" + task.getId())
        .name(name)
        .uiControl(uiControl)
        .type(type)
        .parameters(parameters)
        .cartographyId(cartographyId)
        .scope(scope)
        .mimeType(mimeType)
        .filename(filename)
        .url(url)
        .command(null)
        .build();
  }

  private String extractStringProperty(Map<String, Object> properties, String key) {
    if (properties == null) {
      return null;
    }
    Object value = properties.get(key);
    return value != null ? value.toString() : null;
  }

  private String resolveUrl(
      String scope,
      Task task,
      Map<String, Object> executionProperties,
      Application application,
      Territory territory) {
    if (!StringUtils.hasText(scope) || task.getId() == null) {
      return null;
    }
    if (DomainConstants.Tasks.SCOPE_RESOURCE.equalsIgnoreCase(scope)
        || DomainConstants.Tasks.SCOPE_URL.equalsIgnoreCase(scope)) {
      return extractStringProperty(executionProperties, DomainConstants.Tasks.PROPERTY_COMMAND);
    }
    return ProxyUrlBuilder.forScopedResource(
        proxyUrl, application, territory, scope, String.valueOf(task.getId()));
  }

  private String normalizeExecutionScope(Map<String, Object> properties) {
    if (properties == null) {
      return null;
    }
    Object scopeObj = properties.get(DomainConstants.Tasks.PROPERTY_SCOPE);
    if (scopeObj == null) {
      return null;
    }
    String scope = scopeObj.toString();
    if (DomainConstants.Tasks.SCOPE_SQL_QUERY.equalsIgnoreCase(scope)) {
      return DomainConstants.Tasks.SCOPE_SQL;
    }
    if (DomainConstants.Tasks.SCOPE_WEB_API_QUERY.equalsIgnoreCase(scope)) {
      return DomainConstants.Tasks.SCOPE_API;
    }
    if (DomainConstants.Tasks.SCOPE_WEB_API_QUERY_NO_PROXY.equalsIgnoreCase(scope)) {
      // No-proxy with mimeType → RESOURCE (mimeType-driven rendering, direct fetch)
      // No-proxy without mimeType → URL (external redirect)
      Object mimeTypeObj = properties.get(DomainConstants.Tasks.PROPERTY_MIME_TYPE);
      boolean hasMimeType = mimeTypeObj != null && StringUtils.hasText(mimeTypeObj.toString());
      return hasMimeType ? DomainConstants.Tasks.SCOPE_RESOURCE : DomainConstants.Tasks.SCOPE_URL;
    }
    if (DomainConstants.Tasks.SCOPE_URL_QUERY.equalsIgnoreCase(scope)) {
      return DomainConstants.Tasks.SCOPE_URL;
    }
    return scope;
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
