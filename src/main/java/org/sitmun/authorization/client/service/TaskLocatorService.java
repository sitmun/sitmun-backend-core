package org.sitmun.authorization.client.service;

import static org.sitmun.domain.DomainConstants.Tasks.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.sitmun.authorization.client.dto.TaskDto;
import org.sitmun.authorization.client.support.ProxyUrlBuilder;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.task.MoreInfoTaskResolver;
import org.sitmun.domain.task.Task;
import org.sitmun.domain.task.TaskScopeNormalizer;
import org.sitmun.domain.task.parameter.TaskParameter;
import org.sitmun.domain.task.parameter.TaskParameterProcessor;
import org.sitmun.domain.territory.Territory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Service for handling locator tasks in SITMUN. Maps locator tasks to DTOs and manages their
 * parameters. A locator task delegates its execution to a linked query task via a
 * {@code query-task} relation.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskLocatorService implements TaskMapper {

  @Value("${sitmun.proxy-middleware.url:}")
  private String proxyUrl;

  private final MoreInfoTaskResolver moreInfoTaskResolver;
  private final TaskParameterProcessor taskParameterProcessor;

  /**
   * Determines if a task is a locator task.
   *
   * @param task The task to check
   * @return true if the task is a locator task
   */
  public boolean accept(Task task) {
    return isLocatorTask(task);
  }

  /**
   * Maps a locator task to a TaskDto.
   *
   * @param task The locator task
   * @param application The application context
   * @param territory The territory context
   * @return TaskDto containing task information and parameters
   */
  public TaskDto map(Task task, Application application, Territory territory) {
    Map<String, Object> parametersDto;
    if (task.getProperties() == null) {
      parametersDto = new HashMap<>();
    } else {
      List<TaskParameter> parameters = taskParameterProcessor.parse(task);
      parametersDto = convertToViewerProfile(parameters);
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
    final String scope = TaskScopeNormalizer.normalizeExecutionScope(executionProperties);
    // Use the locator task's own ID in the proxy URL so that access control is validated
    // against the locator task's roles/territories (not the underlying query task's).
    // The backend's MoreInfoTaskResolver will resolve the query task for execution.
    final String url = resolveUrl(scope, task, executionProperties, application, territory);

    return TaskDto.builder()
        .id(TASK_PROFILE_ID_PREFIX + task.getId())
        .name(name)
        .uiControl(uiControl)
        .type(type)
        .parameters(parametersDto)
        .scope(scope)
        .url(url)
        .build();
  }

  /**
   * Converts parsed task parameters to Viewer-compatible profile format. Only includes
   * client-allowed parameters (excludes backend-only LOCKED and PROVIDED parameters).
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
      // Locator config params (resultsPath, labelField, etc.) are plain string values consumed
      // directly by the viewer. Return rawValue so the viewer can read them without unwrapping
      // a FeatureInfoParameter object.
      result.put(param.name(), param.rawValue());
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
      Task locatorTask,
      Map<String, Object> executionProperties,
      Application application,
      Territory territory) {
    if (!StringUtils.hasText(scope) || locatorTask.getId() == null) {
      return null;
    }
    if (SCOPE_RESOURCE.equalsIgnoreCase(scope) || SCOPE_URL.equalsIgnoreCase(scope)) {
      return extractStringProperty(executionProperties, PROPERTY_COMMAND);
    }
    return ProxyUrlBuilder.forScopedResource(
        proxyUrl, application, territory, scope, String.valueOf(locatorTask.getId()));
  }
}
