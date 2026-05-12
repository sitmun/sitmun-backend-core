package org.sitmun.authorization.client.service;

import static org.sitmun.authorization.client.AuthorizationConstants.TaskDto.SIMPLE;
import static org.sitmun.domain.DomainConstants.Tasks.*;
import static org.sitmun.domain.task.parameter.TaskParameterProcessor.ProfileParameterShape.SIMPLE_STRING_DEFAULT;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sitmun.authorization.client.dto.TaskDto;
import org.sitmun.authorization.client.support.ProxyUrlBuilder;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.task.Task;
import org.sitmun.domain.task.parameter.TaskParameter;
import org.sitmun.domain.task.parameter.TaskParameterProcessor;
import org.sitmun.domain.territory.Territory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Maps web service query tasks to DTOs. Handles conversion of task properties and parameters for
 * web API queries.
 *
 * <p>For {@code web-api-query} (proxied), URI {@code template} parameters are omitted from the
 * client profile: the viewer uses the middleware URL only. Implemented via {@link
 * TaskParameterProcessor#toProfileParameterMap} ({@link
 * org.sitmun.domain.task.parameter.TaskParameterProcessor.ProfileParameterShape#SIMPLE_STRING_DEFAULT}
 * with URI-template omission when proxied).
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
   * @return true if the task is a web API query
   */
  public boolean accept(Task task) {
    return isWebApiQuery(task);
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
    String scope = null;
    String mimeType = null;
    String filename = null;
    Map<String, Object> parametersDto = new HashMap<>();

    if (properties != null) {
      Object scopeObj = properties.get(PROPERTY_SCOPE);
      String scopeStr = String.valueOf(scopeObj);
      boolean isNoProxy = SCOPE_WEB_API_QUERY_NO_PROXY.equalsIgnoreCase(scopeStr);

      // Scope-only proxy gating: web-api-query always proxied, web-api-query-no-proxy always direct
      if (isNoProxy) {
        // Direct execution for no-proxy scope
        if (properties.get(PROPERTY_COMMAND) != null) {
          url = properties.get(PROPERTY_COMMAND).toString();
        }
        // No-proxy: RESOURCE if mimeType present, URL otherwise
        Object mimeTypeObj = properties.get(PROPERTY_MIME_TYPE);
        scope =
            (mimeTypeObj != null && !mimeTypeObj.toString().trim().isEmpty())
                ? SCOPE_RESOURCE
                : SCOPE_URL;
      } else {
        // Proxied execution for web-api-query scope
        url = ProxyUrlBuilder.forWebApiTask(proxyUrl, application, territory, task);
        scope = SCOPE_API;
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
      parametersDto =
          taskParameterProcessor.toProfileParameterMap(
              parameters,
              SIMPLE_STRING_DEFAULT,
              /* omitUriTemplatePlaceholders for proxied web-api-query */ !isNoProxy);
    }

    return TaskDto.builder()
        .id(TASK_PROFILE_ID_PREFIX + task.getId())
        .type(SIMPLE)
        .typeId(task.getType() != null ? task.getType().getId() : null)
        .scope(scope)
        .parameters(parametersDto)
        .url(url)
        .mimeType(mimeType)
        .filename(filename)
        .build();
  }
}
