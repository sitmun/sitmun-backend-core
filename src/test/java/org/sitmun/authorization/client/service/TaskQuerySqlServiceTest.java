package org.sitmun.authorization.client.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.sitmun.domain.DomainConstants.Tasks.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.authorization.client.dto.TaskDto;
import org.sitmun.authorization.client.dto.profile.QueryParameter;
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
    providedParam.put(PARAMETERS_VARIABLE, "apiKey");
    providedParam.put(PARAMETERS_VALUE, "secret123");
    providedParam.put(PARAMETERS_PROVIDED, true);
    providedParam.put(PARAMETERS_TYPE, TYPE_STRING);
    providedParam.put(PARAMETERS_REQUIRED, false);

    Map<String, Object> normalParam = new HashMap<>();
    normalParam.put(PARAMETERS_VARIABLE, "filter");
    normalParam.put(PARAMETERS_VALUE, null);
    normalParam.put(PARAMETERS_TYPE, TYPE_STRING);
    normalParam.put(PARAMETERS_REQUIRED, true);

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
    lockedParam.put(PARAMETERS_VARIABLE, "userId");
    lockedParam.put(PARAMETERS_VALUE, "#{USER_ID}");
    lockedParam.put(PARAMETERS_TYPE, TYPE_STRING);
    lockedParam.put(PARAMETERS_REQUIRED, false);
    // No need for provided flag - #{...} implies backend-provided (defensive)

    Map<String, Object> normalParam = new HashMap<>();
    normalParam.put(PARAMETERS_VARIABLE, "where");
    normalParam.put(PARAMETERS_VALUE, null);
    normalParam.put(PARAMETERS_TYPE, TYPE_STRING);
    normalParam.put(PARAMETERS_REQUIRED, false);

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
    spelParam.put(PARAMETERS_VARIABLE, "userEmail");
    spelParam.put(PARAMETERS_VALUE, "#{USER_EMAIL}");
    spelParam.put(PARAMETERS_TYPE, TYPE_STRING);
    spelParam.put(PARAMETERS_REQUIRED, false);
    // No need for provided flag - #{...} implies backend-provided (defensive)

    Map<String, Object> normalParam = new HashMap<>();
    normalParam.put(PARAMETERS_VARIABLE, "status");
    normalParam.put(PARAMETERS_VALUE, "active");
    normalParam.put(PARAMETERS_TYPE, TYPE_STRING);
    normalParam.put(PARAMETERS_REQUIRED, false);

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
    literalParam.put(PARAMETERS_VARIABLE, "format");
    literalParam.put(PARAMETERS_VALUE, "json");
    literalParam.put(PARAMETERS_TYPE, TYPE_STRING);
    literalParam.put(PARAMETERS_REQUIRED, false);

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
    noDefaultParam.put(PARAMETERS_VARIABLE, "searchTerm");
    noDefaultParam.put(PARAMETERS_VALUE, null);
    noDefaultParam.put(PARAMETERS_TYPE, TYPE_STRING);
    noDefaultParam.put(PARAMETERS_REQUIRED, true);

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
  @DisplayName("map returns empty parameters when all are backend-only")
  void mapReturnsEmptyParametersWhenAllAreBackendOnly() {
    // Given
    Task task = mock(Task.class);
    Application application = mock(Application.class);
    Territory territory = mock(Territory.class);

    when(task.getId()).thenReturn(1);
    when(application.getId()).thenReturn(1);
    when(territory.getId()).thenReturn(1);

    Map<String, Object> providedParam = new HashMap<>();
    providedParam.put(PARAMETERS_VARIABLE, "apiKey");
    providedParam.put(PARAMETERS_VALUE, "secret");
    providedParam.put(PARAMETERS_PROVIDED, true);
    providedParam.put(PARAMETERS_TYPE, TYPE_STRING);
    providedParam.put(PARAMETERS_REQUIRED, false);

    Map<String, Object> lockedParam = new HashMap<>();
    lockedParam.put(PARAMETERS_VARIABLE, "userId");
    lockedParam.put(PARAMETERS_VALUE, "#{USER_ID}");
    lockedParam.put(PARAMETERS_TYPE, TYPE_STRING);
    lockedParam.put(PARAMETERS_REQUIRED, false);
    // No need for provided flag - #{...} implies backend-provided (defensive)

    Map<String, Object> properties = new HashMap<>();
    properties.put(PROPERTY_PARAMETERS, List.of(providedParam, lockedParam));
    when(task.getProperties()).thenReturn(properties);

    // When
    TaskDto result = service.map(task, application, territory);

    // Then
    assertTrue(
        result.getParameters().isEmpty(), "Should not expose backend-only slots to the profile");
  }

  @Test
  @DisplayName("map sets scope to SQL")
  void mapSetsScopeToSql() {
    // Given
    Task task = mock(Task.class);
    Application application = mock(Application.class);
    Territory territory = mock(Territory.class);

    when(task.getId()).thenReturn(1);
    when(application.getId()).thenReturn(1);
    when(territory.getId()).thenReturn(1);
    when(task.getProperties()).thenReturn(null);

    // When
    TaskDto result = service.map(task, application, territory);

    // Then
    assertEquals(SCOPE_SQL, result.getScope(), "Scope should be set to SQL for sql-query tasks");
  }

  @Test
  @DisplayName("map passes through template and query parameter types")
  void mapPassesThroughTemplateAndQueryParameterTypes() {
    Task task = mock(Task.class);
    Application application = mock(Application.class);
    Territory territory = mock(Territory.class);
    when(task.getId()).thenReturn(200);
    when(application.getId()).thenReturn(9);
    when(territory.getId()).thenReturn(8);

    Map<String, Object> templateParam = new HashMap<>();
    templateParam.put(PARAMETERS_VARIABLE, "geom");
    templateParam.put("type", PARAM_TYPE_TEMPLATE);
    templateParam.put(PARAMETERS_REQUIRED, true);

    Map<String, Object> queryParam = new HashMap<>();
    queryParam.put(PARAMETERS_VARIABLE, "bbox");
    queryParam.put("type", PARAM_TYPE_QUERY);
    queryParam.put(PARAMETERS_REQUIRED, false);

    Map<String, Object> properties = new HashMap<>();
    properties.put(PROPERTY_PARAMETERS, List.of(templateParam, queryParam));
    when(task.getProperties()).thenReturn(properties);

    TaskDto result = service.map(task, application, territory);
    assertEquals(PARAM_TYPE_TEMPLATE, ((QueryParameter) result.getParameters().get("geom")).type());
    assertEquals(PARAM_TYPE_QUERY, ((QueryParameter) result.getParameters().get("bbox")).type());
  }

  @Test
  @DisplayName("map defaults missing parameter type to string")
  void mapDefaultsMissingParameterTypeToString() {
    Task task = mock(Task.class);
    Application application = mock(Application.class);
    Territory territory = mock(Territory.class);
    when(task.getId()).thenReturn(201);
    when(application.getId()).thenReturn(1);
    when(territory.getId()).thenReturn(1);

    Map<String, Object> raw = new HashMap<>();
    raw.put(PARAMETERS_VARIABLE, "noExplicitType");
    raw.put(PARAMETERS_REQUIRED, false);

    Map<String, Object> properties = new HashMap<>();
    properties.put(PROPERTY_PARAMETERS, List.of(raw));
    when(task.getProperties()).thenReturn(properties);

    TaskDto result = service.map(task, application, territory);
    assertEquals(
        TYPE_STRING, ((QueryParameter) result.getParameters().get("noExplicitType")).type());
  }
}
