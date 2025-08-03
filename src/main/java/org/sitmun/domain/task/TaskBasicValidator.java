package org.sitmun.domain.task;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.springframework.data.rest.core.RepositoryConstraintViolationException;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

@Component
public class TaskBasicValidator implements TaskValidator {

  private static final String PROPERTIES = "properties";

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public boolean accept(Task task) {
    return !getProperties(task).containsKey("scope");
  }

  @Override
  public void validate(Task task) throws RepositoryConstraintViolationException {
    List<Map<String, Object>> parameters = getParameters(task);
    Errors errors = init(task);
    for (Map<String, Object> parameter : parameters) {
      if (!(parameter.containsKey("name")
          && parameter.containsKey("type")
          && parameter.containsKey("value"))) {
        errors.rejectValue(
            PROPERTIES,
            "parameters.missing",
            "[name], [type] and [value] properties were expected");
      } else if (parameter.size() != 3) {
        errors.rejectValue(PROPERTIES, "parameters.extra", "Extra properties were found");
      } else if (!(parameter.get("name") instanceof String name)
          || !(parameter.get("type") instanceof String type)) {
        errors.rejectValue(PROPERTIES, "parameters.invalid", "Invalid parameter types");
      } else {
        Object value = parameter.get("value");

        //noinspection StatementWithEmptyBody
        switch (type) {
          case "string" -> {
            // Any value is valid
            // null is considered equal to the empty string
          }
          case "number" -> {
            if (value instanceof String stringValue) {
              try {
                objectMapper.readValue(stringValue, Number.class);
              } catch (JsonProcessingException e) {
                errors.rejectValue(
                    PROPERTIES,
                    "parameters.number",
                    name + " property contains '" + value + "' when number was expected");
              }
            }
          }
          case "boolean" -> {
            if (value instanceof String stringValue) {
              try {
                objectMapper.readValue(stringValue, Boolean.class);
              } catch (JsonProcessingException e) {
                errors.rejectValue(
                    PROPERTIES,
                    "parameters.boolean",
                    name + " property contains '" + value + "' when boolean was expected");
              }
            }
          }
          case "array" -> {
            if (value instanceof String stringValue) {
              try {
                objectMapper.readValue(stringValue, List.class);
              } catch (JsonProcessingException e) {
                errors.rejectValue(
                    PROPERTIES,
                    "parameters.array",
                    name + " property contains '" + value + "' when array was expected");
              }
            }
          }
          case "object" -> {
            if (value instanceof String stringValue) {
              try {
                objectMapper.readValue(stringValue, Map.class);
              } catch (JsonProcessingException e) {
                errors.rejectValue(
                    PROPERTIES,
                    "parameters.object",
                    name + " property contains '" + value + "' when map was expected");
              }
            }
          }
          case "null" -> {
            if (value != null) {
              errors.rejectValue(
                  PROPERTIES,
                  "parameters.null",
                  name + " property contains '" + value + "' when null was expected");
            }
          }
          default ->
              errors.rejectValue(
                  PROPERTIES,
                  "parameters.any",
                  name + " property contains '" + value + "' when type '" + type + "'");
        }
      }
    }
    if (errors.hasErrors()) {
      throw new RepositoryConstraintViolationException(errors);
    }
  }
}
