package org.sitmun.authorization.client.service;

import static org.sitmun.domain.DomainConstants.Tasks.*;
import static org.sitmun.domain.DomainConstants.Tasks.PROPERTY_FILENAME;
import static org.sitmun.domain.DomainConstants.Tasks.PROPERTY_SCOPE;

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
import org.sitmun.infrastructure.util.ParameterValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Maps web service query tasks to DTOs. Handles conversion of task properties and parameters for
 * web API queries.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskQueryWebService implements TaskMapper {

  private final TaskParameterProcessor taskParameterProcessor;

  @Value("${sitmun.proxy-middleware.url:}")
  private String proxyUrl;

  /**
   * Checks if the task is a web API query task.
   *
   * @param task Task to check
   * @return true if task is a web API query
   */
  public boolean accept(Task task) {
    return DomainConstants.Tasks.isWebApiQuery(task);
  }

  /**
   * Maps a web service query task to a TaskDto.
   *
   * @param task Task to map
   * @param application Application context
   * @param territory Territory context
   * @return Mapped TaskDto with parameters and URL
   */
  public TaskDto map(Task task, Application application, Territory territory) {
    Map<String, Object> properties = task.getProperties();

    String url = null;
    String mimeType = null;
    String filename = null;
    Map<String, Object> parametersDto = new HashMap<>();

    if (properties != null) {
      boolean hasProvidedVars = ParameterValidator.hasProvidedVariables(properties);
      Object scopeObj = properties.get(PROPERTY_SCOPE);
      String scopeStr = String.valueOf(scopeObj);
      boolean isNoProxy = SCOPE_WEB_API_QUERY_NO_PROXY.equalsIgnoreCase(scopeStr);

      if (!isNoProxy && hasProvidedVars) {
        url = ProxyUrlBuilder.forWebApiTask(proxyUrl, application, territory, task);
      } else if (properties.get(PROPERTY_COMMAND) != null) {
        url = properties.get(PROPERTY_COMMAND).toString();
      }

      Object mimeTypeObj = properties.get(PROPERTY_MIME_TYPE);
      if (mimeTypeObj != null) {
        mimeType = mimeTypeObj.toString();
      }
      Object filenameObj = properties.get(PROPERTY_FILENAME);
      if (filenameObj != null) {
        filename = filenameObj.toString();
      }

      List<TaskParameter> parameters = taskParameterProcessor.parse(task);
      parametersDto = convertToClientProfile(parameters);
    }

    return TaskDto.builder()
        .id("task/" + task.getId())
        .type(AuthorizationConstants.TaskDto.SIMPLE)
        .parameters(parametersDto)
        .url(url)
        .mimeType(mimeType)
        .filename(filename)
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
