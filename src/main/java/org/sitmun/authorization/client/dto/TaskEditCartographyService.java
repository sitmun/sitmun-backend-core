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
import org.sitmun.infrastructure.util.ParameterValidator;
import org.sitmun.infrastructure.util.TaskParameterUtil;
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
    Map<String, Object> properties = task.getProperties();
    ParameterValidator.validateProvidedFlag(properties);

    Map<String, Object> parameters = new HashMap<>();
    Map<String, Object> fields = new HashMap<>();
    if (properties != null) {
      parameters = convertParametersToJsonObject(properties);
      fields = convertFieldsToJsonObject(properties, task.getConnection());
    }

    boolean postRequest =
        false; // parameters.entrySet().stream().anyMatch(e -> "body".equals(((Map<String,
    // Object>)e.getValue()).get("type")));
    String paramType =
        postRequest
            ? DomainConstants.Tasks.PARAM_TYPE_BODY
            : DomainConstants.Tasks.PARAM_TYPE_QUERY;

    Cartography cartography = task.getCartography();
    Service service = cartography.getService();

    String url = ProxyUrlBuilder.forCartographyService(proxyUrl, application, territory, service);
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
        (List<Map<String, Object>>)
            properties.getOrDefault(
                DomainConstants.Tasks.PROPERTY_PARAMETERS, Collections.emptyList());

    for (Map<String, Object> param : listOfParameters) {
      Object provided = param.get(DomainConstants.Tasks.PARAMETERS_PROVIDED);
      boolean isProvided =
          Boolean.TRUE.equals(provided) || "true".equalsIgnoreCase(String.valueOf(provided));

      if (isProvided) {
        continue;
      }

      // Use backward-compatible helper to read 'variable' or 'name'
      String name = TaskParameterUtil.getParameterVariable(param);

      if (name != null) {
        String type =
            param.containsKey(DomainConstants.Tasks.PARAMETERS_TYPE)
                ? String.valueOf(param.get(DomainConstants.Tasks.PARAMETERS_TYPE))
                : DomainConstants.Tasks.PARAM_TYPE_QUERY;
        String value =
            param.containsKey(DomainConstants.Tasks.PARAMETERS_VALUE)
                ? String.valueOf(param.get(DomainConstants.Tasks.PARAMETERS_VALUE))
                : null;
        Boolean required =
            param.containsKey(DomainConstants.Tasks.PARAMETERS_REQUIRED)
                ? Boolean.valueOf(
                    String.valueOf(param.get(DomainConstants.Tasks.PARAMETERS_REQUIRED)))
                : false;

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
        (List<Map<String, Object>>)
            properties.getOrDefault(DomainConstants.Tasks.PROPERTY_FIELDS, Collections.emptyList());

    for (Map<String, Object> field : listOfFields) {
      // CRITICAL: 'name' is the minimum required key for fields (edition-mobile contract)
      if (field.containsKey(DomainConstants.Tasks.FIELDS_NAME)) {
        String name = String.valueOf(field.get(DomainConstants.Tasks.FIELDS_NAME));
        String type =
            field.containsKey(DomainConstants.Tasks.FIELDS_TYPE)
                ? String.valueOf(field.get(DomainConstants.Tasks.FIELDS_TYPE))
                : DomainConstants.Tasks.FIELD_TYPE_TEXT;
        String label =
            field.containsKey(DomainConstants.Tasks.FIELDS_LABEL)
                ? String.valueOf(field.get(DomainConstants.Tasks.FIELDS_LABEL))
                : name;
        Boolean required =
            field.containsKey(DomainConstants.Tasks.FIELDS_REQUIRED)
                ? Boolean.valueOf(String.valueOf(field.get(DomainConstants.Tasks.FIELDS_REQUIRED)))
                : false;
        Boolean selectable =
            field.containsKey(DomainConstants.Tasks.FIELDS_SELECTABLE)
                ? Boolean.valueOf(
                    String.valueOf(field.get(DomainConstants.Tasks.FIELDS_SELECTABLE)))
                : false;
        Boolean editable =
            field.containsKey(DomainConstants.Tasks.FIELDS_EDITABLE)
                ? Boolean.valueOf(String.valueOf(field.get(DomainConstants.Tasks.FIELDS_EDITABLE)))
                : true;
        String value =
            field.containsKey(DomainConstants.Tasks.PARAMETERS_VALUE)
                ? String.valueOf(field.get(DomainConstants.Tasks.PARAMETERS_VALUE))
                : null;

        // Handle listValues - either from query or direct value
        List<Map<String, Object>> listValues = null;
        if (field.containsKey(DomainConstants.Tasks.FIELDS_QUERY) && connection != null) {
          listValues =
              dbConService.executeQuery(
                  connection, String.valueOf(field.get(DomainConstants.Tasks.FIELDS_QUERY)));
        } else if (field.containsKey(DomainConstants.Tasks.FIELDS_LIST_VALUES)) {
          // Preserve direct listValues (could be String or List)
          Object listValuesObj = field.get(DomainConstants.Tasks.FIELDS_LIST_VALUES);
          if (listValuesObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> castList = (List<Map<String, Object>>) listValuesObj;
            listValues = castList;
          }
          // If it's a String, we'll pass it as-is through the field values
        }

        Map<String, Object> values =
            getFieldObject(name, label, type, required, selectable, editable, value, listValues);

        // If listValues is a String, preserve it directly
        if (field.containsKey(DomainConstants.Tasks.FIELDS_LIST_VALUES)
            && field.get(DomainConstants.Tasks.FIELDS_LIST_VALUES) instanceof String) {
          values.put(
              DomainConstants.Tasks.FIELDS_LIST_VALUES,
              field.get(DomainConstants.Tasks.FIELDS_LIST_VALUES));
        }

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
   * @param name The field name (identifier)
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
      String name,
      String label,
      String type,
      Boolean required,
      Boolean selectable,
      Boolean editable,
      String value,
      List<Map<String, Object>> listValues) {
    Map<String, Object> values = new HashMap<>();
    values.put(
        DomainConstants.Tasks.FIELDS_NAME,
        name); // CRITICAL: edition-mobile needs this to identify fields
    values.put(DomainConstants.Tasks.FIELDS_TYPE, type);
    values.put(DomainConstants.Tasks.FIELDS_LABEL, label);
    values.put(DomainConstants.Tasks.FIELDS_REQUIRED, required);
    values.put(DomainConstants.Tasks.FIELDS_SELECTABLE, selectable);
    values.put(DomainConstants.Tasks.FIELDS_EDITABLE, editable);
    if (value != null && !value.isEmpty()) {
      values.put(DomainConstants.Tasks.PARAMETERS_VALUE, value);
    }
    if (listValues != null && !listValues.isEmpty()) {
      values.put(DomainConstants.Tasks.FIELDS_LIST_VALUES, listValues);
    }
    return values;
  }
}
