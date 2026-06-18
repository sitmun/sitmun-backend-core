package org.sitmun.authorization.client.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.sitmun.domain.DomainConstants.Tasks.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.authorization.client.dto.TaskDto;
import org.sitmun.authorization.client.dto.profile.FeatureInfoParameter;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.cartography.Cartography;
import org.sitmun.domain.task.MoreInfoTaskResolver;
import org.sitmun.domain.task.Task;
import org.sitmun.domain.task.parameter.TaskParameterProcessor;
import org.sitmun.domain.task.type.TaskType;
import org.sitmun.domain.task.ui.TaskUI;
import org.sitmun.domain.territory.Territory;
import org.sitmun.infrastructure.variables.SystemVariableResolver;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("TaskMoreInfoService baseline tests")
class TaskMoreInfoServiceTest {

  private TaskMoreInfoService service;
  private MoreInfoTaskResolver moreInfoTaskResolver;

  @BeforeEach
  void setUp() {
    moreInfoTaskResolver = mock(MoreInfoTaskResolver.class);
    SystemVariableResolver mockSystemVariableResolver = mock(SystemVariableResolver.class);
    TaskParameterProcessor taskParameterProcessor =
        new TaskParameterProcessor(mockSystemVariableResolver);
    service = new TaskMoreInfoService(moreInfoTaskResolver, taskParameterProcessor);
    ReflectionTestUtils.setField(service, "proxyUrl", "http://localhost:8080/middleware");
  }

  @Test
  @DisplayName("accept returns true for More Info task type id")
  void acceptReturnsTrueForMoreInfoTask() {
    // Given
    Task task = mock(Task.class);
    TaskType taskType = mock(TaskType.class);
    when(taskType.getId()).thenReturn(TASK_TYPE_ID_MORE_INFO);
    when(task.getType()).thenReturn(taskType);

    // When
    boolean result = service.accept(task);

    // Then
    assertTrue(result);
  }

  @Test
  @DisplayName("accept returns true for moreInfoAdvanced task")
  void acceptReturnsTrueForMoreInfoAdvancedTask() {
    Task task = mock(Task.class);
    TaskType taskType = mock(TaskType.class);
    when(taskType.getId()).thenReturn(TASK_TYPE_ID_MORE_INFO_ADVANCED);
    when(task.getType()).thenReturn(taskType);

    assertTrue(service.accept(task));
  }

  @Test
  @DisplayName("map exposes MIA client contract without requiring a ui-control")
  void mapExposesMoreInfoAdvancedClientContract() {
    Task task = mock(Task.class);
    TaskType taskType = mock(TaskType.class);
    Cartography cartography = mock(Cartography.class);

    when(task.getId()).thenReturn(16);
    when(task.getName()).thenReturn("MIA parent");
    when(taskType.getId()).thenReturn(TASK_TYPE_ID_MORE_INFO_ADVANCED);
    when(task.getType()).thenReturn(taskType);
    when(task.getUi()).thenReturn(null);
    when(cartography.getId()).thenReturn(12);
    when(task.getCartography()).thenReturn(cartography);
    when(task.getProperties())
        .thenReturn(
            Map.of(
                "advancedTaskKind",
                "parent",
                "parentLayout",
                "scroll",
                "childTaskOrderIds",
                List.of(101, 102),
                "moreInfoAdvanced",
                true));
    when(moreInfoTaskResolver.findRelatedQueryTask(task)).thenReturn(java.util.Optional.empty());

    TaskDto result = service.map(task, mock(Application.class), mock(Territory.class));

    assertEquals(TASK_TYPE_ID_MORE_INFO_ADVANCED, result.getTypeId());
    assertNull(result.getUiControl());
    assertEquals("12", result.getCartographyId());
    assertEquals(PROFILE_LAYER_ID_PREFIX + "12", result.getLayer());
    assertEquals("parent", result.getParameters().get("advancedTaskKind"));
    assertEquals(true, result.getParameters().get("moreInfoAdvanced"));
    assertEquals("scroll", result.getParameters().get("visualizationMode"));
    assertEquals(List.of(101, 102), result.getParameters().get("childTaskOrderIds"));
  }

  @Test
  @DisplayName("accept returns false for null task type")
  void acceptReturnsFalseForNullTaskType() {
    // Given
    Task task = mock(Task.class);
    when(task.getType()).thenReturn(null);

    // When
    boolean result = service.accept(task);

    // Then
    assertFalse(result);
  }

  @Test
  @DisplayName("accept returns false when task type id is unset")
  void acceptReturnsFalseWhenTaskTypeIdIsUnset() {
    // Given
    Task task = mock(Task.class);
    TaskType taskType = mock(TaskType.class);
    when(task.getType()).thenReturn(taskType);

    // When
    boolean result = service.accept(task);

    // Then
    assertFalse(result);
  }

  @Test
  @DisplayName("accept ignores task type title")
  void acceptIgnoresTaskTypeTitle() {
    // Given
    Task task = mock(Task.class);
    TaskType taskType = mock(TaskType.class);
    when(taskType.getId()).thenReturn(TASK_TYPE_ID_MORE_INFO);
    when(taskType.getTitle()).thenReturn("Some Title");
    when(task.getType()).thenReturn(taskType);

    // When
    boolean result = service.accept(task);

    // Then
    assertTrue(result);
    verify(taskType, never()).getTitle();
  }

  @Test
  @DisplayName("accept returns false for other task type")
  void acceptReturnsFalseForOtherTaskType() {
    // Given
    Task task = mock(Task.class);
    TaskType taskType = mock(TaskType.class);
    when(taskType.getId()).thenReturn(TASK_TYPE_ID_QUERY);
    when(task.getType()).thenReturn(taskType);

    // When
    boolean result = service.accept(task);

    // Then
    assertFalse(result);
  }

  @Test
  @DisplayName("map uses linked query task execution while keeping more-info parameters")
  void mapUsesLinkedQueryTaskExecutionWhileKeepingMoreInfoParameters() {
    Task task = mock(Task.class);
    Task relatedQueryTask = mock(Task.class);
    Application application = mock(Application.class);
    Territory territory = mock(Territory.class);

    when(task.getId()).thenReturn(42);
    when(task.getName()).thenReturn("More info");
    when(task.getUi()).thenReturn(null);
    when(task.getCartography()).thenReturn(null);

    Map<String, Object> ownParameter = new HashMap<>();
    ownParameter.put("label", "docId");
    ownParameter.put("value", "ID");
    when(task.getProperties()).thenReturn(Map.of(PROPERTY_PARAMETERS, List.of(ownParameter)));

    when(relatedQueryTask.getProperties())
        .thenReturn(
            Map.of(
                PROPERTY_SCOPE,
                SCOPE_WEB_API_QUERY,
                PROPERTY_COMMAND,
                "https://api.example.com/info/{docId}"));

    when(application.getId()).thenReturn(1);
    when(territory.getId()).thenReturn(2);

    when(moreInfoTaskResolver.findRelatedQueryTask(task))
        .thenReturn(java.util.Optional.of(relatedQueryTask));

    TaskDto result = service.map(task, application, territory);

    assertNotNull(result);
    assertEquals(SCOPE_API, result.getScope());
    assertEquals("http://localhost:8080/middleware/proxy/1/2/API/42", result.getUrl());
    assertNotNull(result.getParameters());
    assertTrue(result.getParameters().containsKey("docId"));
  }

  @Test
  @DisplayName("map resolves URL-scope query task linked via relation: url is set to command")
  void mapResolvesUrlQueryLinkedTask() {
    Task task = mock(Task.class);
    Task relatedUrlQueryTask = mock(Task.class);
    Application application = mock(Application.class);
    Territory territory = mock(Territory.class);

    when(task.getId()).thenReturn(55);
    when(task.getName()).thenReturn("More info URL");
    when(task.getUi()).thenReturn(null);
    when(task.getCartography()).thenReturn(null);
    when(task.getProperties()).thenReturn(Map.of(PROPERTY_PARAMETERS, List.of()));

    when(relatedUrlQueryTask.getProperties())
        .thenReturn(
            Map.of(
                PROPERTY_SCOPE,
                SCOPE_URL,
                PROPERTY_COMMAND,
                "https://external.example.com/doc?id={code}"));

    when(application.getId()).thenReturn(1);
    when(territory.getId()).thenReturn(2);

    when(moreInfoTaskResolver.findRelatedQueryTask(task))
        .thenReturn(java.util.Optional.of(relatedUrlQueryTask));

    TaskDto result = service.map(task, application, territory);

    assertNotNull(result);
    assertEquals(SCOPE_URL, result.getScope());
    assertEquals("https://external.example.com/doc?id={code}", result.getUrl());
    assertNull(result.getCommand()); // Command is never exposed (only url field)
  }

  @Test
  @DisplayName("map returns TaskDto with all fields mapped")
  void mapReturnsTaskDtoWithAllFieldsMapped() {
    // Given
    Task task = mock(Task.class);
    TaskUI taskUI = mock(TaskUI.class);
    Application application = mock(Application.class);
    Territory territory = mock(Territory.class);
    Cartography cartography = mock(Cartography.class);

    when(task.getId()).thenReturn(42);
    when(task.getName()).thenReturn("Test Task");
    when(task.getUi()).thenReturn(taskUI);
    when(taskUI.getName()).thenReturn("infoControl");
    when(taskUI.getType()).thenReturn("info");
    when(task.getCartography()).thenReturn(cartography);
    when(cartography.getId()).thenReturn(10);

    Map<String, Object> param1 = new HashMap<>();
    param1.put("label", "city");
    param1.put("value", "Barcelona");

    Map<String, Object> properties = new HashMap<>();
    properties.put(PROPERTY_PARAMETERS, List.of(param1));
    properties.put(PROPERTY_SCOPE, SCOPE_API);
    properties.put(PROPERTY_COMMAND, "https://api.example.com/info");

    when(task.getProperties()).thenReturn(properties);
    when(application.getId()).thenReturn(1);
    when(territory.getId()).thenReturn(2);

    // When
    TaskDto result = service.map(task, application, territory);

    // Then
    assertNotNull(result);
    assertEquals("task/42", result.getId());
    assertEquals("Test Task", result.getName());
    assertEquals("infoControl", result.getUiControl());
    assertEquals("info", result.getType());
    assertEquals("10", result.getCartographyId());
    assertEquals(PROFILE_LAYER_ID_PREFIX + "10", result.getLayer());
    assertEquals(SCOPE_API, result.getScope());
    assertNull(result.getCommand()); // Command is always null for more-info tasks (security)
    assertEquals("http://localhost:8080/middleware/proxy/1/2/API/42", result.getUrl());
  }

  @Test
  @DisplayName("map builds URL when scope and id exist")
  void mapBuildsUrlWhenScopeAndIdExist() {
    // Given
    Task task = mock(Task.class);
    Application application = mock(Application.class);
    Territory territory = mock(Territory.class);

    when(task.getId()).thenReturn(100);
    when(task.getName()).thenReturn("Test");
    when(task.getUi()).thenReturn(null);
    when(task.getCartography()).thenReturn(null);

    Map<String, Object> properties = new HashMap<>();
    properties.put(PROPERTY_SCOPE, "WMS");
    when(task.getProperties()).thenReturn(properties);

    when(application.getId()).thenReturn(5);
    when(territory.getId()).thenReturn(7);

    // When
    TaskDto result = service.map(task, application, territory);

    // Then
    assertEquals("http://localhost:8080/middleware/proxy/5/7/WMS/100", result.getUrl());
  }

  @Test
  @DisplayName("map leaves URL null when scope is missing")
  void mapLeavesUrlNullWhenScopeIsMissing() {
    // Given
    Task task = mock(Task.class);
    Application application = mock(Application.class);
    Territory territory = mock(Territory.class);

    when(task.getId()).thenReturn(100);
    when(task.getName()).thenReturn("Test");
    when(task.getProperties()).thenReturn(new HashMap<>());

    // When
    TaskDto result = service.map(task, application, territory);

    // Then
    assertNull(result.getUrl());
  }

  @Test
  @DisplayName("map leaves URL null when scope is empty string")
  void mapLeavesUrlNullWhenScopeIsEmpty() {
    // Given
    Task task = mock(Task.class);
    Application application = mock(Application.class);
    Territory territory = mock(Territory.class);

    when(task.getId()).thenReturn(100);
    when(task.getName()).thenReturn("Test");

    Map<String, Object> properties = new HashMap<>();
    properties.put(PROPERTY_SCOPE, "");
    when(task.getProperties()).thenReturn(properties);

    // When
    TaskDto result = service.map(task, application, territory);

    // Then
    assertNull(result.getUrl());
  }

  @Test
  @DisplayName("map leaves URL null when scope is blank")
  void mapLeavesUrlNullWhenScopeIsBlank() {
    // Given
    Task task = mock(Task.class);
    Application application = mock(Application.class);
    Territory territory = mock(Territory.class);

    when(task.getId()).thenReturn(100);
    when(task.getName()).thenReturn("Test");

    Map<String, Object> properties = new HashMap<>();
    properties.put(PROPERTY_SCOPE, "   ");
    when(task.getProperties()).thenReturn(properties);

    // When
    TaskDto result = service.map(task, application, territory);

    // Then
    assertNull(result.getUrl());
  }

  @Test
  @DisplayName("map leaves URL null when task id is null")
  void mapLeavesUrlNullWhenTaskIdIsNull() {
    // Given
    Task task = mock(Task.class);
    Application application = mock(Application.class);
    Territory territory = mock(Territory.class);

    when(task.getId()).thenReturn(null);
    when(task.getName()).thenReturn("Test");

    Map<String, Object> properties = new HashMap<>();
    properties.put(PROPERTY_SCOPE, SCOPE_SQL_QUERY);
    when(task.getProperties()).thenReturn(properties);

    // When
    TaskDto result = service.map(task, application, territory);

    // Then
    assertNull(result.getUrl());
  }

  @Test
  @DisplayName("map builds URL even when proxy URL is empty (URL field shows relative path)")
  void mapBuildsRelativeUrlWhenProxyUrlIsEmpty() {
    // Given
    ReflectionTestUtils.setField(service, "proxyUrl", "");

    Task task = mock(Task.class);
    Application application = mock(Application.class);
    Territory territory = mock(Territory.class);

    when(task.getId()).thenReturn(100);
    when(task.getName()).thenReturn("Test");

    Map<String, Object> properties = new HashMap<>();
    properties.put(PROPERTY_SCOPE, SCOPE_SQL_QUERY);
    when(task.getProperties()).thenReturn(properties);
    when(application.getId()).thenReturn(0);
    when(territory.getId()).thenReturn(0);

    // When
    TaskDto result = service.map(task, application, territory);

    // Then
    assertEquals("/proxy/0/0/SQL/100", result.getUrl());
  }

  @Test
  @DisplayName(
      "map maps parameters entries by variable (with backward compatibility for label-only legacy data)")
  void mapMapsParametersEntriesByVariable() {
    // Given
    Task task = mock(Task.class);
    Application application = mock(Application.class);
    Territory territory = mock(Territory.class);

    when(task.getId()).thenReturn(1);
    when(task.getName()).thenReturn("Test");

    Map<String, Object> param1 = new HashMap<>();
    param1.put("label", "city");
    param1.put("value", "Barcelona");

    Map<String, Object> param2 = new HashMap<>();
    param2.put("label", "country");
    param2.put("value", "Spain");

    Map<String, Object> properties = new HashMap<>();
    properties.put(PROPERTY_PARAMETERS, List.of(param1, param2));
    when(task.getProperties()).thenReturn(properties);

    // When
    TaskDto result = service.map(task, application, territory);

    // Then
    assertNotNull(result.getParameters());
    assertEquals(2, result.getParameters().size());

    // Verify the map is keyed by variable (fallback to label for legacy data)
    assertTrue(result.getParameters().containsKey("city"));
    assertTrue(result.getParameters().containsKey("country"));

    // Verify enriched parameter structure includes name field
    FeatureInfoParameter cityParam = (FeatureInfoParameter) result.getParameters().get("city");
    assertEquals("city", cityParam.label());
    assertEquals("Barcelona", cityParam.value());
    assertEquals("city", cityParam.name()); // New standard field

    FeatureInfoParameter countryParam =
        (FeatureInfoParameter) result.getParameters().get("country");
    assertEquals("country", countryParam.label());
    assertEquals("Spain", countryParam.value());
    assertEquals("country", countryParam.name()); // New standard field
  }

  @Test
  @DisplayName("map returns null parameters when no matching parameters")
  void mapReturnsNullParametersWhenNoMatchingParameters() {
    // Given
    Task task = mock(Task.class);
    Application application = mock(Application.class);
    Territory territory = mock(Territory.class);

    when(task.getId()).thenReturn(1);
    when(task.getName()).thenReturn("Test");

    Map<String, Object> properties = new HashMap<>();
    properties.put(PROPERTY_PARAMETERS, Collections.emptyList());
    when(task.getProperties()).thenReturn(properties);

    // When
    TaskDto result = service.map(task, application, territory);

    // Then
    assertNull(result.getParameters());
  }

  @Test
  @DisplayName("map returns empty map parameters when properties is null")
  void mapReturnsEmptyMapParametersWhenPropertiesIsNull() {
    // Given
    Task task = mock(Task.class);
    Application application = mock(Application.class);
    Territory territory = mock(Territory.class);

    when(task.getId()).thenReturn(1);
    when(task.getName()).thenReturn("Test");
    when(task.getProperties()).thenReturn(null);

    // When
    TaskDto result = service.map(task, application, territory);

    // Then
    assertNotNull(result.getParameters());
    assertTrue(result.getParameters().isEmpty());
  }

  @Test
  @DisplayName("map handles parameters without label key")
  void mapHandlesParametersWithoutLabelKey() {
    // Given
    Task task = mock(Task.class);
    Application application = mock(Application.class);
    Territory territory = mock(Territory.class);

    when(task.getId()).thenReturn(1);
    when(task.getName()).thenReturn("Test");

    Map<String, Object> param1 = new HashMap<>();
    param1.put("value", "Barcelona");
    // No "label" key

    Map<String, Object> properties = new HashMap<>();
    properties.put(PROPERTY_PARAMETERS, List.of(param1));
    when(task.getProperties()).thenReturn(properties);

    // When
    TaskDto result = service.map(task, application, territory);

    // Then
    assertNull(result.getParameters());
  }

  @Test
  @DisplayName("map handles null cartography")
  void mapHandlesNullCartography() {
    // Given
    Task task = mock(Task.class);
    Application application = mock(Application.class);
    Territory territory = mock(Territory.class);

    when(task.getId()).thenReturn(1);
    when(task.getName()).thenReturn("Test");
    when(task.getCartography()).thenReturn(null);
    when(task.getProperties()).thenReturn(new HashMap<>());

    // When
    TaskDto result = service.map(task, application, territory);

    // Then
    assertNull(result.getCartographyId());
    assertNull(result.getLayer());
  }

  @Test
  @DisplayName("map handles null UI")
  void mapHandlesNullUi() {
    // Given
    Task task = mock(Task.class);
    Application application = mock(Application.class);
    Territory territory = mock(Territory.class);

    when(task.getId()).thenReturn(1);
    when(task.getName()).thenReturn("Test");
    when(task.getUi()).thenReturn(null);
    when(task.getProperties()).thenReturn(new HashMap<>());

    // When
    TaskDto result = service.map(task, application, territory);

    // Then
    assertNull(result.getUiControl());
    assertNull(result.getType());
  }

  @Test
  @DisplayName("map handles null command in properties")
  void mapHandlesNullCommandInProperties() {
    // Given
    Task task = mock(Task.class);
    Application application = mock(Application.class);
    Territory territory = mock(Territory.class);

    when(task.getId()).thenReturn(1);
    when(task.getName()).thenReturn("Test");

    Map<String, Object> properties = new HashMap<>();
    properties.put(PROPERTY_COMMAND, null);
    when(task.getProperties()).thenReturn(properties);

    // When
    TaskDto result = service.map(task, application, territory);

    // Then
    assertNull(result.getCommand());
  }
}
