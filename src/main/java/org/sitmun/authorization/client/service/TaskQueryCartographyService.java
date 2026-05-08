package org.sitmun.authorization.client.service;

import static org.sitmun.authorization.client.AuthorizationConstants.TaskDto.*;
import static org.sitmun.domain.DomainConstants.Tasks.PARAM_TYPE_QUERY;
import static org.sitmun.domain.DomainConstants.Tasks.PROFILE_LAYER_ID_PREFIX;
import static org.sitmun.domain.DomainConstants.Tasks.TASK_PROFILE_ID_PREFIX;
import static org.sitmun.domain.DomainConstants.Tasks.isCartographyQueryTask;
import static org.sitmun.domain.task.parameter.TaskParameterProcessor.ProfileParameterShape.CARTOGRAPHY_QUERY_WITH_VALUE_STRING_DEFAULT;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sitmun.authorization.client.dto.TaskDto;
import org.sitmun.authorization.client.support.CartographyTaskProfileSupport;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.cartography.Cartography;
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
    return isCartographyQueryTask(task);
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
    Map<String, Object> parametersDto =
        taskParameterProcessor.toProfileParameterMap(
            parameters, CARTOGRAPHY_QUERY_WITH_VALUE_STRING_DEFAULT, false);

    Cartography cartography = task.getCartography();

    String url =
        CartographyTaskProfileSupport.putCartographyProxyAndLayerSlots(
            parametersDto, proxyUrl, application, territory, cartography, PARAM_TYPE_QUERY);

    // Deprecated cartographyId: bare id string; canonical profile id is PROFILE_LAYER_ID_PREFIX +
    // id.
    String cartographyId = String.valueOf(cartography.getId());

    return TaskDto.builder()
        .id(TASK_PROFILE_ID_PREFIX + task.getId())
        .type(SIMPLE)
        .cartographyId(cartographyId)
        .layer(PROFILE_LAYER_ID_PREFIX + cartography.getId())
        .parameters(parametersDto)
        .url(url)
        .build();
  }
}
