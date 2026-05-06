package org.sitmun.domain.task;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sitmun.domain.DomainConstants;
import org.sitmun.domain.task.type.TaskType;
import org.springframework.data.rest.core.RepositoryConstraintViolationException;
import org.springframework.validation.Errors;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaskQueryValidator unit tests")
class TaskQueryValidatorTest {

  private TaskQueryValidator validator;

  @BeforeEach
  void setUp() {
    validator = new TaskQueryValidator();
  }

  @Test
  @DisplayName("accept returns true for Query task type")
  void acceptReturnsTrueForQueryTaskType() {
    // Given
    Task task = mock(Task.class);
    TaskType taskType = mock(TaskType.class);
    when(taskType.getTitle()).thenReturn("Query");
    when(task.getType()).thenReturn(taskType);

    // When
    boolean result = validator.accept(task);

    // Then
    assertTrue(result);
  }

  @Test
  @DisplayName("accept returns false for non-Query task type")
  void acceptReturnsFalseForNonQueryTaskType() {
    // Given
    Task task = mock(Task.class);
    TaskType taskType = mock(TaskType.class);
    when(taskType.getTitle()).thenReturn("MoreInfo");
    when(task.getType()).thenReturn(taskType);

    // When
    boolean result = validator.accept(task);

    // Then
    assertFalse(result);
  }

  @Test
  @DisplayName("accept returns false for null task type title")
  void acceptReturnsFalseForNullTaskTypeTitle() {
    // Given
    Task task = mock(Task.class);
    TaskType taskType = mock(TaskType.class);
    when(taskType.getTitle()).thenReturn(null);
    when(task.getType()).thenReturn(taskType);

    // When
    boolean result = validator.accept(task);

    // Then
    assertFalse(result);
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
    properties.put(DomainConstants.Tasks.PROPERTY_SCOPE, DomainConstants.Tasks.SCOPE_WEB_API_QUERY);

    Map<String, Object> param = new HashMap<>();
    param.put("variable", "apiKey");
    param.put("provided", true);
    properties.put(DomainConstants.Tasks.PROPERTY_PARAMETERS, List.of(param));

    when(task.getProperties()).thenReturn(properties);

    // When/Then - no exception thrown
    assertDoesNotThrow(() -> validator.validate(task));
  }

  @Test
  @DisplayName(
      "validate throws RepositoryConstraintViolationException for web-api-query-no-proxy with provided parameters")
  void validateThrowsExceptionForNoProxyWithProvidedParameters() {
    // Given
    Task task = mock(Task.class);
    TaskType taskType = mock(TaskType.class);
    when(taskType.getTitle()).thenReturn("Query");
    when(task.getType()).thenReturn(taskType);

    Map<String, Object> properties = new HashMap<>();
    properties.put(
        DomainConstants.Tasks.PROPERTY_SCOPE, DomainConstants.Tasks.SCOPE_WEB_API_QUERY_NO_PROXY);

    Map<String, Object> param = new HashMap<>();
    param.put("variable", "apiKey");
    param.put("provided", true);
    properties.put(DomainConstants.Tasks.PROPERTY_PARAMETERS, List.of(param));

    when(task.getProperties()).thenReturn(properties);

    // When
    RepositoryConstraintViolationException exception =
        assertThrows(RepositoryConstraintViolationException.class, () -> validator.validate(task));

    // Then
    Errors errors = exception.getErrors();
    assertNotNull(errors);
    assertTrue(errors.hasFieldErrors("properties"));
    assertEquals(1, errors.getFieldErrorCount());
    assertEquals("parameters.providedNotAllowed", errors.getFieldError("properties").getCode());
    assertEquals(
        "Web API Query (No Proxy) tasks cannot have backend-provided (proxy-injected) variables",
        errors.getFieldError("properties").getDefaultMessage());
  }

  @Test
  @DisplayName("validate is a no-op for web-api-query-no-proxy without provided parameters")
  void validateIsNoOpForNoProxyWithoutProvidedParameters() {
    // Given
    Task task = mock(Task.class);
    Map<String, Object> properties = new HashMap<>();
    properties.put(
        DomainConstants.Tasks.PROPERTY_SCOPE, DomainConstants.Tasks.SCOPE_WEB_API_QUERY_NO_PROXY);

    Map<String, Object> param = new HashMap<>();
    param.put("variable", "publicParam");
    param.put("provided", false);
    properties.put(DomainConstants.Tasks.PROPERTY_PARAMETERS, List.of(param));

    when(task.getProperties()).thenReturn(properties);

    // When/Then - no exception thrown
    assertDoesNotThrow(() -> validator.validate(task));
  }

  @Test
  @DisplayName("validate is a no-op for web-api-query-no-proxy with no parameters")
  void validateIsNoOpForNoProxyWithNoParameters() {
    // Given
    Task task = mock(Task.class);
    Map<String, Object> properties = new HashMap<>();
    properties.put(
        DomainConstants.Tasks.PROPERTY_SCOPE, DomainConstants.Tasks.SCOPE_WEB_API_QUERY_NO_PROXY);

    when(task.getProperties()).thenReturn(properties);

    // When/Then - no exception thrown
    assertDoesNotThrow(() -> validator.validate(task));
  }
}
