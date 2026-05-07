package org.sitmun.authorization.client.service;

import static org.sitmun.authorization.client.AuthorizationConstants.TaskDto.*;
import static org.sitmun.authorization.client.AuthorizationConstants.TaskDto.PARAMETER_REQUIRED;
import static org.sitmun.authorization.client.AuthorizationConstants.TaskDto.PARAMETER_TYPE;
import static org.sitmun.authorization.client.AuthorizationConstants.TaskDto.PARAMETER_VALUE;
import static org.sitmun.domain.DomainConstants.Tasks.PARAM_TYPE_QUERY;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sitmun.authorization.client.AuthorizationConstants;
import org.sitmun.authorization.client.dto.TaskDto;
import org.sitmun.authorization.client.support.ProxyUrlBuilder;
import org.sitmun.domain.DomainConstants;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.cartography.Cartography;
import org.sitmun.domain.service.Service;
import org.sitmun.domain.task.Task;
import org.sitmun.domain.task.parameter.TaskParameter;
import org.sitmun.domain.task.parameter.TaskParameterProcessor;
import org.sitmun.domain.territory.Territory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Maps cartography query tasks to DTOs for authorization purposes. Handles the transformation of
 * cartography query tasks into a standardized format that includes service parameters, layer
 * information, and proxy URLs.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskQueryCartographyService implements TaskMapper {

  private final TaskParameterProcessor taskParameterProcessor;

  @Value("${sitmun.proxy-middleware.url:}")
  private String proxyUrl;

  /**
   * Determines if this mapper can handle the given task.
   *
   * @param task The task to check
   * @return true if the task is a cartography query task
   */
  public boolean accept(Task task) {
    return DomainConstants.Tasks.isCartographyQueryTask(task);
  }

  /**
   * Maps a cartography query task to a TaskDto. Constructs the proxy URL and includes service
   * parameters and layer information.
   *
   * @param task The task to map
   * @param application The associated application
   * @param territory The associated territory
   * @return A TaskDto containing the mapped task information
   */
  public TaskDto map(Task task, Application application, Territory territory) {
    List<TaskParameter> parameters = taskParameterProcessor.parse(task);
    Map<String, Object> parametersDto = convertToClientProfile(parameters);

    Cartography cartography = task.getCartography();
    Service service = cartography.getService();

    String url = ProxyUrlBuilder.forCartographyService(proxyUrl, application, territory, service);
    parametersDto.put(PARAMETER_SERVICE, getParametersObject(service.getType()));
    String layers = cartography.getLayers().stream().reduce((a, b) -> a + "," + b).orElse("");
    if (DomainConstants.Services.isWfsService(service)) {
      parametersDto.put(PARAMETER_WFS_TYPENAME, getParametersObject(layers));
    } else {
      parametersDto.put(PARAMETER_LAYERS, getParametersObject(layers));
    }
    return TaskDto.builder()
        .id("task/" + task.getId())
        .type(AuthorizationConstants.TaskDto.SIMPLE)
        .parameters(parametersDto)
        .url(url)
        .build();
  }

  /**
   * Converts parsed task parameters to client profile DTO format. Only includes client-allowed
   * parameters (excludes backend-only LOCKED and PROVIDED parameters).
   *
   * <p>Output format: {@code {name -> {type, required, value?}}}
   *
   * @param parameters The parsed task parameters
   * @return Map of parameter names to their configuration
   */
  private Map<String, Object> convertToClientProfile(List<TaskParameter> parameters) {
    Map<String, Object> result = new HashMap<>();

    for (TaskParameter param : parameters) {
      if (taskParameterProcessor.classify(param).isBackendOnly()) {
        continue;
      }
      result.put(param.name(), taskParameterProcessor.toParameterDtoWithValue(param, "string"));
    }

    return result;
  }

  /**
   * Creates a parameter configuration object with type, required flag, and optional value.
   *
   * @param value The parameter value (can be null)
   * @return A map containing the parameter configuration
   */
  private Map<String, Object> getParametersObject(String value) {
    Map<String, Object> values = new HashMap<>();
    values.put(PARAMETER_TYPE, PARAM_TYPE_QUERY);
    values.put(PARAMETER_REQUIRED, true);
    if (value != null) {
      values.put(PARAMETER_VALUE, value);
    }
    return values;
  }
}
