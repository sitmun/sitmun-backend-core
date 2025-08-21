package org.sitmun.authorization.client.dto;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.sitmun.administration.service.database.DatabaseConnectionService;
import org.sitmun.authorization.client.AuthorizationConstants;
import org.sitmun.domain.DomainConstants;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.cartography.Cartography;
import org.sitmun.domain.database.DatabaseConnection;
import org.sitmun.domain.service.Service;
import org.sitmun.domain.task.Task;
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
public class TaskEditCartographyService implements TaskMapper {

  @Value("${sitmun.proxy.url:}")
  private String proxyUrl;

  private final DatabaseConnectionService dbConService;

  public TaskEditCartographyService(DatabaseConnectionService dbConService) {
    this.dbConService = dbConService;
  }

  /**
   * Determines if this mapper can handle the given task.
   *
   * @param task The task to check
   * @return true if the task is a cartography edit task
   */
  public boolean accept(Task task) {
    return DomainConstants.Tasks.isCartographyEditionTask(task);
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
    Map<String, Object> parameters = new HashMap<>();
    Map<String, Object> fields = new HashMap<>();
    Map<String, Object> properties = task.getProperties();
    if (properties != null) {
      parameters = convertParametersToJsonObject(properties);
      fields = convertFieldsToJsonObject(properties, task.getConnection());
    }

    boolean postRequest =
        false; // parameters.entrySet().stream().anyMatch(e -> "body".equals(((Map<String,
    // Object>)e.getValue()).get("type")));
    String paramType = postRequest ? "body" : AuthorizationConstants.TaskDto.QUERY;

    Cartography cartography = task.getCartography();
    Service service = cartography.getService();

    String url =
        proxyUrl
            + "/proxy/"
            + application.getId()
            + "/"
            + territory.getId()
            + "/"
            + service.getType()
            + "/"
            + service.getId();
    parameters.put(
        AuthorizationConstants.TaskDto.PARAMETER_SERVICE,
        getParametersObject(paramType, true, service.getType()));
    String layers = cartography.getLayers().stream().reduce((a, b) -> a + "," + b).orElse("");
    if (DomainConstants.Services.isWfsService(service)) {
      parameters.put(
          AuthorizationConstants.TaskDto.PARAMETER_WFS_TYPENAME,
          getParametersObject(paramType, true, layers));
    } else {
      parameters.put(
          AuthorizationConstants.TaskDto.PARAMETER_LAYERS,
          getParametersObject(paramType, true, layers));
    }
    return TaskDto.builder()
        .id("task/" + task.getId())
        .type(AuthorizationConstants.TaskDto.EDITION)
        .parameters(parameters)
        .fields(fields)
        .url(url)
        .build();
  }

  /**
   * Converts task properties to a JSON-compatible parameter map.
   *
   * @param properties The task properties to convert
   * @return A map of parameter names to their configuration
   */
  private Map<String, Object> convertParametersToJsonObject(Map<String, Object> properties) {
    Map<String, Object> parameters = new HashMap<>();

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> listOfParameters =
        (List<Map<String, Object>>) properties.getOrDefault("parameters", Collections.emptyList());

    for (Map<String, Object> param : listOfParameters) {
      if (param.containsKey(DomainConstants.Tasks.PARAMETERS_NAME)
          && param.containsKey(DomainConstants.Tasks.PARAMETERS_TYPE)
          && param.containsKey(DomainConstants.Tasks.PARAMETERS_REQUIRED)) {
        String name = String.valueOf(param.get(DomainConstants.Tasks.PARAMETERS_NAME));
        String type = String.valueOf(param.get(DomainConstants.Tasks.PARAMETERS_TYPE));
        String value =
            param.containsKey(DomainConstants.Tasks.PARAMETERS_VALUE)
                ? String.valueOf(param.get(DomainConstants.Tasks.PARAMETERS_VALUE))
                : null;
        Boolean required =
            Boolean.valueOf(String.valueOf(param.get(DomainConstants.Tasks.PARAMETERS_REQUIRED)));

        Map<String, Object> values = getParametersObject(type, required, value);
        parameters.put(name, values);
      }
    }

    return parameters;
  }

  /**
   * Converts task properties to a JSON-compatible fields map.
   *
   * @param properties The task properties to convert
   * @return A map of fields names to their configuration
   */
  private Map<String, Object> convertFieldsToJsonObject(
      Map<String, Object> properties, DatabaseConnection connection) {
    Map<String, Object> fields = new HashMap<>();

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> listOfFields =
        (List<Map<String, Object>>) properties.getOrDefault("fields", Collections.emptyList());

    for (Map<String, Object> field : listOfFields) {
      if (field.containsKey("name")
          && field.containsKey("type")
          && field.containsKey("required")
          && field.containsKey("selectable")
          && field.containsKey("editable")
          && field.containsKey("label")) {
        String name = String.valueOf(field.get("name"));
        String type = String.valueOf(field.get("type"));
        String label = String.valueOf(field.get("label"));
        Boolean required = Boolean.valueOf(String.valueOf(field.get("required")));
        Boolean selectable = Boolean.valueOf(String.valueOf(field.get("selectable")));
        Boolean editable = Boolean.valueOf(String.valueOf(field.get("editable")));
        String value = field.containsKey("value") ? String.valueOf(field.get("value")) : null;
        List<Map<String, Object>> listValues = null;
        if (field.containsKey("query") && connection != null) {
          listValues = dbConService.executeQuery(connection, String.valueOf(field.get("query")));
        }

        Map<String, Object> values =
            getFieldObject(label, type, required, selectable, editable, value, listValues);
        fields.put(name, values);
      }
    }

    return !fields.isEmpty() ? fields : null;
  }

  /**
   * Creates a parameter configuration object with type, required flag, and optional value.
   *
   * @param type The parameter type
   * @param required Whether the parameter is required
   * @param value The parameter value (can be null)
   * @return A map containing the parameter configuration
   */
  private Map<String, Object> getParametersObject(String type, Boolean required, String value) {
    Map<String, Object> values = new HashMap<>();
    values.put(AuthorizationConstants.TaskDto.PARAMETER_TYPE, type);
    values.put(AuthorizationConstants.TaskDto.PARAMETER_REQUIRED, required);
    if (value != null) {
      values.put(AuthorizationConstants.TaskDto.PARAMETER_VALUE, value);
    }
    return values;
  }

  /**
   * Creates a field configuration object with type, required flag, and optional value.
   *
   * @param label The field label
   * @param type The field data type
   * @param required Whether the field is required
   * @param selectable Whether the field is selectable
   * @param editable Whether the field is editable
   * @param value The parameter value (can be null)
   * @param listValues The listBox data type values (can be null)
   * @return A map containing the field configuration
   */
  private Map<String, Object> getFieldObject(
      String label,
      String type,
      Boolean required,
      Boolean selectable,
      Boolean editable,
      String value,
      List<Map<String, Object>> listValues) {
    Map<String, Object> values = new HashMap<>();
    values.put("type", type);
    values.put("label", label);
    values.put("required", required);
    values.put("selectable", selectable);
    values.put("editable", editable);
    if (value != null && !value.isEmpty()) {
      values.put("value", value);
    }
    if (listValues != null && !listValues.isEmpty()) {
      values.put("listValues", listValues);
    }
    return values;
  }
}
