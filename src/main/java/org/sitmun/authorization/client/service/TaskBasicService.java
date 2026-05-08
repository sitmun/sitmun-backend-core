package org.sitmun.authorization.client.service;

import static org.sitmun.domain.DomainConstants.Tasks.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.sitmun.authorization.client.dto.TaskDto;
import org.sitmun.authorization.client.service.support.BasicParameterValueConverter;
import org.sitmun.authorization.client.service.support.BasicParameterValueType;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.task.Task;
import org.sitmun.domain.task.parameter.TaskParameter;
import org.sitmun.domain.task.parameter.TaskParameterProcessor;
import org.sitmun.domain.territory.Territory;
import org.sitmun.infrastructure.util.ParameterValidator;
import org.springframework.stereotype.Component;

/**
 * Service for mapping basic tasks to DTOs. Handles conversion of task properties to appropriate
 * parameter types.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskBasicService implements TaskMapper {

  private final TaskParameterProcessor taskParameterProcessor;
  private final BasicParameterValueConverter parameterValueConverter;

  /**
   * Checks if the task is a basic task type.
   *
   * @param task Task to check
   * @return true if task is a basic task type
   */
  public boolean accept(Task task) {
    return isBasicTask(task);
  }

  /**
   * Maps a Task to its DTO representation.
   *
   * @param task Task to map
   * @param application Associated application
   * @param territory Associated territory
   * @return TaskDto with mapped properties
   */
  public TaskDto map(Task task, Application application, Territory territory) {
    String control = null;
    if (task.getUi() != null) {
      control = task.getUi().getName();
    }

    Map<String, Object> parameters = new HashMap<>();
    Map<String, Object> properties = task.getProperties();
    if (properties != null) {
      List<TaskParameter> taskParameters = taskParameterProcessor.parse(task);
      ParameterValidator.validateNoProvidedVariables(taskParameters, "Basic");
      parameters = convertToJsonObject(taskParameters);
    }
    return TaskDto.builder()
        .id(TASK_PROFILE_ID_PREFIX + task.getId())
        .uiControl(control)
        .parameters(parameters)
        .build();
  }

  /**
   * Converts parsed task parameters to JSON object structure with type-based conversion.
   *
   * @param taskParameters Parsed task parameters
   * @return Map of converted parameters or null if empty
   */
  @Nullable
  private Map<String, Object> convertToJsonObject(List<TaskParameter> taskParameters) {
    Map<String, Object> parameters = new HashMap<>();

    for (TaskParameter param : taskParameters) {
      if (param.type() != null && param.rawValue() != null) {
        typeBasedConversion(param.type(), param.rawValue(), parameters, param.name());
      }
    }
    return parameters.isEmpty() ? null : parameters;
  }

  /**
   * Converts parameter value based on its type using {@link BasicParameterValueConverter}.
   *
   * @param typeString Parameter type string
   * @param value Parameter value
   * @param parameters Target parameters map
   * @param name Parameter name
   */
  private void typeBasedConversion(
      String typeString, String value, Map<String, Object> parameters, String name) {
    BasicParameterValueType type = BasicParameterValueType.from(typeString);
    if (type == null) {
      log.warn("Unknown type {} for parameter {}", typeString, name);
      return;
    }
    Object converted = parameterValueConverter.convert(type, value);
    if (converted != null || type == BasicParameterValueType.NULL) {
      parameters.put(name, converted);
    }
  }
}
