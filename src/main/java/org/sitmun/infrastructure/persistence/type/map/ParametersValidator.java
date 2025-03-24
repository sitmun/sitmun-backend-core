package org.sitmun.infrastructure.persistence.type.map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.List;
import java.util.Map;

public class ParametersValidator implements ConstraintValidator<Parameters, Map<String, Object>> {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final Logger logger = LoggerFactory.getLogger(ParametersValidator.class);

  @Override
  public boolean isValid(Map<String, Object> map, ConstraintValidatorContext constraintValidatorContext) {

    if (map == null) {
      return true;
    }

    if (!map.containsKey("parameters")) {
      logger.error("[parameters] property was expected");
      constraintValidatorContext.buildConstraintViolationWithTemplate("[parameters] property was expected")
        .addConstraintViolation();
      return false;
    }

    if (!(map.get("parameters") instanceof List)) {
      logger.error("[parameters] property must be a list");
      constraintValidatorContext.buildConstraintViolationWithTemplate("[parameters] property must be a list")
        .addConstraintViolation();
      return false;
    }

    boolean result = true;

    @SuppressWarnings("unchecked")
    List<Object> list = (List<Object>) map.get("parameters");

    for (Object obj : list) {
      if (!(obj instanceof Map)) {
        constraintValidatorContext.buildConstraintViolationWithTemplate("[parameters] must be a list of maps")
          .addConstraintViolation();
        return false;
      }
    }

    for (int i = 0; i < list.size(); i++) {
      @SuppressWarnings("unchecked")
      Map<Object, Object> parameter = (Map<Object, Object>) list.get(i);
      if (!(parameter.containsKey("name") && parameter.containsKey("type") && parameter.containsKey("value"))) {
        constraintValidatorContext.buildConstraintViolationWithTemplate("Entry " + i + ": Must have the properties [name], [type] and [value]")
          .addConstraintViolation();
        result = false;
      } else if (parameter.size() != 3) {
        constraintValidatorContext.buildConstraintViolationWithTemplate("Entry " + i + ": Must have only the properties [name], [type] and [value]")
          .addConstraintViolation();
        result = false;
      } else if (!(parameter.get("name") instanceof String)) {
        constraintValidatorContext.buildConstraintViolationWithTemplate("Entry " + i + ": [name] property must be a string")
          .addConstraintViolation();
        result = false;
      } else if (!(parameter.get("type") instanceof String)) {
        constraintValidatorContext.buildConstraintViolationWithTemplate("Entry " + i + ": [type] property must be a string")
          .addConstraintViolation();
        result = false;
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
              constraintValidatorContext.buildConstraintViolationWithTemplate("Entry " + i + ": " + name + " property contains '" + value + "' when number was expected")
                .addConstraintViolation();
              result = false;
            }
          }
        } else if ("boolean".equals(type)) {
          if (value instanceof String) {
            try {
              objectMapper.readValue(value.toString(), Boolean.class);
            } catch (JsonProcessingException e) {
              constraintValidatorContext.buildConstraintViolationWithTemplate("Entry " + i + ": " + name + " property contains '" + value + "' when boolean was expected")
                .addConstraintViolation();
              result = false;
            }
          }
        } else if ("array".equals(type)) {
          if (value instanceof String) {
            try {
              objectMapper.readValue(value.toString(), List.class);
            } catch (JsonProcessingException e) {
              constraintValidatorContext.buildConstraintViolationWithTemplate("Entry " + i + ": " + name + " property contains '" + value + "' when array was expected")
                .addConstraintViolation();
              result = false;
            }
          }
        } else if ("object".equals(type)) {
          if (value instanceof String) {
            try {
              objectMapper.readValue(value.toString(), Map.class);
            } catch (JsonProcessingException e) {
              constraintValidatorContext.buildConstraintViolationWithTemplate("Entry " + i + ": " + name + " property contains '" + value + "' when map was expected")
                .addConstraintViolation();
              result = false;
            }
          }
        } else if ("null".equals(type)) {
          if (value != null) {
            constraintValidatorContext.buildConstraintViolationWithTemplate("Entry " + i + ": " + name + " property contains '" + value + "' when null was expected")
              .addConstraintViolation();
            result = false;
          }
        } else {
          constraintValidatorContext.buildConstraintViolationWithTemplate("Entry " + i + ": " + name + " property contains '" + value + "' when of unexpected type '" + type + "'")
            .addConstraintViolation();
          result = false;
        }
      }
    }
    return result;
  }
}
