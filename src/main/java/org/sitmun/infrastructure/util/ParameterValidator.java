package org.sitmun.infrastructure.util;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.sitmun.domain.DomainConstants;

public class ParameterValidator {

  private static final Pattern SYSTEM_VARIABLE_PATTERN = Pattern.compile("#\\{[^}]+}");

  private ParameterValidator() {}

  public static boolean hasProvidedVariables(Map<String, Object> properties) {
    if (properties == null) {
      return false;
    }

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> parameters =
        (List<Map<String, Object>>)
            properties.getOrDefault(
                DomainConstants.Tasks.PROPERTY_PARAMETERS, Collections.emptyList());

    return parameters.stream()
        .anyMatch(
            param -> {
              Object provided = param.get(DomainConstants.Tasks.PARAMETERS_PROVIDED);
              return Boolean.TRUE.equals(provided)
                  || "true".equalsIgnoreCase(String.valueOf(provided));
            });
  }

  public static boolean containsSystemVariables(String value) {
    if (value == null) {
      return false;
    }
    return SYSTEM_VARIABLE_PATTERN.matcher(value).find();
  }

  public static void validateProvidedFlag(Map<String, Object> properties) {
    if (properties == null) {
      return;
    }

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> parameters =
        (List<Map<String, Object>>)
            properties.getOrDefault(
                DomainConstants.Tasks.PROPERTY_PARAMETERS, Collections.emptyList());

    for (Map<String, Object> param : parameters) {
      Object valueObj = param.get(DomainConstants.Tasks.PARAMETERS_VALUE);
      if (valueObj == null) {
        continue;
      }

      String value = String.valueOf(valueObj);
      boolean hasSystemVars = containsSystemVariables(value);

      if (hasSystemVars) {
        Object providedObj = param.get(DomainConstants.Tasks.PARAMETERS_PROVIDED);
        boolean isProvided =
            Boolean.TRUE.equals(providedObj)
                || "true".equalsIgnoreCase(String.valueOf(providedObj));

        if (!isProvided) {
          String varName = getVariableName(param);
          throw new IllegalArgumentException(
              "System variables (#{...}) require 'provided' flag for variable: " + varName);
        }
      }
    }
  }

  public static void validateNoProvidedVariables(Map<String, Object> properties, String taskType) {
    if (hasProvidedVariables(properties)) {
      throw new IllegalArgumentException(
          taskType + " tasks cannot have provided variables (no proxy execution path)");
    }
  }

  private static String getVariableName(Map<String, Object> param) {
    String varName = TaskParameterUtil.getParameterVariable(param);
    if (varName != null) {
      return varName;
    }
    Object label = param.get(DomainConstants.Tasks.PARAMETERS_LABEL);
    return label != null ? String.valueOf(label) : "unknown";
  }
}
