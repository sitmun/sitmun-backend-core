package org.sitmun.infrastructure.util;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.sitmun.domain.task.parameter.TaskParameter;

/**
 * Utility for validating task parameters.
 *
 * <p>Phase 4: Now operates on typed {@link TaskParameter} instead of {@code Map<String, Object>}.
 */
public class ParameterValidator {

  private static final Pattern SYSTEM_VARIABLE_PATTERN = Pattern.compile("#\\{[^}]+}");

  private ParameterValidator() {}

  /**
   * Checks if any parameter has the provided flag set.
   *
   * @param parameters Typed task parameters
   * @return true if any parameter has providedFlag = true
   */
  public static boolean hasProvidedVariables(List<TaskParameter> parameters) {
    if (parameters == null || parameters.isEmpty()) {
      return false;
    }
    return parameters.stream().anyMatch(TaskParameter::providedFlag);
  }

  public static boolean containsSystemVariables(String value) {
    if (value == null) {
      return false;
    }
    return SYSTEM_VARIABLE_PATTERN.matcher(value).find();
  }

  /**
   * Checks if any parameter contains system variables (#{...}) in its raw value.
   *
   * @param parameters Typed task parameters
   * @return true if any parameter raw value contains system variables
   */
  public static boolean containsSystemVariablesInParameters(List<TaskParameter> parameters) {
    if (parameters == null || parameters.isEmpty()) {
      return false;
    }
    return parameters.stream().anyMatch(param -> containsSystemVariables(param.rawValue()));
  }

  /**
   * Checks if any value in the given map contains system variables (#{...}).
   *
   * @param map Map to check (e.g., headers or queryParams)
   * @return true if any value contains system variables
   */
  public static boolean containsSystemVariablesInMapValues(Map<String, Object> map) {
    if (map == null || map.isEmpty()) {
      return false;
    }
    return map.values().stream()
        .anyMatch(value -> value != null && containsSystemVariables(String.valueOf(value)));
  }

  /**
   * Validates that no parameters have the provided flag set.
   *
   * @param parameters Typed task parameters
   * @param taskType Task type label for error message
   * @throws IllegalArgumentException if any parameter has providedFlag = true
   */
  public static void validateNoProvidedVariables(List<TaskParameter> parameters, String taskType) {
    if (hasProvidedVariables(parameters)) {
      throw new IllegalArgumentException(
          taskType + " tasks cannot have provided variables (no proxy execution path)");
    }
  }
}
