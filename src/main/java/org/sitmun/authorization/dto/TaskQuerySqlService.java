package org.sitmun.authorization.dto;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.sitmun.authorization.service.Profile;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.task.Task;
import org.sitmun.domain.territory.Territory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class TaskQuerySqlService implements TaskMapper {

  @Value("${sitmun.proxy.url:}")
  private String proxyUrl;

  public boolean accept(Task task) {
    return task != null &&
      task.getType() != null &&
      "Query".equals(task.getType().getTitle()) &&
      task.getProperties() != null &&
      "sql-query".equals(task.getProperties().get("scope"));
  }

  public TaskDto map(Task task, Application application, Territory territory) {
    Map<String, Object> parameters = new HashMap<>();
    Map<String, Object> properties = task.getProperties();
    if (properties != null) {
      parameters = convertToJsonObject(properties);
    }

    // the endpoint should be
    String url = proxyUrl + "/proxy/" + application.getId() + "/" + territory.getId() + "/SQL/" + task.getId();

    // http://localhost:9000/middleware/proxy/19/4/SQL/32286
    return TaskDto.builder()
      .id("task/" + task.getId())
      .type("simple")
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
