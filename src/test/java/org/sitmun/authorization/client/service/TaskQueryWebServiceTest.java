package org.sitmun.authorization.client.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.sitmun.domain.DomainConstants.Tasks.*;
import static org.sitmun.domain.DomainConstants.Tasks.PROPERTY_COMMAND;
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

@DisplayName("TaskQueryWebService security and parameter visibility tests")
class TaskQueryWebServiceTest {

  private TaskQueryWebService service;

  @BeforeEach
  void setUp() {
    // Create a real TaskParameterProcessor with mock SystemVariableResolver
    SystemVariableResolver mockResolver = mock(SystemVariableResolver.class);
    when(mockResolver.resolve(anyString(), any())).thenAnswer(inv -> inv.getArgument(0));

    TaskParameterProcessor processor = new TaskParameterProcessor(mockResolver);

    service = new TaskQueryWebService(processor);
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
    normalParam.put("variable", "format");
    normalParam.put("value", null);
    normalParam.put("type", "string");
    normalParam.put("required", true);

    Map<String, Object> properties = new HashMap<>();
    properties.put(PROPERTY_PARAMETERS, List.of(providedParam, normalParam));
    properties.put(PROPERTY_COMMAND, "https://api.example.com/data");
    when(task.getProperties()).thenReturn(properties);

    // When
    TaskDto result = service.map(task, application, territory);

    // Then
    assertNotNull(result.getParameters());
    assertFalse(
        result.getParameters().containsKey("apiKey"), "Provided parameter should not be exposed");
    assertTrue(result.getParameters().containsKey("format"), "Normal parameter should be exposed");
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
    lockedParam.put("variable", "territoryId");
    lockedParam.put("value", "#{TERR_ID}");
    lockedParam.put("type", "string");
    lockedParam.put("required", false);
    // No need for provided flag - #{...} implies backend-provided (defensive)

    Map<String, Object> normalParam = new HashMap<>();
    normalParam.put("variable", "query");
    normalParam.put("value", null);
    normalParam.put("type", "string");
    normalParam.put("required", false);

    Map<String, Object> properties = new HashMap<>();
    properties.put(PROPERTY_PARAMETERS, List.of(lockedParam, normalParam));
    properties.put(PROPERTY_COMMAND, "https://api.example.com/search");
    when(task.getProperties()).thenReturn(properties);

    // When
    TaskDto result = service.map(task, application, territory);

    // Then
    assertNotNull(result.getParameters());
    assertFalse(
        result.getParameters().containsKey("territoryId"),
        "Locked parameter should not be exposed");
    assertTrue(result.getParameters().containsKey("query"), "Normal parameter should be exposed");
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
    spelParam.put("variable", "authToken");
    spelParam.put("value", "#{AUTH_TOKEN}");
    spelParam.put("type", "string");
    spelParam.put("required", false);
    // No need for provided flag - #{...} implies backend-provided (defensive)

    Map<String, Object> normalParam = new HashMap<>();
    normalParam.put("variable", "limit");
    normalParam.put("value", "100");
    normalParam.put("type", "number");
    normalParam.put("required", false);

    Map<String, Object> properties = new HashMap<>();
    properties.put(PROPERTY_PARAMETERS, List.of(spelParam, normalParam));
    properties.put(PROPERTY_COMMAND, "https://api.example.com/items");
    when(task.getProperties()).thenReturn(properties);

    // When
    TaskDto result = service.map(task, application, territory);

    // Then
    assertNotNull(result.getParameters());
    assertFalse(
        result.getParameters().containsKey("authToken"),
        "Complex SpEL parameter should not be exposed");
    assertTrue(result.getParameters().containsKey("limit"), "Normal parameter should be exposed");
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
    literalParam.put("variable", "outputFormat");
    literalParam.put("value", "geojson");
    literalParam.put("type", "string");
    literalParam.put("required", false);

    Map<String, Object> properties = new HashMap<>();
    properties.put(PROPERTY_PARAMETERS, List.of(literalParam));
    properties.put(PROPERTY_COMMAND, "https://api.example.com/features");
    when(task.getProperties()).thenReturn(properties);

    // When
    TaskDto result = service.map(task, application, territory);

    // Then
    assertNotNull(result.getParameters());
    assertTrue(
        result.getParameters().containsKey("outputFormat"), "Literal default should be exposed");
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
    noDefaultParam.put("variable", "featureId");
    noDefaultParam.put("value", null);
    noDefaultParam.put("type", "string");
    noDefaultParam.put("required", true);

    Map<String, Object> properties = new HashMap<>();
    properties.put(PROPERTY_PARAMETERS, List.of(noDefaultParam));
    properties.put(PROPERTY_COMMAND, "https://api.example.com/feature/{featureId}");
    when(task.getProperties()).thenReturn(properties);

    // When
    TaskDto result = service.map(task, application, territory);

    // Then
    assertNotNull(result.getParameters());
    assertTrue(
        result.getParameters().containsKey("featureId"),
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
    providedParam.put("variable", "clientSecret");
    providedParam.put("value", "secret");
    providedParam.put("provided", true);
    providedParam.put("type", "string");
    providedParam.put("required", false);

    Map<String, Object> lockedParam = new HashMap<>();
    lockedParam.put("variable", "appId");
    lockedParam.put("value", "#{APP_ID}");
    lockedParam.put("type", "string");
    lockedParam.put("required", false);
    lockedParam.put("provided", true);

    Map<String, Object> properties = new HashMap<>();
    properties.put(PROPERTY_PARAMETERS, List.of(providedParam, lockedParam));
    properties.put(PROPERTY_COMMAND, "https://api.example.com/data");
    when(task.getProperties()).thenReturn(properties);

    // When
    TaskDto result = service.map(task, application, territory);

    // Then
    assertNull(result.getParameters(), "Should return null when no client-visible parameters");
  }

  @Test
  @DisplayName("map routes through proxy when task has provided parameters")
  void mapRoutesThroughProxyWhenTaskHasProvidedParameters() {
    // Given
    Task task = mock(Task.class);
    Application application = mock(Application.class);
    Territory territory = mock(Territory.class);

    when(task.getId()).thenReturn(42);
    when(application.getId()).thenReturn(1);
    when(territory.getId()).thenReturn(2);

    Map<String, Object> providedParam = new HashMap<>();
    providedParam.put("variable", "apiKey");
    providedParam.put("value", "secret");
    providedParam.put("provided", true);

    Map<String, Object> properties = new HashMap<>();
    properties.put(PROPERTY_PARAMETERS, List.of(providedParam));
    properties.put(PROPERTY_COMMAND, "https://api.example.com/data");
    properties.put(PROPERTY_SCOPE, SCOPE_WEB_API_QUERY);
    when(task.getProperties()).thenReturn(properties);

    // When
    TaskDto result = service.map(task, application, territory);

    // Then
    assertEquals("http://localhost:8080/middleware/proxy/1/2/API/42", result.getUrl());
  }
}
