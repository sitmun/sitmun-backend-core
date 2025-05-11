package org.sitmun.authorization.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.task.Task;
import org.sitmun.domain.territory.Territory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class TaskBasicService implements TaskMapper {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final TypeReference<List<Object>> ARRAY_TYPE_REFERENCE = new TypeReference<>() {};
  private static final TypeReference<Map<String, Object>> OBJECT_TYPE_REFERENCE = new TypeReference<>() {};

  public boolean accept(Task task) {
    return task != null && task.getType() != null && "Basic".equals(task.getType().getTitle());
  }

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

  @Nullable
  private Map<String, Object> convertToJsonObject(Map<String, Object> properties) {
    Map<String, Object> parameters = new HashMap<>();
    
    @SuppressWarnings("unchecked")
    List<Map<String, String>> listOfParameters = (List<Map<String, String>>) properties.getOrDefault("parameters", Collections.emptyList());
    
    for (Map<String, String> param : listOfParameters) {
      if (param.containsKey("name") && param.containsKey("type") && param.containsKey("value")) {
        String name = param.get("name");
        String type = param.get("type");
        String value = param.get("value");
        typeBasedConversion(type, value, parameters, name);
      }
    }
    return parameters.isEmpty() ? null : parameters;
  }

  private void typeBasedConversion(String type, String value, Map<String, Object> parameters, String name) {
    try {
      switch (type) {
        case "string":
          parameters.put(name, value != null ? value : "");
          break;
        case "number":
          parameters.put(name, Double.parseDouble(value));
          break;
        case "array":
          parameters.put(name, OBJECT_MAPPER.readValue(value, ARRAY_TYPE_REFERENCE));
          break;
        case "object":
          parameters.put(name, OBJECT_MAPPER.readValue(value, OBJECT_TYPE_REFERENCE));
          break;
        case "boolean":
          parameters.put(name, Boolean.parseBoolean(value));
          break;
        case "null":
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
