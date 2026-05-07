package org.sitmun.authorization.client.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.sitmun.domain.DomainConstants.Tasks.PROPERTY_PARAMETERS;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.authorization.client.dto.TaskDto;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.task.Task;
import org.sitmun.domain.task.parameter.TaskParameterProcessor;
import org.sitmun.domain.territory.Territory;
import org.sitmun.infrastructure.variables.SystemVariableResolver;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("TaskQuerySqlService security and parameter visibility tests")
class TaskQuerySqlServiceTest {

  private TaskQuerySqlService service;

  @BeforeEach
  void setUp() {
    // Create a real TaskParameterProcessor with mock SystemVariableResolver
    SystemVariableResolver mockResolver = mock(SystemVariableResolver.class);
    when(mockResolver.resolve(anyString(), any())).thenAnswer(inv -> inv.getArgument(0));

    TaskParameterProcessor processor = new TaskParameterProcessor(mockResolver);

    service = new TaskQuerySqlService(processor);
    ReflectionTestUtils.setField(service, "proxyUrl", "http://localhost:8080/middleware");
  }

  @Test
  @DisplayName("map excludes provided parameters from client profile")
  void mapExcludesProvidedParametersFromClientProfile() {
    // Given
    Task task = mock(Task.class);
    Application application = mock(Application.class);
    Territory territory = mock(Territory.class);

    when(task.getId()).thenReturn(1);
    when(application.getId()).thenReturn(1);
    when(territory.getId()).thenReturn(1);

    Map<String, Object> providedParam = new HashMap<>();
    providedParam.put("variable", "apiKey");
    providedParam.put("value", "secret123");
    providedParam.put("provided", true);
    providedParam.put("type", "string");
    providedParam.put("required", false);

    Map<String, Object> normalParam = new HashMap<>();
    normalParam.put("variable", "filter");
    normalParam.put("value", null);
    normalParam.put("type", "string");
    normalParam.put("required", true);

    Map<String, Object> properties = new HashMap<>();
    properties.put(PROPERTY_PARAMETERS, List.of(providedParam, normalParam));
    when(task.getProperties()).thenReturn(properties);

    // When
    TaskDto result = service.map(task, application, territory);

    // Then
    assertNotNull(result.getParameters());
    assertFalse(
        result.getParameters().containsKey("apiKey"), "Provided parameter should not be exposed");
    assertTrue(result.getParameters().containsKey("filter"), "Normal parameter should be exposed");
  }

  @Test
  @DisplayName("map excludes locked parameters (#{...} system variables) from client profile")
  void mapExcludesLockedParametersFromClientProfile() {
    // Given
    Task task = mock(Task.class);
    Application application = mock(Application.class);
    Territory territory = mock(Territory.class);

    when(task.getId()).thenReturn(1);
    when(application.getId()).thenReturn(1);
    when(territory.getId()).thenReturn(1);

    Map<String, Object> lockedParam = new HashMap<>();
    lockedParam.put("variable", "userId");
    lockedParam.put("value", "#{USER_ID}");
    lockedParam.put("type", "string");
    lockedParam.put("required", false);
    // No need for provided flag - #{...} implies backend-provided (defensive)

    Map<String, Object> normalParam = new HashMap<>();
    normalParam.put("variable", "where");
    normalParam.put("value", null);
    normalParam.put("type", "string");
    normalParam.put("required", false);

    Map<String, Object> properties = new HashMap<>();
    properties.put(PROPERTY_PARAMETERS, List.of(lockedParam, normalParam));
    when(task.getProperties()).thenReturn(properties);

    // When
    TaskDto result = service.map(task, application, territory);

    // Then
    assertNotNull(result.getParameters());
    assertFalse(
        result.getParameters().containsKey("userId"), "Locked parameter should not be exposed");
    assertTrue(result.getParameters().containsKey("where"), "Normal parameter should be exposed");
  }

  @Test
  @DisplayName("map excludes parameters with uppercase #{...} system variable expressions")
  void mapExcludesParametersWithUppercaseSystemVariableExpressions() {
    // Given
    Task task = mock(Task.class);
    Application application = mock(Application.class);
    Territory territory = mock(Territory.class);

    when(task.getId()).thenReturn(1);
    when(application.getId()).thenReturn(1);
    when(territory.getId()).thenReturn(1);

    Map<String, Object> spelParam = new HashMap<>();
    spelParam.put("variable", "userEmail");
    spelParam.put("value", "#{USER_EMAIL}");
    spelParam.put("type", "string");
    spelParam.put("required", false);
    // No need for provided flag - #{...} implies backend-provided (defensive)

    Map<String, Object> normalParam = new HashMap<>();
    normalParam.put("variable", "status");
    normalParam.put("value", "active");
    normalParam.put("type", "string");
    normalParam.put("required", false);

    Map<String, Object> properties = new HashMap<>();
    properties.put(PROPERTY_PARAMETERS, List.of(spelParam, normalParam));
    when(task.getProperties()).thenReturn(properties);

    // When
    TaskDto result = service.map(task, application, territory);

    // Then
    assertNotNull(result.getParameters());
    assertFalse(
        result.getParameters().containsKey("userEmail"), "SpEL parameter should not be exposed");
    assertTrue(result.getParameters().containsKey("status"), "Normal parameter should be exposed");
  }

  @Test
  @DisplayName("map includes literal default parameters in client profile")
  void mapIncludesLiteralDefaultParametersInClientProfile() {
    // Given
    Task task = mock(Task.class);
    Application application = mock(Application.class);
    Territory territory = mock(Territory.class);

    when(task.getId()).thenReturn(1);
    when(application.getId()).thenReturn(1);
    when(territory.getId()).thenReturn(1);

    Map<String, Object> literalParam = new HashMap<>();
    literalParam.put("variable", "format");
    literalParam.put("value", "json");
    literalParam.put("type", "string");
    literalParam.put("required", false);

    Map<String, Object> properties = new HashMap<>();
    properties.put(PROPERTY_PARAMETERS, List.of(literalParam));
    when(task.getProperties()).thenReturn(properties);

    // When
    TaskDto result = service.map(task, application, territory);

    // Then
    assertNotNull(result.getParameters());
    assertTrue(result.getParameters().containsKey("format"), "Literal default should be exposed");
  }

  @Test
  @DisplayName("map includes declared-without-default parameters in client profile")
  void mapIncludesDeclaredWithoutDefaultParametersInClientProfile() {
    // Given
    Task task = mock(Task.class);
    Application application = mock(Application.class);
    Territory territory = mock(Territory.class);

    when(task.getId()).thenReturn(1);
    when(application.getId()).thenReturn(1);
    when(territory.getId()).thenReturn(1);

    Map<String, Object> noDefaultParam = new HashMap<>();
    noDefaultParam.put("variable", "searchTerm");
    noDefaultParam.put("value", null);
    noDefaultParam.put("type", "string");
    noDefaultParam.put("required", true);

    Map<String, Object> properties = new HashMap<>();
    properties.put(PROPERTY_PARAMETERS, List.of(noDefaultParam));
    when(task.getProperties()).thenReturn(properties);

    // When
    TaskDto result = service.map(task, application, territory);

    // Then
    assertNotNull(result.getParameters());
    assertTrue(
        result.getParameters().containsKey("searchTerm"),
        "Declared-without-default should be exposed");
  }

  @Test
  @DisplayName("map returns null parameters when all are backend-only")
  void mapReturnsNullParametersWhenAllAreBackendOnly() {
    // Given
    Task task = mock(Task.class);
    Application application = mock(Application.class);
    Territory territory = mock(Territory.class);

    when(task.getId()).thenReturn(1);
    when(application.getId()).thenReturn(1);
    when(territory.getId()).thenReturn(1);

    Map<String, Object> providedParam = new HashMap<>();
    providedParam.put("variable", "apiKey");
    providedParam.put("value", "secret");
    providedParam.put("provided", true);
    providedParam.put("type", "string");
    providedParam.put("required", false);

    Map<String, Object> lockedParam = new HashMap<>();
    lockedParam.put("variable", "userId");
    lockedParam.put("value", "#{USER_ID}");
    lockedParam.put("type", "string");
    lockedParam.put("required", false);
    // No need for provided flag - #{...} implies backend-provided (defensive)

    Map<String, Object> properties = new HashMap<>();
    properties.put(PROPERTY_PARAMETERS, List.of(providedParam, lockedParam));
    when(task.getProperties()).thenReturn(properties);

    // When
    TaskDto result = service.map(task, application, territory);

    // Then
    assertNull(result.getParameters(), "Should return null when no client-visible parameters");
  }
}
