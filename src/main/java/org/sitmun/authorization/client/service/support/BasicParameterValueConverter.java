package org.sitmun.authorization.client.service.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

/**
 * Converter for basic task parameter values.
 *
 * <p>Consolidates type-based conversion logic previously duplicated between {@code
 * TaskBasicService.typeBasedConversion} (profile output) and {@code
 * TaskBasicValidator.validateValueMatchesDeclaredType} (admin validation).
 *
 * <p>Backed by a single Jackson {@link ObjectMapper} for JSON parsing.
 */
@Slf4j
@Component
public class BasicParameterValueConverter {

  /** Spring-binding field for {@link org.sitmun.domain.task.Task#getProperties()} constraints. */
  private static final String ERRORS_PROPERTIES_FIELD = "properties";

  private static final TypeReference<List<Object>> ARRAY_TYPE_REFERENCE = new TypeReference<>() {};
  private static final TypeReference<Map<String, Object>> OBJECT_TYPE_REFERENCE =
      new TypeReference<>() {};

  private final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * Converts a raw parameter value to its typed representation.
   *
   * <p>Used by {@code TaskBasicService} to build client-facing parameter DTOs.
   *
   * <p>A {@code null} {@code rawValue} is treated as a missing value: {@code STRING} yields the
   * empty string for backward compatibility; every other type yields {@code null}. This avoids the
   * latent {@link NullPointerException}s that {@code Double.parseDouble} and {@code
   * ObjectMapper.readValue} would otherwise raise on a null input, and the silent {@code false}
   * that {@code Boolean.parseBoolean(null)} would return.
   *
   * @param type parameter value type
   * @param rawValue raw string value from task properties
   * @return converted value (String, Double, Boolean, List, Map, or null), or null on parse error
   */
  @Nullable
  public Object convert(BasicParameterValueType type, @Nullable String rawValue) {
    if (rawValue == null) {
      return type == BasicParameterValueType.STRING ? "" : null;
    }
    try {
      return switch (type) {
        case STRING -> rawValue;
        case NUMBER -> Double.parseDouble(rawValue);
        case BOOLEAN -> Boolean.parseBoolean(rawValue);
        case ARRAY -> objectMapper.readValue(rawValue, ARRAY_TYPE_REFERENCE);
        case OBJECT -> objectMapper.readValue(rawValue, OBJECT_TYPE_REFERENCE);
        case NULL -> null;
      };
    } catch (JsonProcessingException e) {
      log.error("Error processing {} type for value {}", type.getTypeString(), rawValue, e);
      return null;
    } catch (NumberFormatException e) {
      log.error("Error parsing number from value {}", rawValue, e);
      return null;
    }
  }

  /**
   * Validates that a parameter value matches its declared type.
   *
   * <p>Used by {@code TaskBasicValidator} to enforce type constraints on task save.
   *
   * @param name parameter name (for error messages)
   * @param type declared parameter type
   * @param value raw value to validate
   * @param errors Spring validation errors accumulator
   */
  public void validate(
      String name, BasicParameterValueType type, @Nullable Object value, Errors errors) {
    switch (type) {
      case STRING -> {
        // Any value is valid; null is treated like an empty string.
      }
      case NUMBER ->
          rejectIfStringDoesNotParseAs(
              name, value, errors, "parameters.number", "number", Number.class);
      case BOOLEAN ->
          rejectIfStringDoesNotParseAs(
              name, value, errors, "parameters.boolean", "boolean", Boolean.class);
      case ARRAY ->
          rejectIfStringDoesNotParseAs(
              name, value, errors, "parameters.array", "array", List.class);
      case OBJECT ->
          rejectIfStringDoesNotParseAs(name, value, errors, "parameters.object", "map", Map.class);
      case NULL -> rejectIfValueNotNull(name, value, errors);
    }
  }

  private void rejectIfStringDoesNotParseAs(
      String name,
      Object value,
      Errors errors,
      String errorCode,
      String expectedLabel,
      Class<?> jsonTarget) {
    if (!(value instanceof String stringValue)) {
      return;
    }
    try {
      objectMapper.readValue(stringValue, objectMapper.constructType(jsonTarget));
    } catch (JsonProcessingException e) {
      errors.rejectValue(
          ERRORS_PROPERTIES_FIELD,
          errorCode,
          String.format("[%s] value must be valid %s", name, expectedLabel));
    }
  }

  private void rejectIfValueNotNull(String name, Object value, Errors errors) {
    if (value != null) {
      errors.rejectValue(
          ERRORS_PROPERTIES_FIELD,
          "parameters.null",
          String.format("[%s] value must be null", name));
    }
  }
}
