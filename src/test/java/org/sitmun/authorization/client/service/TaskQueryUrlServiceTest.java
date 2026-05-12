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
import org.sitmun.authorization.client.dto.profile.FeatureInfoParameter;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.task.Task;
import org.sitmun.domain.task.parameter.TaskParameterProcessor;
import org.sitmun.domain.task.type.TaskType;
import org.sitmun.domain.territory.Territory;
import org.sitmun.infrastructure.variables.SystemVariableResolver;

@DisplayName("TaskQueryUrlService unit tests")
class TaskQueryUrlServiceTest {

  private TaskQueryUrlService service;

  @BeforeEach
  void setUp() {
    // Create a real TaskParameterProcessor with mock SystemVariableResolver
    SystemVariableResolver mockResolver = mock(SystemVariableResolver.class);
    when(mockResolver.resolve(anyString(), any())).thenAnswer(inv -> inv.getArgument(0));

    TaskParameterProcessor processor = new TaskParameterProcessor(mockResolver);

    service = new TaskQueryUrlService(processor);
  }

  @Test
  @DisplayName("accept returns true for external-link query task")
  void acceptReturnsTrueForExternalLinkQueryTask() {
    // Given
    Task task = mock(Task.class);
    when(task.getId()).thenReturn(1);
    when(task.getConnection()).thenReturn(null);
    when(task.getCartography()).thenReturn(null);

    Map<String, Object> properties = new HashMap<>();
    properties.put(PROPERTY_SCOPE, SCOPE_URL_QUERY);
    properties.put(PROPERTY_COMMAND, "https://www.example.com/document.pdf");
    when(task.getProperties()).thenReturn(properties);

    TaskType taskType = mock(TaskType.class);
    when(taskType.getId()).thenReturn(TASK_TYPE_ID_QUERY);
    when(task.getType()).thenReturn(taskType);

    // When
    boolean result = service.accept(task);

    // Then
    assertTrue(result);
  }

  @Test
  @DisplayName("accept returns true for legacy URL query task scope")
  void acceptReturnsTrueForLegacyUrlQueryTask() {
    // Given
    Task task = mock(Task.class);
    when(task.getId()).thenReturn(1);
    when(task.getConnection()).thenReturn(null);
    when(task.getCartography()).thenReturn(null);

    Map<String, Object> properties = new HashMap<>();
    properties.put(PROPERTY_SCOPE, SCOPE_URL);
    properties.put(PROPERTY_COMMAND, "https://www.example.com/document.pdf");
    when(task.getProperties()).thenReturn(properties);

    TaskType taskType = mock(TaskType.class);
    when(taskType.getId()).thenReturn(TASK_TYPE_ID_QUERY);
    when(task.getType()).thenReturn(taskType);

    // When
    boolean result = service.accept(task);

    // Then
    assertTrue(result);
  }

  @Test
  @DisplayName("accept returns false for non-external-link query task")
  void acceptReturnsFalseForNonExternalLinkTask() {
    // Given
    Task task = mock(Task.class);
    when(task.getId()).thenReturn(1);

    Map<String, Object> properties = new HashMap<>();
    properties.put(PROPERTY_SCOPE, SCOPE_WEB_API_QUERY);
    when(task.getProperties()).thenReturn(properties);

    TaskType taskType = mock(TaskType.class);
    when(taskType.getId()).thenReturn(TASK_TYPE_ID_QUERY);
    when(task.getType()).thenReturn(taskType);

    // When
    boolean result = service.accept(task);

    // Then
    assertFalse(result);
  }

  @Test
  @DisplayName("map builds TaskDto with URL scope and command URL")
  void mapBuildsTaskDtoWithUrlScopeAndCommand() {
    // Given
    Task task = mock(Task.class);
    Application application = mock(Application.class);
    Territory territory = mock(Territory.class);

    when(task.getId()).thenReturn(42);
    when(application.getId()).thenReturn(1);
    when(territory.getId()).thenReturn(2);

    Map<String, Object> properties = new HashMap<>();
    properties.put(PROPERTY_SCOPE, SCOPE_URL_QUERY);
    properties.put(PROPERTY_COMMAND, "https://www.example.com/info.html");
    when(task.getProperties()).thenReturn(properties);

    // When
    TaskDto result = service.map(task, application, territory);

    // Then
    assertNotNull(result);
    assertEquals(TASK_PROFILE_ID_PREFIX + "42", result.getId());
    assertEquals("https://www.example.com/info.html", result.getUrl());
    assertEquals(SCOPE_URL, result.getScope());
    assertNull(result.getMimeType());
    assertNull(result.getFilename());
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
    properties.put(PROPERTY_COMMAND, "https://www.example.com/data");
    properties.put(PROPERTY_SCOPE, SCOPE_URL_QUERY);
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
  @DisplayName("map excludes locked parameters (#{...}) from client profile")
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

    Map<String, Object> normalParam = new HashMap<>();
    normalParam.put(PARAMETERS_VARIABLE, "page");
    normalParam.put(PARAMETERS_VALUE, "1");
    normalParam.put(PARAMETERS_TYPE, TYPE_STRING);
    normalParam.put(PARAMETERS_REQUIRED, false);

    Map<String, Object> properties = new HashMap<>();
    properties.put(PROPERTY_PARAMETERS, List.of(lockedParam, normalParam));
    properties.put(PROPERTY_COMMAND, "https://www.example.com/catalog");
    properties.put(PROPERTY_SCOPE, SCOPE_URL_QUERY);
    when(task.getProperties()).thenReturn(properties);

    // When
    TaskDto result = service.map(task, application, territory);

    // Then
    assertNotNull(result.getParameters());
    assertFalse(
        result.getParameters().containsKey("territoryId"),
        "Locked parameter should not be exposed");
    assertTrue(result.getParameters().containsKey("page"), "Normal parameter should be exposed");
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
    providedParam.put(PARAMETERS_VARIABLE, "secret");
    providedParam.put(PARAMETERS_VALUE, "value");
    providedParam.put(PARAMETERS_PROVIDED, true);
    providedParam.put(PARAMETERS_TYPE, TYPE_STRING);
    providedParam.put(PARAMETERS_REQUIRED, false);

    Map<String, Object> properties = new HashMap<>();
    properties.put(PROPERTY_PARAMETERS, List.of(providedParam));
    properties.put(PROPERTY_COMMAND, "https://www.example.com/data");
    properties.put(PROPERTY_SCOPE, SCOPE_URL_QUERY);
    when(task.getProperties()).thenReturn(properties);

    // When
    TaskDto result = service.map(task, application, territory);

    // Then
    assertTrue(
        result.getParameters().isEmpty(), "Should not expose backend-only slots to the profile");
  }

  @Test
  @DisplayName("map handles null properties gracefully")
  void mapHandlesNullPropertiesGracefully() {
    // Given
    Task task = mock(Task.class);
    Application application = mock(Application.class);
    Territory territory = mock(Territory.class);

    when(task.getId()).thenReturn(42);
    when(task.getProperties()).thenReturn(null);

    // When
    TaskDto result = service.map(task, application, territory);

    // Then
    assertNotNull(result);
    assertEquals(TASK_PROFILE_ID_PREFIX + "42", result.getId());
    assertNull(result.getUrl());
    assertEquals(SCOPE_URL, result.getScope());
  }

  @Test
  @DisplayName("map exposes template and query types in viewer parameter DTO shape")
  void mapExposesTemplateAndQueryTypesInViewerParameterShape() {
    Task task = mock(Task.class);
    Application application = mock(Application.class);
    Territory territory = mock(Territory.class);

    when(task.getId()).thenReturn(55);
    when(application.getId()).thenReturn(1);
    when(territory.getId()).thenReturn(1);

    Map<String, Object> templateParam = new HashMap<>();
    templateParam.put(PARAMETERS_VARIABLE, "codigo");
    templateParam.put(PARAMETERS_LABEL, "Codi");
    templateParam.put(PARAMETERS_TYPE, PARAM_TYPE_TEMPLATE);
    templateParam.put(PARAMETERS_REQUIRED, true);
    templateParam.put(PARAMETERS_VALUE, null);

    Map<String, Object> queryParam = new HashMap<>();
    queryParam.put(PARAMETERS_VARIABLE, "limit");
    queryParam.put(PARAMETERS_LABEL, "Límit");
    queryParam.put(PARAMETERS_TYPE, PARAM_TYPE_QUERY);
    queryParam.put(PARAMETERS_REQUIRED, false);
    queryParam.put(PARAMETERS_VALUE, null);

    Map<String, Object> properties = new HashMap<>();
    properties.put(PROPERTY_PARAMETERS, List.of(templateParam, queryParam));
    properties.put(PROPERTY_COMMAND, "https://www.example.com/stop/{codigo}");
    properties.put(PROPERTY_SCOPE, SCOPE_URL_QUERY);
    when(task.getProperties()).thenReturn(properties);

    TaskDto result = service.map(task, application, territory);

    FeatureInfoParameter codigo = (FeatureInfoParameter) result.getParameters().get("codigo");
    FeatureInfoParameter limit = (FeatureInfoParameter) result.getParameters().get("limit");

    assertEquals("codigo", codigo.name());
    assertEquals("codigo", codigo.label());
    assertEquals(PARAM_TYPE_TEMPLATE, codigo.type());
    assertEquals(Boolean.TRUE, codigo.required());

    assertEquals("limit", limit.name());
    assertEquals("limit", limit.label());
    assertEquals(PARAM_TYPE_QUERY, limit.type());
    assertNotEquals(Boolean.TRUE, limit.required());
  }

  @Test
  @DisplayName("map omits type in viewer DTO when storage has no parameter type")
  void mapOmitsTypeWhenStorageHasNoParameterType() {
    Task task = mock(Task.class);
    Application application = mock(Application.class);
    Territory territory = mock(Territory.class);

    when(task.getId()).thenReturn(56);
    when(application.getId()).thenReturn(1);
    when(territory.getId()).thenReturn(1);

    Map<String, Object> raw = new HashMap<>();
    raw.put(PARAMETERS_VARIABLE, "plain");
    raw.put(PARAMETERS_LABEL, "Plain");
    raw.put(PARAMETERS_REQUIRED, true);

    Map<String, Object> properties = new HashMap<>();
    properties.put(PROPERTY_PARAMETERS, List.of(raw));
    properties.put(PROPERTY_COMMAND, "https://www.example.com/x");
    properties.put(PROPERTY_SCOPE, SCOPE_URL_QUERY);
    when(task.getProperties()).thenReturn(properties);

    TaskDto result = service.map(task, application, territory);
    FeatureInfoParameter plain = (FeatureInfoParameter) result.getParameters().get("plain");
    assertNull(plain.type());
    assertEquals(Boolean.TRUE, plain.required());
  }
}
