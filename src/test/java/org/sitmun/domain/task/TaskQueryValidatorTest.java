package org.sitmun.domain.task;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.sitmun.domain.DomainConstants.Tasks.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sitmun.domain.task.parameter.TaskParameter;
import org.sitmun.domain.task.parameter.TaskParameterProcessor;
import org.sitmun.domain.task.type.TaskType;
import org.springframework.data.rest.core.RepositoryConstraintViolationException;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaskQueryValidator unit tests")
class TaskQueryValidatorTest {

  @Mock private TaskParameterProcessor taskParameterProcessor;

  private TaskQueryValidator validator;

  @BeforeEach
  void setUp() {
    validator = new TaskQueryValidator(taskParameterProcessor);
  }

  /**
   * Helper method to set up mocks for task.getProperties() and taskParameterProcessor.parse().
   *
   * @param task mocked task
   * @param properties task properties containing parameters
   */
  @SuppressWarnings("unchecked")
  private void mockTaskParameterParsing(Task task, Map<String, Object> properties) {
    when(task.getProperties()).thenReturn(properties);

    List<Map<String, Object>> paramList =
        (List<Map<String, Object>>) properties.getOrDefault(PROPERTY_PARAMETERS, List.of());
    List<TaskParameter> taskParameters =
        paramList.stream()
            .map(
                param -> {
                  String name = (String) param.getOrDefault("variable", "unknown");
                  String rawValue = (String) param.get("value");
                  Boolean provided = (Boolean) param.get("provided");
                  boolean providedFlag = Boolean.TRUE.equals(provided);
                  return new TaskParameter(
                      name, rawValue, null, null, null, providedFlag, null, null, param);
                })
            .toList();
    when(taskParameterProcessor.parse(task)).thenReturn(taskParameters);
  }

  @Test
  @DisplayName("accept returns true when task type id matches Query (seed TASK_TYPE_ID_QUERY)")
  void acceptReturnsTrueForQueryTaskType() {
    // Given
    Task task = mock(Task.class);
    TaskType taskType = mock(TaskType.class);
    when(taskType.getId()).thenReturn(TASK_TYPE_ID_QUERY);
    when(task.getType()).thenReturn(taskType);

    // When
    boolean result = validator.accept(task);

    // Then
    assertTrue(result);
  }

  @Test
  @DisplayName(
      "accept returns true for Query id even when localized title differs from English \"Query\"")
  void acceptReturnsTrueForQueryTypeIdRegardlessOfTranslatedTitle() {
    Task task = mock(Task.class);
    TaskType taskType = mock(TaskType.class);
    when(taskType.getId()).thenReturn(TASK_TYPE_ID_QUERY);
    lenient().when(taskType.getTitle()).thenReturn("Consulta");
    when(task.getType()).thenReturn(taskType);

    assertTrue(validator.accept(task));
  }

  @Test
  @DisplayName("accept returns false for non-Query task type id")
  void acceptReturnsFalseForNonQueryTaskType() {
    // Given
    Task task = mock(Task.class);
    TaskType taskType = mock(TaskType.class);
    when(taskType.getId()).thenReturn(TASK_TYPE_ID_MORE_INFO);
    when(task.getType()).thenReturn(taskType);

    // When
    boolean result = validator.accept(task);

    // Then
    assertFalse(result);
  }

  @Test
  @DisplayName("accept returns false when task type id is null")
  void acceptReturnsFalseWhenTaskTypeIdIsNull() {
    // Given
    Task task = mock(Task.class);
    TaskType taskType = mock(TaskType.class);
    when(taskType.getId()).thenReturn(null);
    when(task.getType()).thenReturn(taskType);

    // When
    boolean result = validator.accept(task);

    // Then
    assertFalse(result);
  }

  @Test
  @DisplayName(
      "accept returns false when task type id is not TASK_TYPE_ID_QUERY (id wins over title)")
  void acceptReturnsFalseWhenTaskTypeIdIsNotQuery() {
    Task task = mock(Task.class);
    TaskType taskType = mock(TaskType.class);
    when(taskType.getId()).thenReturn(TASK_TYPE_ID_BASIC);
    when(task.getType()).thenReturn(taskType);

    assertFalse(validator.accept(task));
  }

  @Test
  @DisplayName("accept returns false for null task type")
  void acceptReturnsFalseForNullTaskType() {
    // Given
    Task task = mock(Task.class);
    when(task.getType()).thenReturn(null);

    // When
    boolean result = validator.accept(task);

    // Then
    assertFalse(result);
  }

  @Test
  @DisplayName("accept returns false for null task")
  void acceptReturnsFalseForNullTask() {
    // When
    boolean result = validator.accept(null);

    // Then
    assertFalse(result);
  }

  @Test
  @DisplayName("validate is a no-op when task is null")
  void validateIsNoOpWhenTaskIsNull() {
    assertDoesNotThrow(() -> validator.validate(null));
  }

  @Test
  @DisplayName("validate is a no-op when properties is null")
  void validateIsNoOpWhenPropertiesIsNull() {
    // Given
    Task task = mock(Task.class);
    when(task.getProperties()).thenReturn(null);

    // When/Then - no exception thrown
    assertDoesNotThrow(() -> validator.validate(task));
  }

  @Test
  @DisplayName(
      "validate is a no-op for web-api-query scope (proxied) even with provided parameters")
  void validateIsNoOpForProxiedWebApiQuery() {
    // Given
    Task task = mock(Task.class);
    Map<String, Object> properties = new HashMap<>();
    properties.put(PROPERTY_SCOPE, SCOPE_WEB_API_QUERY);

    Map<String, Object> param = new HashMap<>();
    param.put("variable", "apiKey");
    param.put("provided", true);
    properties.put(PROPERTY_PARAMETERS, List.of(param));

    when(task.getProperties()).thenReturn(properties);

    // When/Then - no exception thrown
    assertDoesNotThrow(() -> validator.validate(task));
  }

  @Test
  @DisplayName("validate throws exception for web-api-query-no-proxy with provided parameters")
  void validateThrowsExceptionForNoProxyWithProvidedParameters() {
    // Given
    Task task = mock(Task.class);

    Map<String, Object> properties = new HashMap<>();
    properties.put(PROPERTY_SCOPE, SCOPE_WEB_API_QUERY_NO_PROXY);

    Map<String, Object> param = new HashMap<>();
    param.put("variable", "apiKey");
    param.put("provided", true);
    properties.put(PROPERTY_PARAMETERS, List.of(param));

    when(task.getProperties()).thenReturn(properties);
    mockTaskParameterParsing(task, properties);

    // When
    RepositoryConstraintViolationException exception =
        assertThrows(RepositoryConstraintViolationException.class, () -> validator.validate(task));

    // Then
    Errors errors = exception.getErrors();
    assertNotNull(errors);
    assertTrue(errors.hasFieldErrors("properties"));
    FieldError propertiesError = errors.getFieldError("properties");
    assertNotNull(propertiesError);
    assertEquals("directExecution.backendFeaturesNotAllowed", propertiesError.getCode());
  }

  @Test
  @DisplayName("validate throws exception for web-api-query-no-proxy with #{...} in command")
  void validateThrowsExceptionForNoProxyWithSystemVariablesInCommand() {
    // Given
    Task task = mock(Task.class);

    Map<String, Object> properties = new HashMap<>();
    properties.put(PROPERTY_SCOPE, SCOPE_WEB_API_QUERY_NO_PROXY);
    properties.put(PROPERTY_COMMAND, "https://api.example.com?app=#{APP_ID}");

    mockTaskParameterParsing(task, properties);

    // When
    RepositoryConstraintViolationException exception =
        assertThrows(RepositoryConstraintViolationException.class, () -> validator.validate(task));

    // Then
    Errors errors = exception.getErrors();
    assertNotNull(errors);
    assertTrue(errors.hasFieldErrors("properties"));
    FieldError propertiesError = errors.getFieldError("properties");
    assertNotNull(propertiesError);
    String message = propertiesError.getDefaultMessage();
    assertNotNull(message);
    assertTrue(message.contains("system variables"));
  }

  @Test
  @DisplayName("validate throws exception for web-api-query-no-proxy with authentication")
  void validateThrowsExceptionForNoProxyWithAuthentication() {
    // Given
    Task task = mock(Task.class);

    Map<String, Object> properties = new HashMap<>();
    properties.put(PROPERTY_SCOPE, SCOPE_WEB_API_QUERY_NO_PROXY);
    properties.put(PROPERTY_AUTHENTICATION_MODE, "Basic");
    properties.put(PROPERTY_COMMAND, "https://api.example.com/data");

    mockTaskParameterParsing(task, properties);

    // When
    RepositoryConstraintViolationException exception =
        assertThrows(RepositoryConstraintViolationException.class, () -> validator.validate(task));

    // Then
    Errors errors = exception.getErrors();
    assertNotNull(errors);
    assertTrue(errors.hasFieldErrors("properties"));
    FieldError propertiesError = errors.getFieldError("properties");
    assertNotNull(propertiesError);
    String message = propertiesError.getDefaultMessage();
    assertNotNull(message);
    assertTrue(message.contains("Authentication"));
  }

  @Test
  @DisplayName("validate throws exception for web-api-query-no-proxy with headers")
  void validateThrowsExceptionForNoProxyWithHeaders() {
    // Given
    Task task = mock(Task.class);

    Map<String, Object> properties = new HashMap<>();
    properties.put(PROPERTY_SCOPE, SCOPE_WEB_API_QUERY_NO_PROXY);
    properties.put(PROPERTY_COMMAND, "https://api.example.com/data");
    properties.put(PROPERTY_HEADERS, Map.of("X-Custom", "value"));

    mockTaskParameterParsing(task, properties);

    // When
    RepositoryConstraintViolationException exception =
        assertThrows(RepositoryConstraintViolationException.class, () -> validator.validate(task));

    // Then
    Errors errors = exception.getErrors();
    assertNotNull(errors);
    assertTrue(errors.hasFieldErrors("properties"));
    FieldError propertiesError = errors.getFieldError("properties");
    assertNotNull(propertiesError);
    String message = propertiesError.getDefaultMessage();
    assertNotNull(message);
    assertTrue(message.contains("Headers"));
  }

  @Test
  @DisplayName("validate throws exception for external-link with provided parameters")
  void validateThrowsExceptionForExternalLinkWithProvidedParameters() {
    // Given
    Task task = mock(Task.class);

    Map<String, Object> properties = new HashMap<>();
    properties.put(PROPERTY_SCOPE, SCOPE_URL_QUERY);

    Map<String, Object> param = new HashMap<>();
    param.put("variable", "apiKey");
    param.put("provided", true);
    properties.put(PROPERTY_PARAMETERS, List.of(param));

    mockTaskParameterParsing(task, properties);

    // When
    RepositoryConstraintViolationException exception =
        assertThrows(RepositoryConstraintViolationException.class, () -> validator.validate(task));

    // Then
    Errors errors = exception.getErrors();
    assertNotNull(errors);
    assertTrue(errors.hasFieldErrors("properties"));
    FieldError propertiesError = errors.getFieldError("properties");
    assertNotNull(propertiesError);
    String message = propertiesError.getDefaultMessage();
    assertNotNull(message);
    assertTrue(message.contains(SCOPE_URL_QUERY));
  }

  @Test
  @DisplayName("validate allows web-api-query-no-proxy with plain public parameters")
  void validateAllowsNoProxyWithPlainParameters() {
    // Given
    Task task = mock(Task.class);
    Map<String, Object> properties = new HashMap<>();
    properties.put(PROPERTY_SCOPE, SCOPE_WEB_API_QUERY_NO_PROXY);
    properties.put(PROPERTY_COMMAND, "https://public-api.example.com/data");

    Map<String, Object> param = new HashMap<>();
    param.put("variable", "format");
    param.put("value", "json");
    param.put("provided", false);
    properties.put(PROPERTY_PARAMETERS, List.of(param));

    mockTaskParameterParsing(task, properties);

    // When/Then - no exception thrown
    assertDoesNotThrow(() -> validator.validate(task));
  }

  @Test
  @DisplayName("validate allows external-link with plain command and no backend features")
  void validateAllowsExternalLinkWithPlainCommand() {
    // Given
    Task task = mock(Task.class);
    Map<String, Object> properties = new HashMap<>();
    properties.put(PROPERTY_SCOPE, SCOPE_URL_QUERY);
    properties.put(PROPERTY_COMMAND, "https://www.example.com/document.pdf");

    mockTaskParameterParsing(task, properties);

    // When/Then - no exception thrown
    assertDoesNotThrow(() -> validator.validate(task));
  }
}
