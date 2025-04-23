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

import java.util.*;

@Slf4j
@Component
public class TaskQueryWebService implements TaskMapper {

  public boolean accept(Task task) {
    return task != null &&
      task.getType() != null &&
      "Query".equals(task.getType().getTitle()) &&
      task.getProperties() != null &&
      "web-api-query".equals(task.getProperties().get("scope"));
  }

  public TaskDto map(Task task, Application application, Territory territory) {
    String url = null;
    Map<String, Object> parameters = new HashMap<>();
    Map<String, Object> properties = task.getProperties();
    if (properties != null) {
      parameters = convertToJsonObject(properties);
      if (properties.get("command") != null) {;
        url = properties.get("command").toString();
      }
    }
    return TaskDto.builder()
      .id("task/" + task.getId())
      .parameters(parameters)
      .url(url)
      .build();
  }

  @Nullable
  private Map<String, Object> convertToJsonObject(Map<String, Object> properties) {
    Map<String, Object> parameters;
    parameters = new HashMap<>();
    //noinspection unchecked
    List<Map<String, String>> listOfParameters = (List<Map<String, String>>) properties.getOrDefault("parameters", Collections.emptyList());
    for (Map<String, String> param : listOfParameters) {
      if (param.containsKey("key")) {
        String key = param.get("key");
        HashMap<String, String> values = new HashMap<>(param);
        values.remove("key");
        parameters.put(key, values);
      }
    }
    if (parameters.isEmpty()) {
      return null;
    }
    return parameters;
  }
}
