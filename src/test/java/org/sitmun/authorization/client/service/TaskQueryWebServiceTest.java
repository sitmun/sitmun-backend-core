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
    providedParam.put(PARAMETERS_VARIABLE, "apiKey");
    providedParam.put(PARAMETERS_VALUE, "secret123");
    providedParam.put(PARAMETERS_PROVIDED, true);
    providedParam.put(PARAMETERS_TYPE, TYPE_STRING);
    providedParam.put(PARAMETERS_REQUIRED, false);

    Map<String, Object> normalParam = new HashMap<>();
    normalParam.put(PARAMETERS_VARIABLE, "format");
    normalParam.put(PARAMETERS_VALUE, null);
    normalParam.put(PARAMETERS_TYPE, TYPE_STRING);
    normalParam.put(PARAMETERS_REQUIRED, true);

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
    lockedParam.put(PARAMETERS_VARIABLE, "territoryId");
    lockedParam.put(PARAMETERS_VALUE, "#{TERR_ID}");
    lockedParam.put(PARAMETERS_TYPE, TYPE_STRING);
    lockedParam.put(PARAMETERS_REQUIRED, false);
    // No need for provided flag - #{...} implies backend-provided (defensive)

    Map<String, Object> normalParam = new HashMap<>();
    normalParam.put(PARAMETERS_VARIABLE, "query");
    normalParam.put(PARAMETERS_VALUE, null);
    normalParam.put(PARAMETERS_TYPE, TYPE_STRING);
    normalParam.put(PARAMETERS_REQUIRED, false);

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
    spelParam.put(PARAMETERS_VARIABLE, "authToken");
    spelParam.put(PARAMETERS_VALUE, "#{AUTH_TOKEN}");
    spelParam.put(PARAMETERS_TYPE, TYPE_STRING);
    spelParam.put(PARAMETERS_REQUIRED, false);
    // No need for provided flag - #{...} implies backend-provided (defensive)

    Map<String, Object> normalParam = new HashMap<>();
    normalParam.put(PARAMETERS_VARIABLE, "limit");
    normalParam.put(PARAMETERS_VALUE, "100");
    normalParam.put(PARAMETERS_TYPE, TYPE_NUMBER);
    normalParam.put(PARAMETERS_REQUIRED, false);

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
    literalParam.put(PARAMETERS_VARIABLE, "outputFormat");
    literalParam.put(PARAMETERS_VALUE, "geojson");
    literalParam.put(PARAMETERS_TYPE, TYPE_STRING);
    literalParam.put(PARAMETERS_REQUIRED, false);

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
    noDefaultParam.put(PARAMETERS_VARIABLE, "featureId");
    noDefaultParam.put(PARAMETERS_VALUE, null);
    noDefaultParam.put(PARAMETERS_TYPE, TYPE_STRING);
    noDefaultParam.put(PARAMETERS_REQUIRED, true);

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
    providedParam.put(PARAMETERS_VARIABLE, "clientSecret");
    providedParam.put(PARAMETERS_VALUE, "secret");
    providedParam.put(PARAMETERS_PROVIDED, true);
    providedParam.put(PARAMETERS_TYPE, TYPE_STRING);
    providedParam.put(PARAMETERS_REQUIRED, false);

    Map<String, Object> lockedParam = new HashMap<>();
    lockedParam.put(PARAMETERS_VARIABLE, "appId");
    lockedParam.put(PARAMETERS_VALUE, "#{APP_ID}");
    lockedParam.put(PARAMETERS_TYPE, TYPE_STRING);
    lockedParam.put(PARAMETERS_REQUIRED, false);
    lockedParam.put(PARAMETERS_PROVIDED, true);

    Map<String, Object> properties = new HashMap<>();
    properties.put(PROPERTY_PARAMETERS, List.of(providedParam, lockedParam));
    properties.put(PROPERTY_COMMAND, "https://api.example.com/data");
    when(task.getProperties()).thenReturn(properties);

    // When
    TaskDto result = service.map(task, application, territory);

    // Then
    assertTrue(
        result.getParameters().isEmpty(), "Should not expose backend-only slots to the profile");
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
    providedParam.put(PARAMETERS_VARIABLE, "apiKey");
    providedParam.put(PARAMETERS_VALUE, "secret");
    providedParam.put(PARAMETERS_PROVIDED, true);

    Map<String, Object> properties = new HashMap<>();
    properties.put(PROPERTY_PARAMETERS, List.of(providedParam));
    properties.put(PROPERTY_COMMAND, "https://api.example.com/data");
    properties.put(PROPERTY_SCOPE, SCOPE_WEB_API_QUERY);
    when(task.getProperties()).thenReturn(properties);

    // When
    TaskDto result = service.map(task, application, territory);

    // Then: proxied middleware URL — no upward API URL on the client
    assertEquals("http://localhost:8080/middleware/proxy/1/2/API/42", result.getUrl());
    // Provided-secret parameters stay server-side (same contract as
    // mapExcludesProvidedParametersFromClientProfile).
    assertTrue(
        result.getParameters().isEmpty(),
        "No parameter map should leak when every slot is backend-only");
  }

  @Test
  @DisplayName("web-api-query with #{...} in command but no provided parameter is still proxied")
  void webApiQueryWithSystemVariableInCommandIsProxied() {
    // Given
    Task task = mock(Task.class);
    Application application = mock(Application.class);
    Territory territory = mock(Territory.class);

    when(task.getId()).thenReturn(42);
    when(application.getId()).thenReturn(1);
    when(territory.getId()).thenReturn(2);

    Map<String, Object> normalParam = new HashMap<>();
    normalParam.put(PARAMETERS_VARIABLE, "format");
    normalParam.put(PARAMETERS_VALUE, null);
    normalParam.put(PARAMETERS_TYPE, TYPE_STRING);
    normalParam.put(PARAMETERS_REQUIRED, true);

    Map<String, Object> properties = new HashMap<>();
    properties.put(PROPERTY_PARAMETERS, List.of(normalParam));
    properties.put(PROPERTY_COMMAND, "https://api.example.com/data?appId=#{APP_ID}");
    properties.put(PROPERTY_SCOPE, SCOPE_WEB_API_QUERY);
    when(task.getProperties()).thenReturn(properties);

    // When
    TaskDto result = service.map(task, application, territory);

    // Then
    assertEquals(
        "http://localhost:8080/middleware/proxy/1/2/API/42",
        result.getUrl(),
        "web-api-query with #{...} in command should be proxied based on scope alone");
  }

  @Test
  @DisplayName("web-api-query-no-proxy uses direct URL even with no provided parameters")
  void webApiQueryNoProxyUsesDirectUrl() {
    // Given
    Task task = mock(Task.class);
    Application application = mock(Application.class);
    Territory territory = mock(Territory.class);

    when(task.getId()).thenReturn(42);
    when(application.getId()).thenReturn(1);
    when(territory.getId()).thenReturn(2);

    Map<String, Object> normalParam = new HashMap<>();
    normalParam.put(PARAMETERS_VARIABLE, "format");
    normalParam.put(PARAMETERS_VALUE, "json");
    normalParam.put(PARAMETERS_TYPE, TYPE_STRING);
    normalParam.put(PARAMETERS_REQUIRED, false);

    Map<String, Object> properties = new HashMap<>();
    properties.put(PROPERTY_PARAMETERS, List.of(normalParam));
    properties.put(PROPERTY_COMMAND, "https://public-api.example.com/data");
    properties.put(PROPERTY_SCOPE, SCOPE_WEB_API_QUERY_NO_PROXY);
    when(task.getProperties()).thenReturn(properties);

    // When
    TaskDto result = service.map(task, application, territory);

    // Then
    assertEquals(
        "https://public-api.example.com/data",
        result.getUrl(),
        "web-api-query-no-proxy should use direct command URL");
  }

  @Test
  @DisplayName(
      "map omits template parameters for proxied web-api-query (middleware URL only on client)")
  void mapOmitsTemplateParametersForProxiedWebApiButKeepsQuery() {
    Task task = mock(Task.class);
    Application application = mock(Application.class);
    Territory territory = mock(Territory.class);
    when(task.getId()).thenReturn(99);
    when(application.getId()).thenReturn(4);
    when(territory.getId()).thenReturn(5);

    Map<String, Object> templateParam = new HashMap<>();
    templateParam.put(PARAMETERS_VARIABLE, "codigo");
    templateParam.put("type", PARAM_TYPE_TEMPLATE);
    templateParam.put(PARAMETERS_REQUIRED, true);

    Map<String, Object> queryParam = new HashMap<>();
    queryParam.put(PARAMETERS_VARIABLE, "limit");
    queryParam.put("type", PARAM_TYPE_QUERY);
    queryParam.put(PARAMETERS_REQUIRED, false);

    Map<String, Object> properties = new HashMap<>();
    properties.put(PROPERTY_PARAMETERS, List.of(templateParam, queryParam));
    properties.put(PROPERTY_SCOPE, SCOPE_WEB_API_QUERY);
    properties.put(PROPERTY_COMMAND, "https://api.example.com/ruta/{codigo}");
    when(task.getProperties()).thenReturn(properties);

    TaskDto result = service.map(task, application, territory);

    assertNotNull(result.getParameters());
    assertFalse(
        result.getParameters().containsKey("codigo"),
        "URI template placeholder should not appear in proxied API profile");
    QueryParameter limit = (QueryParameter) result.getParameters().get("limit");
    assertEquals(PARAM_TYPE_QUERY, limit.type());
    assertFalse(limit.required());
  }

  @Test
  @DisplayName("map defaults missing parameter type to string (proxied web-api-query)")
  void mapDefaultsMissingParameterTypeToStringForProxiedWebApi() {
    Task task = mock(Task.class);
    Application application = mock(Application.class);
    Territory territory = mock(Territory.class);
    when(task.getId()).thenReturn(101);
    when(application.getId()).thenReturn(1);
    when(territory.getId()).thenReturn(2);

    Map<String, Object> raw = new HashMap<>();
    raw.put(PARAMETERS_VARIABLE, "orphanType");
    raw.put(PARAMETERS_REQUIRED, true);

    Map<String, Object> properties = new HashMap<>();
    properties.put(PROPERTY_PARAMETERS, List.of(raw));
    properties.put(PROPERTY_SCOPE, SCOPE_WEB_API_QUERY);
    properties.put(PROPERTY_COMMAND, "https://api.example.com/x");
    when(task.getProperties()).thenReturn(properties);

    TaskDto result = service.map(task, application, territory);
    QueryParameter dto = (QueryParameter) result.getParameters().get("orphanType");
    assertEquals(TYPE_STRING, dto.type());
  }

  @Test
  @DisplayName(
      "Direct web-api-query (no-proxy): only template + query parameters reach the client; "
          + "provided and locked parameters are filtered out")
  void directWebApiQueryExposesTemplateAndQuerySlots() {
    Task task = mock(Task.class);
    Application application = mock(Application.class);
    Territory territory = mock(Territory.class);
    when(task.getId()).thenReturn(102);
    when(application.getId()).thenReturn(1);
    when(territory.getId()).thenReturn(2);

    // Persisted tasks with scope web-api-query-no-proxy cannot put #{...} in command (validated by
    // TaskQueryValidator — direct execution). URL may still carry client template placeholders such
    // as {id}; pageSize uses PARAMETERS_TYPE query for a client-built query fragment.
    String upstreamCommandUrl = "https://catalog.example.com/catalog/items/{id}";

    Map<String, Object> templateParam = new HashMap<>();
    templateParam.put(PARAMETERS_VARIABLE, "id");
    templateParam.put(PARAMETERS_TYPE, PARAM_TYPE_TEMPLATE);
    templateParam.put(PARAMETERS_REQUIRED, true);

    Map<String, Object> queryParam = new HashMap<>();
    queryParam.put(PARAMETERS_VARIABLE, "pageSize");
    queryParam.put(PARAMETERS_TYPE, PARAM_TYPE_QUERY);
    queryParam.put(PARAMETERS_REQUIRED, false);

    // TaskQueryValidator rejects provided params and #{...} in parameter values for this scope;
    // list them anyway so the mapper's client-profile filter is explicit (defense in depth).
    Map<String, Object> providedParam = new HashMap<>();
    providedParam.put(PARAMETERS_VARIABLE, "apiKey");
    providedParam.put(PARAMETERS_VALUE, "secret");
    providedParam.put(PARAMETERS_PROVIDED, true);
    providedParam.put(PARAMETERS_TYPE, TYPE_STRING);
    providedParam.put(PARAMETERS_REQUIRED, false);

    Map<String, Object> lockedParam = new HashMap<>();
    lockedParam.put(PARAMETERS_VARIABLE, "tenantId");
    lockedParam.put(PARAMETERS_VALUE, "#{TENANT_ID}");
    lockedParam.put(PARAMETERS_TYPE, TYPE_STRING);
    lockedParam.put(PARAMETERS_REQUIRED, false);

    Map<String, Object> properties = new HashMap<>();
    properties.put(
        PROPERTY_PARAMETERS, List.of(templateParam, queryParam, providedParam, lockedParam));
    properties.put(PROPERTY_SCOPE, SCOPE_WEB_API_QUERY_NO_PROXY);
    properties.put(PROPERTY_COMMAND, upstreamCommandUrl);
    when(task.getProperties()).thenReturn(properties);

    TaskDto result = service.map(task, application, territory);

    assertEquals(SCOPE_URL, result.getScope());
    assertEquals(upstreamCommandUrl, result.getUrl());

    Map<String, Object> published = result.getParameters();
    assertNotNull(published);
    assertFalse(published.containsKey("apiKey"), "provided=true must not surface");
    assertFalse(published.containsKey("tenantId"), "locked #{...} must not surface");
    assertEquals(2, published.size(), "Only template + query slots for client-side assembly");

    QueryParameter templateSlot = (QueryParameter) published.get("id");
    assertEquals(PARAM_TYPE_TEMPLATE, templateSlot.type());
    assertTrue(templateSlot.required());

    QueryParameter querySlot = (QueryParameter) published.get("pageSize");
    assertEquals(PARAM_TYPE_QUERY, querySlot.type());
    assertFalse(querySlot.required());
  }
}
