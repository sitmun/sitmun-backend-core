package org.sitmun.authorization.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.sitmun.domain.DomainConstants;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.task.Task;
import org.sitmun.domain.territory.Territory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for mapping basic tasks to DTOs.
 * Handles conversion of task properties to appropriate parameter types.
 */
@Slf4j
@Component
public class TaskBasicService implements TaskMapper {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final TypeReference<List<Object>> ARRAY_TYPE_REFERENCE = new TypeReference<>() {
  };
  private static final TypeReference<Map<String, Object>> OBJECT_TYPE_REFERENCE = new TypeReference<>() {
  };

  /**
   * Checks if the task is a basic task type.
   * @param task Task to check
   * @return true if task is a basic task type
   */
  public boolean accept(Task task) {
    return DomainConstants.Tasks.isBasicTask(task);
  }

  /**
   * Maps a Task to its DTO representation.
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
      parameters = convertToJsonObject(properties);
    }
    return TaskDto.builder()
      .id("task/" + task.getId())
      .uiControl(control)
      .parameters(parameters)
      .build();
  }

  /**
   * Converts task properties to a JSON object structure.
   * @param properties Task properties to convert
   * @return Map of converted parameters or null if empty
   */
  @Nullable
  private Map<String, Object> convertToJsonObject(Map<String, Object> properties) {
    Map<String, Object> parameters = new HashMap<>();

    @SuppressWarnings("unchecked")
    List<Map<String, String>> listOfParameters = (List<Map<String, String>>) properties.getOrDefault(DomainConstants.Tasks.PROPERTY_PARAMETERS, Collections.emptyList());

    for (Map<String, String> param : listOfParameters) {
      if (param.containsKey(DomainConstants.Tasks.PARAMETERS_NAME) && param.containsKey(DomainConstants.Tasks.PARAMETERS_TYPE) && param.containsKey(DomainConstants.Tasks.PARAMETERS_VALUE)) {
        String name = param.get(DomainConstants.Tasks.PARAMETERS_NAME);
        String type = param.get(DomainConstants.Tasks.PARAMETERS_TYPE);
        String value = param.get(DomainConstants.Tasks.PARAMETERS_VALUE);
        typeBasedConversion(type, value, parameters, name);
      }
    }
    return parameters.isEmpty() ? null : parameters;
  }

  /**
   * Converts parameter value based on its type.
   * @param type Parameter type
   * @param value Parameter value
   * @param parameters Target parameters map
   * @param name Parameter name
   */
  private void typeBasedConversion(String type, String value, Map<String, Object> parameters, String name) {
    try {
      switch (type) {
        case DomainConstants.Tasks.TYPE_STRING:
          parameters.put(name, value != null ? value : "");
          break;
        case DomainConstants.Tasks.TYPE_NUMBER:
          parameters.put(name, Double.parseDouble(value));
          break;
        case DomainConstants.Tasks.TYPE_ARRAY:
          parameters.put(name, OBJECT_MAPPER.readValue(value, ARRAY_TYPE_REFERENCE));
          break;
        case DomainConstants.Tasks.TYPE_OBJECT:
          parameters.put(name, OBJECT_MAPPER.readValue(value, OBJECT_TYPE_REFERENCE));
          break;
        case DomainConstants.Tasks.TYPE_BOOLEAN:
          parameters.put(name, Boolean.parseBoolean(value));
          break;
        case DomainConstants.Tasks.TYPE_NULL:
          parameters.put(name, null);
          break;
        default:
          log.warn("Unknown type {} for parameter {}", type, name);
          break;
      }
    } catch (JsonProcessingException e) {
      log.error("Error processing {} type for parameter {}", type, name, e);
    }
  }
}
