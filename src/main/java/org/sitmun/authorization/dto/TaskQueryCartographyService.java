package org.sitmun.authorization.dto;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.cartography.Cartography;
import org.sitmun.domain.service.Service;
import org.sitmun.domain.task.Task;
import org.sitmun.domain.territory.Territory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class TaskQueryCartographyService implements TaskMapper {

  @Value("${sitmun.proxy.url:}")
  private String proxyUrl;

  public boolean accept(Task task) {
    return task != null &&
      task.getType() != null &&
      "Query".equals(task.getType().getTitle()) &&
      task.getProperties() != null &&
      "cartography-query".equals(task.getProperties().get("scope"));
  }

  public TaskDto map(Task task, Application application, Territory territory) {
    Map<String, Object> parameters = new HashMap<>();
    Map<String, Object> properties = task.getProperties();
    if (properties != null) {
      parameters = convertToJsonObject(properties);
    }

    Cartography cartography = task.getCartography();
    Service service = cartography.getService();

    String url = proxyUrl + "/proxy/" + application.getId() + "/" + territory.getId() + "/" + service.getType() + "/" + service.getId();
    parameters.put("service", getParametersObject("query", true, service.getType()));
    String layers = cartography.getLayers().stream().reduce((a, b) -> a + "," + b).orElse("");
    if ("WFS".equals(service.getType())) {
      parameters.put("typename", getParametersObject("query", true, layers));
    } else {
      parameters.put("layers", getParametersObject("query", true, layers));
    }
    return TaskDto.builder()
      .id("task/" + task.getId())
      .type("simple")
      .parameters(parameters)
      .url(url)
      .build();
  }

  private Map<String, Object> convertToJsonObject(Map<String, Object> properties) {
    Map<String, Object> parameters = new HashMap<>();
    
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> listOfParameters = (List<Map<String, Object>>) properties.getOrDefault("parameters", Collections.emptyList());
    
    for (Map<String, Object> param : listOfParameters) {
      if (param.containsKey("name") && param.containsKey("type") && param.containsKey("required")) {
        String name = String.valueOf(param.get("name"));
        String type = String.valueOf(param.get("type"));
        Boolean required = Boolean.valueOf(String.valueOf(param.get("required")));
        
        Map<String, Object> values = getParametersObject(type, required, null);
        parameters.put(name, values);
      }
    }
    
    return parameters;
  }

  private Map<String, Object> getParametersObject(String type, Boolean required, String value) {
    Map<String, Object> values = new HashMap<>();
    values.put("type", type);
    values.put("required", required);
    if (value != null) {
      values.put("value", value);
    }
    return values;
  }
}
