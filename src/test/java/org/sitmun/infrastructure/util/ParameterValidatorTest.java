package org.sitmun.infrastructure.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.domain.task.parameter.TaskParameter;

@DisplayName("ParameterValidator unit tests")
class ParameterValidatorTest {

  @Test
  @DisplayName("containsSystemVariables returns true when #{...} is present")
  void containsSystemVariablesReturnsTrueWhenPresent() {
    assertTrue(ParameterValidator.containsSystemVariables("#{APP_ID}"));
    assertTrue(ParameterValidator.containsSystemVariables("prefix #{TERRITORY_ID} suffix"));
    assertTrue(ParameterValidator.containsSystemVariables("#{USER_ID}"));
  }

  @Test
  @DisplayName("containsSystemVariables returns false when #{...} is absent")
  void containsSystemVariablesReturnsFalseWhenAbsent() {
    assertFalse(ParameterValidator.containsSystemVariables(null));
    assertFalse(ParameterValidator.containsSystemVariables(""));
    assertFalse(ParameterValidator.containsSystemVariables("literal value"));
    assertFalse(ParameterValidator.containsSystemVariables("{not_a_system_var}"));
  }

  @Test
  @DisplayName("containsSystemVariablesInParameters returns true when parameter value has #{...}")
  void containsSystemVariablesInParametersReturnsTrueWhenPresent() {
    // Given
    TaskParameter param1 =
        new TaskParameter("param1", "#{APP_ID}", null, null, null, false, null, null, Map.of());
    TaskParameter param2 =
        new TaskParameter("param2", "literal", null, null, null, false, null, null, Map.of());
    List<TaskParameter> parameters = List.of(param1, param2);

    // When/Then
    assertTrue(ParameterValidator.containsSystemVariablesInParameters(parameters));
  }

  @Test
  @DisplayName("containsSystemVariablesInParameters returns false when no #{...} present")
  void containsSystemVariablesInParametersReturnsFalseWhenAbsent() {
    // Given
    TaskParameter param1 =
        new TaskParameter("param1", "literal1", null, null, null, false, null, null, Map.of());
    TaskParameter param2 =
        new TaskParameter("param2", "literal2", null, null, null, false, null, null, Map.of());
    List<TaskParameter> parameters = List.of(param1, param2);

    // When/Then
    assertFalse(ParameterValidator.containsSystemVariablesInParameters(parameters));
  }

  @Test
  @DisplayName("containsSystemVariablesInParameters returns false for null or empty list")
  void containsSystemVariablesInParametersReturnsFalseForNullOrEmpty() {
    assertFalse(ParameterValidator.containsSystemVariablesInParameters(null));
    assertFalse(ParameterValidator.containsSystemVariablesInParameters(List.of()));
  }

  @Test
  @DisplayName("containsSystemVariablesInMapValues returns true when map value has #{...}")
  void containsSystemVariablesInMapValuesReturnsTrueWhenPresent() {
    // Given
    Map<String, Object> map = new HashMap<>();
    map.put("Authorization", "Bearer #{API_TOKEN}");
    map.put("Content-Type", "application/json");

    // When/Then
    assertTrue(ParameterValidator.containsSystemVariablesInMapValues(map));
  }

  @Test
  @DisplayName("containsSystemVariablesInMapValues returns false when no #{...} present")
  void containsSystemVariablesInMapValuesReturnsFalseWhenAbsent() {
    // Given
    Map<String, Object> map = new HashMap<>();
    map.put("Authorization", "Bearer token");
    map.put("Content-Type", "application/json");

    // When/Then
    assertFalse(ParameterValidator.containsSystemVariablesInMapValues(map));
  }

  @Test
  @DisplayName("containsSystemVariablesInMapValues returns false for null or empty map")
  void containsSystemVariablesInMapValuesReturnsFalseForNullOrEmpty() {
    assertFalse(ParameterValidator.containsSystemVariablesInMapValues(null));
    assertFalse(ParameterValidator.containsSystemVariablesInMapValues(Map.of()));
  }

  @Test
  @DisplayName("hasProvidedVariables returns true when any parameter has providedFlag set")
  void hasProvidedVariablesReturnsTrueWhenPresent() {
    // Given
    TaskParameter param1 =
        new TaskParameter("param1", "value1", null, null, null, false, null, null, Map.of());
    TaskParameter param2 =
        new TaskParameter("param2", "value2", null, null, null, true, null, null, Map.of());
    List<TaskParameter> parameters = List.of(param1, param2);

    // When/Then
    assertTrue(ParameterValidator.hasProvidedVariables(parameters));
  }

  @Test
  @DisplayName("hasProvidedVariables returns false when no parameter has providedFlag set")
  void hasProvidedVariablesReturnsFalseWhenAbsent() {
    // Given
    TaskParameter param1 =
        new TaskParameter("param1", "value1", null, null, null, false, null, null, Map.of());
    TaskParameter param2 =
        new TaskParameter("param2", "value2", null, null, null, false, null, null, Map.of());
    List<TaskParameter> parameters = List.of(param1, param2);

    // When/Then
    assertFalse(ParameterValidator.hasProvidedVariables(parameters));
  }

  @Test
  @DisplayName("hasProvidedVariables returns false for null or empty list")
  void hasProvidedVariablesReturnsFalseForNullOrEmpty() {
    assertFalse(ParameterValidator.hasProvidedVariables(null));
    assertFalse(ParameterValidator.hasProvidedVariables(List.of()));
  }

  @Test
  @DisplayName("validateNoProvidedVariables throws when provided parameters exist")
  void validateNoProvidedVariablesThrowsWhenPresent() {
    // Given
    TaskParameter param =
        new TaskParameter("apiKey", "secret", null, null, null, true, null, null, Map.of());
    List<TaskParameter> parameters = List.of(param);

    // When/Then
    assertThrows(
        IllegalArgumentException.class,
        () -> ParameterValidator.validateNoProvidedVariables(parameters, "TestTask"));
  }

  @Test
  @DisplayName("validateNoProvidedVariables succeeds when no provided parameters")
  void validateNoProvidedVariablesSucceedsWhenAbsent() {
    // Given
    TaskParameter param =
        new TaskParameter("filter", "value", null, null, null, false, null, null, Map.of());
    List<TaskParameter> parameters = List.of(param);

    // When/Then - no exception thrown
    assertDoesNotThrow(
        () -> ParameterValidator.validateNoProvidedVariables(parameters, "TestTask"));
  }
}
