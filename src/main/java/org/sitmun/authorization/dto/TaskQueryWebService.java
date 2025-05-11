package org.sitmun.authorization.dto;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
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
      if (properties.get("command") != null) {
        url = properties.get("command").toString();
      }
    }
    return TaskDto.builder()
      .id("task/" + task.getId())
      .type("simple")
      .parameters(parameters)
      .url(url)
      .build();
  }

  @Nullable
  private Map<String, Object> convertToJsonObject(Map<String, Object> properties) {
    Map<String, Object> parameters = new HashMap<>();
    
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> listOfParameters = (List<Map<String, Object>>) properties.getOrDefault("parameters", Collections.emptyList());
    
    for (Map<String, Object> param : listOfParameters) {
      if (param.containsKey("name") && param.containsKey("type") && param.containsKey("mandatory")) {
        String name = String.valueOf(param.get("name"));
        String type = String.valueOf(param.get("type"));
        Boolean mandatory = Boolean.valueOf(String.valueOf(param.get("mandatory")));
        
        Map<String, Object> values = new HashMap<>();
        values.put("type", type);
        values.put("mandatory", mandatory);
        parameters.put(name, values);
      }
    }
    
    return parameters.isEmpty() ? null : parameters;
  }
}
