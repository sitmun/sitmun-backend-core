package org.sitmun.authorization.client.service;

import static org.sitmun.domain.DomainConstants.Tasks.*;
import static org.sitmun.domain.DomainConstants.Tasks.PROPERTY_FILENAME;
import static org.sitmun.domain.DomainConstants.Tasks.PROPERTY_MIME_TYPE;
import static org.sitmun.domain.DomainConstants.Tasks.SCOPE_API;
import static org.sitmun.domain.DomainConstants.Tasks.SCOPE_SQL;
import static org.sitmun.domain.DomainConstants.Tasks.SCOPE_SQL_QUERY;
import static org.sitmun.domain.DomainConstants.Tasks.SCOPE_URL_QUERY;
import static org.sitmun.domain.DomainConstants.Tasks.SCOPE_WEB_API_QUERY;
import static org.sitmun.domain.DomainConstants.Tasks.SCOPE_WEB_API_QUERY_NO_PROXY;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.sitmun.authorization.client.dto.TaskDto;
import org.sitmun.authorization.client.support.ProxyUrlBuilder;
import org.sitmun.domain.DomainConstants;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.task.MoreInfoTaskResolver;
import org.sitmun.domain.task.Task;
import org.sitmun.domain.task.parameter.TaskParameter;
import org.sitmun.domain.task.parameter.TaskParameterProcessor;
import org.sitmun.domain.territory.Territory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Service for handling moreInfo tasks in SITMUN. Maps moreInfo tasks to DTOs and manages their
 * parameters.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskMoreInfoService implements TaskMapper {

  @Value("${sitmun.proxy-middleware.url:}")
  private String proxyUrl;

  private final MoreInfoTaskResolver moreInfoTaskResolver;
  private final TaskParameterProcessor taskParameterProcessor;

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
    Map<String, Object> parametersDto;
    if (task.getProperties() == null) {
      // When properties is null, return empty map for backward compatibility
      parametersDto = new HashMap<>();
    } else {
      // When properties exist, parse and convert to viewer profile
      List<TaskParameter> parameters = taskParameterProcessor.parse(task);
      parametersDto = convertToViewerProfile(parameters);
      // If no valid parameters, convertToViewerProfile returns null
    }

    Task executionTask = moreInfoTaskResolver.findRelatedQueryTask(task).orElse(task);
    Map<String, Object> executionProperties = executionTask.getProperties();

    String uiControl = null;
    String type = null;
    if (task.getUi() != null) {
      uiControl = task.getUi().getName();
      type = task.getUi().getType();
    }

    String name = task.getName();
    String cartographyId =
        task.getCartography() != null ? String.valueOf(task.getCartography().getId()) : null;
    final String scope = normalizeExecutionScope(executionProperties);
    final String mimeType = extractStringProperty(executionProperties, PROPERTY_MIME_TYPE);
    final String filename = extractStringProperty(executionProperties, PROPERTY_FILENAME);
    final String url = resolveUrl(scope, task, executionProperties, application, territory);

    return TaskDto.builder()
        .id("task/" + task.getId())
        .name(name)
        .uiControl(uiControl)
        .type(type)
        .parameters(parametersDto)
        .cartographyId(cartographyId)
        .scope(scope)
        .mimeType(mimeType)
        .filename(filename)
        .url(url)
        .command(null)
        .build();
  }

  /**
   * Converts parsed task parameters to Viewer-compatible profile format. Only includes
   * client-allowed parameters (excludes backend-only LOCKED and PROVIDED parameters).
   *
   * <p>Output format: {@code {name -> {label, value (from field), name, type?, required?}}}
   *
   * @param parameters The parsed task parameters
   * @return Map of parameter names to their configuration, or null if no parameters
   */
  @Nullable
  private Map<String, Object> convertToViewerProfile(List<TaskParameter> parameters) {
    Map<String, Object> result = new HashMap<>();

    for (TaskParameter param : parameters) {
      if (taskParameterProcessor.classify(param).isBackendOnly()) {
        continue;
      }
      result.put(param.name(), taskParameterProcessor.toViewerParameterDto(param));
    }

    return result.isEmpty() ? null : result;
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
    if (SCOPE_RESOURCE.equalsIgnoreCase(scope) || SCOPE_URL.equalsIgnoreCase(scope)) {
      return extractStringProperty(executionProperties, DomainConstants.Tasks.PROPERTY_COMMAND);
    }
    return ProxyUrlBuilder.forScopedResource(
        proxyUrl, application, territory, scope, String.valueOf(task.getId()));
  }

  private String normalizeExecutionScope(Map<String, Object> properties) {
    if (properties == null) {
      return null;
    }
    Object scopeObj = properties.get(PROPERTY_SCOPE);
    if (scopeObj == null) {
      return null;
    }
    String scope = scopeObj.toString();
    if (SCOPE_SQL_QUERY.equalsIgnoreCase(scope)) {
      return SCOPE_SQL;
    }
    if (SCOPE_WEB_API_QUERY.equalsIgnoreCase(scope)) {
      return SCOPE_API;
    }
    if (SCOPE_WEB_API_QUERY_NO_PROXY.equalsIgnoreCase(scope)) {
      // No-proxy with mimeType → RESOURCE (mimeType-driven rendering, direct fetch)
      // No-proxy without mimeType → URL (external redirect)
      Object mimeTypeObj = properties.get(PROPERTY_MIME_TYPE);
      boolean hasMimeType = mimeTypeObj != null && StringUtils.hasText(mimeTypeObj.toString());
      return hasMimeType ? SCOPE_RESOURCE : SCOPE_URL;
    }
    if (SCOPE_URL_QUERY.equalsIgnoreCase(scope)) {
      return SCOPE_URL;
    }
    return scope;
  }
}
