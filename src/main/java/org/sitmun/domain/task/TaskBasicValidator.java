package org.sitmun.domain.task;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.rest.core.RepositoryConstraintViolationException;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

import java.util.List;
import java.util.Map;

@Component
public class TaskBasicValidator implements TaskValidator {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public boolean accept(Task task) {
    if (getProperties(task).containsKey("scope")) {
      return false;
    }
    return true;
  }

  @Override
  public void validate(Task task) throws RepositoryConstraintViolationException {
    List<Map<String, Object>> parameters = getParameters(task);
    Errors errors = init(task);
    for(Map<String, Object> parameter : parameters) {
      if (!(parameter.containsKey("name") && parameter.containsKey("type") && parameter.containsKey("value"))) {
        errors.rejectValue("properties", "parameters.missing", "[name], [type] and [value] properties were expected");
      } else if (parameter.size() != 3) {
        errors.rejectValue("properties", "parameters.extra", "Extra properties were found");
      } else if (!(parameter.get("name") instanceof String) || !(parameter.get("type") instanceof String)) {
        errors.rejectValue("properties", "parameters.invalid", "Invalid parameter types");
      } else if (!(parameter.get("name") instanceof String)) {
        errors.rejectValue("properties", "parameters.name", "[name] property must be a string");
      } else if (!(parameter.get("type") instanceof String)) {
        errors.rejectValue("properties", "parameters.type", "[type] property must be a string");
      } else {
        String name = (String) parameter.get("name");
        String type = (String) parameter.get("type");
        Object value = parameter.get("value");

        //noinspection StatementWithEmptyBody
        if ("string".equals(type)) {
          // Any value is valid
          // null is considered equal to the empty string
        } else if ("number".equals(type)) {
          if (value instanceof String) {
            try {
              objectMapper.readValue(value.toString(), Number.class);
            } catch (JsonProcessingException e) {
              errors.rejectValue("properties", "parameters.number", name + " property contains '" + value + "' when number was expected");
            }
          }
        } else if ("boolean".equals(type)) {
          if (value instanceof String) {
            try {
              objectMapper.readValue(value.toString(), Boolean.class);
            } catch (JsonProcessingException e) {
              errors.rejectValue("properties", "parameters.boolean", name + " property contains '" + value + "' when boolean was expected");
            }
          }
        } else if ("array".equals(type)) {
          if (value instanceof String) {
            try {
              objectMapper.readValue(value.toString(), List.class);
            } catch (JsonProcessingException e) {
              errors.rejectValue("properties", "parameters.array", name + " property contains '" + value + "' when array was expected");
            }
          }
        } else if ("object".equals(type)) {
          if (value instanceof String) {
            try {
              objectMapper.readValue(value.toString(), Map.class);
            } catch (JsonProcessingException e) {
              errors.rejectValue("properties", "parameters.object", name + " property contains '" + value + "' when map was expected");
            }
          }
        } else if ("null".equals(type)) {
          if (value != null) {
            errors.rejectValue("properties", "parameters.null", name + " property contains '" + value + "' when null was expected");
          }
        } else {
          errors.rejectValue("properties", "parameters.any", name + " property contains '" + value + "' when type '" + type + "'");
        }
      }
    }
    if (errors.hasErrors()) {
      throw new RepositoryConstraintViolationException(errors);
    }
  }
}
