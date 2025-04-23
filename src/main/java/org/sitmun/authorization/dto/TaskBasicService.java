package org.sitmun.authorization.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.sitmun.authorization.service.Profile;
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
    Map<String, Object> parameters;
    parameters = new HashMap<>();
    //noinspection unchecked
    List<Map<String, String>> listOfParameters = (List<Map<String, String>>) properties.getOrDefault("parameters", Collections.emptyList());
    for (Map<String, String> param : listOfParameters) {
      if (param.containsKey("name") && param.containsKey("type") && param.containsKey("value")) {
        String name = param.get("name");
        String type = param.get("type");
        String value = param.get("value");
        typeBasedConversion(type, value, parameters, name);
      }
    }
    if (parameters.isEmpty()) {
      return null;
    }
    return parameters;
  }

  private void typeBasedConversion(String type, String value, Map<String, Object> parameters, String name) {
    switch (type) {
      case "string":
        if (value == null) {
          value = "";
        }
        parameters.put(name, value);
        break;
      case "number":
        parameters.put(name, Double.parseDouble(value));
        break;
      case "array":
        try {
          parameters.put(name, new ObjectMapper().readValue(value, List.class));
        } catch (JsonProcessingException e) {
          log.error("Error processing array", e);
        }
        break;
      case "object":
        try {
          parameters.put(name, new ObjectMapper().readValue(value, Map.class));
        } catch (JsonProcessingException e) {
          log.error("Error processing object", e);
        }
        break;
      case "boolean":
        parameters.put(name, Boolean.parseBoolean(value));
        break;
      case "null":
        parameters.put(name, null);
        break;
      default:
        break;
    }
  }
}
