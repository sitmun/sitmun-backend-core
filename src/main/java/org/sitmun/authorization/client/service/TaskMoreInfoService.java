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
    return isMoreInfoTask(task) || isMoreInfoAdvancedTask(task);
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

    if (isMoreInfoAdvancedTask(task) && task.getProperties() != null) {
      if (parametersDto == null) {
        parametersDto = new HashMap<>();
      }
      addMoreInfoAdvancedClientProperties(parametersDto, task.getProperties());
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
    String cartographyNumericId =
        task.getCartography() != null ? String.valueOf(task.getCartography().getId()) : null;
    String cartographyProfileId =
        task.getCartography() != null
            ? PROFILE_LAYER_ID_PREFIX + task.getCartography().getId()
            : null;
    final String scope = TaskScopeNormalizer.normalizeExecutionScope(executionProperties);
    final String mimeType = extractStringProperty(executionProperties, PROPERTY_MIME_TYPE);
    final String filename = extractStringProperty(executionProperties, PROPERTY_FILENAME);
    final String url = resolveUrl(scope, task, executionProperties, application, territory);

    return TaskDto.builder()
        .id(TASK_PROFILE_ID_PREFIX + task.getId())
        .name(name)
        .uiControl(uiControl)
        .type(type)
        .typeId(task.getType() != null ? task.getType().getId() : null)
        .parameters(parametersDto)
        .cartographyId(cartographyNumericId)
        .layer(cartographyProfileId)
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
      result.put(param.name(), taskParameterProcessor.toFeatureInfoParameter(param));
    }

    return result.isEmpty() ? null : result;
  }

  private void addMoreInfoAdvancedClientProperties(
      Map<String, Object> parameters, Map<String, Object> properties) {
    copyIfPresent(parameters, properties, "advancedTaskKind");
    copyIfPresent(parameters, properties, "moreInfoAdvanced");
    copyIfPresent(parameters, properties, "childTaskOrderIds");
    Object parentLayout = properties.get("parentLayout");
    if (parentLayout != null) {
      parameters.put("visualizationMode", parentLayout);
    }
  }

  private void copyIfPresent(
      Map<String, Object> target, Map<String, Object> source, String propertyName) {
    if (source.containsKey(propertyName)) {
      target.put(propertyName, source.get(propertyName));
    }
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
      return extractStringProperty(executionProperties, PROPERTY_COMMAND);
    }
    return ProxyUrlBuilder.forScopedResource(
        proxyUrl, application, territory, scope, String.valueOf(task.getId()));
  }
}
