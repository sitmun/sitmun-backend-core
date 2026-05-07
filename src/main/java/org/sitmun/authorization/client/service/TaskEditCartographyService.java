package org.sitmun.authorization.client.service;

import static org.sitmun.authorization.client.AuthorizationConstants.TaskDto.*;
import static org.sitmun.authorization.client.AuthorizationConstants.TaskDto.EDITION;
import static org.sitmun.authorization.client.AuthorizationConstants.TaskDto.PARAMETER_REQUIRED;
import static org.sitmun.authorization.client.AuthorizationConstants.TaskDto.PARAMETER_TYPE;
import static org.sitmun.authorization.client.AuthorizationConstants.TaskDto.PARAMETER_VALUE;
import static org.sitmun.domain.DomainConstants.Tasks.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.sitmun.administration.service.database.DatabaseConnectionService;
import org.sitmun.authorization.client.dto.TaskDto;
import org.sitmun.authorization.client.support.ProxyUrlBuilder;
import org.sitmun.domain.DomainConstants;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.cartography.Cartography;
import org.sitmun.domain.database.DatabaseConnection;
import org.sitmun.domain.service.Service;
import org.sitmun.domain.task.Task;
import org.sitmun.domain.task.parameter.TaskParameter;
import org.sitmun.domain.task.parameter.TaskParameterProcessor;
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
  private final TaskParameterProcessor taskParameterProcessor;

  public TaskEditCartographyService(
      DatabaseConnectionService dbConService, TaskParameterProcessor taskParameterProcessor) {
    this.dbConService = dbConService;
    this.taskParameterProcessor = taskParameterProcessor;
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
    List<TaskParameter> parameters = taskParameterProcessor.parse(task);
    Map<String, Object> parametersDto = convertParametersToClientProfile(parameters);

    Map<String, Object> fields = new HashMap<>();
    Map<String, Object> properties = task.getProperties();
    if (properties != null) {
      fields = convertFieldsToJsonObject(properties, task.getConnection());
    }

    // boolean postRequest = parameters.entrySet().stream().anyMatch(e ->
    // "body".equals(((Map<String,
    // Object>)e.getValue()).get("type")));
    // String paramType =
    //    postRequest
    //        ? DomainConstants.Tasks.PARAM_TYPE_BODY
    //        : DomainConstants.Tasks.PARAM_TYPE_QUERY;
    String paramType = PARAM_TYPE_QUERY;

    Cartography cartography = task.getCartography();
    Service service = cartography.getService();

    String url = ProxyUrlBuilder.forCartographyService(proxyUrl, application, territory, service);
    parametersDto.put(PARAMETER_SERVICE, getParametersObject(paramType, service.getType()));
    String layers = cartography.getLayers().stream().reduce((a, b) -> a + "," + b).orElse("");
    if (DomainConstants.Services.isWfsService(service)) {
      parametersDto.put(PARAMETER_WFS_TYPENAME, getParametersObject(paramType, layers));
    } else {
      parametersDto.put(PARAMETER_LAYERS, getParametersObject(paramType, layers));
    }
    return TaskDto.builder()
        .id("task/" + task.getId())
        .type(EDITION)
        .parameters(parametersDto)
        .fields(fields)
        .url(url)
        .build();
  }

  /**
   * Converts parsed task parameters to client profile DTO format. Only includes client-allowed
   * parameters (excludes backend-only LOCKED and PROVIDED parameters).
   *
   * <p>Output format: {@code {name -> {type, required, value?}}}
   *
   * @param parameters The parsed task parameters
   * @return A map of parameter names to their configuration
   */
  private Map<String, Object> convertParametersToClientProfile(List<TaskParameter> parameters) {
    Map<String, Object> result = new HashMap<>();

    for (TaskParameter param : parameters) {
      if (taskParameterProcessor.classify(param).isBackendOnly()) {
        continue;
      }
      result.put(
          param.name(), taskParameterProcessor.toParameterDtoWithValue(param, PARAM_TYPE_QUERY));
    }

    return result;
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
            properties.getOrDefault(PROPERTY_FIELDS, Collections.emptyList());

    for (Map<String, Object> field : listOfFields) {
      // CRITICAL: 'name' is the minimum required key for fields (edition-mobile contract)
      if (field.containsKey(FIELDS_NAME)) {
        String name = String.valueOf(field.get(FIELDS_NAME));
        String type =
            field.containsKey(FIELDS_TYPE)
                ? String.valueOf(field.get(FIELDS_TYPE))
                : FIELD_TYPE_TEXT;
        String label =
            field.containsKey(FIELDS_LABEL) ? String.valueOf(field.get(FIELDS_LABEL)) : name;
        Boolean required =
            field.containsKey(FIELDS_REQUIRED)
                && Boolean.parseBoolean(String.valueOf(field.get(FIELDS_REQUIRED)));
        Boolean selectable =
            field.containsKey(FIELDS_SELECTABLE)
                && Boolean.parseBoolean(String.valueOf(field.get(FIELDS_SELECTABLE)));
        Boolean editable =
            field.containsKey(FIELDS_EDITABLE)
                && Boolean.parseBoolean(String.valueOf(field.get(FIELDS_EDITABLE)));
        String value =
            field.containsKey(PARAMETERS_VALUE)
                ? String.valueOf(field.get(PARAMETERS_VALUE))
                : null;

        // Handle listValues - either from query or direct value
        List<Map<String, Object>> listValues = null;
        if (field.containsKey(FIELDS_QUERY) && (connection != null)) {
          listValues =
              dbConService.executeQuery(connection, String.valueOf(field.get(FIELDS_QUERY)));
        } else if (field.containsKey(FIELDS_LIST_VALUES)) {
          // Preserve direct listValues (could be String or List)
          Object listValuesObj = field.get(FIELDS_LIST_VALUES);
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
        if (field.containsKey(FIELDS_LIST_VALUES)
            && field.get(FIELDS_LIST_VALUES) instanceof String) {
          values.put(FIELDS_LIST_VALUES, field.get(FIELDS_LIST_VALUES));
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
   * @param value The parameter value (can be null)
   * @return A map containing the parameter configuration
   */
  private Map<String, Object> getParametersObject(String type, String value) {
    Map<String, Object> values = new HashMap<>();
    values.put(PARAMETER_TYPE, type);
    values.put(PARAMETER_REQUIRED, true);
    if (value != null) {
      values.put(PARAMETER_VALUE, value);
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
    values.put(FIELDS_NAME, name); // CRITICAL: edition-mobile needs this to identify fields
    values.put(FIELDS_TYPE, type);
    values.put(FIELDS_LABEL, label);
    values.put(FIELDS_REQUIRED, required);
    values.put(FIELDS_SELECTABLE, selectable);
    values.put(FIELDS_EDITABLE, editable);
    if (value != null && !value.isEmpty()) {
      values.put(PARAMETERS_VALUE, value);
    }
    if (listValues != null && !listValues.isEmpty()) {
      values.put(FIELDS_LIST_VALUES, listValues);
    }
    return values;
  }
}
