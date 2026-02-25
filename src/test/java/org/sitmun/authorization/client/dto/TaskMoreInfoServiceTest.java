package org.sitmun.authorization.client.dto;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.domain.DomainConstants;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.cartography.Cartography;
import org.sitmun.domain.task.Task;
import org.sitmun.domain.task.type.TaskType;
import org.sitmun.domain.task.ui.TaskUI;
import org.sitmun.domain.territory.Territory;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("TaskMoreInfoService baseline tests")
class TaskMoreInfoServiceTest {

  private TaskMoreInfoService service;

  @BeforeEach
  void setUp() {
    service = new TaskMoreInfoService();
    ReflectionTestUtils.setField(service, "proxyUrl", "http://localhost:8080/middleware");
  }

  @Test
  @DisplayName("accept returns true for moreInfo task (case-insensitive)")
  void acceptReturnsTrueForMoreInfoTask() {
    // Given
    Task task = mock(Task.class);
    TaskType taskType = mock(TaskType.class);
    when(taskType.getTitle()).thenReturn("moreInfo");
    when(task.getType()).thenReturn(taskType);

    // When
    boolean result = service.accept(task);

    // Then
    assertTrue(result);
  }

  @Test
  @DisplayName("accept returns true for MOREINFO task (uppercase)")
  void acceptReturnsTrueForUppercaseMoreInfo() {
    // Given
    Task task = mock(Task.class);
    TaskType taskType = mock(TaskType.class);
    when(taskType.getTitle()).thenReturn("MOREINFO");
    when(task.getType()).thenReturn(taskType);

    // When
    boolean result = service.accept(task);

    // Then
    assertTrue(result);
  }

  @Test
  @DisplayName("accept returns true for MoReInFo task (mixed case)")
  void acceptReturnsTrueForMixedCaseMoreInfo() {
    // Given
    Task task = mock(Task.class);
    TaskType taskType = mock(TaskType.class);
    when(taskType.getTitle()).thenReturn("MoReInFo");
    when(task.getType()).thenReturn(taskType);

    // When
    boolean result = service.accept(task);

    // Then
    assertTrue(result);
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
  @DisplayName("accept returns false for null task type title")
  void acceptReturnsFalseForNullTaskTypeTitle() {
    // Given
    Task task = mock(Task.class);
    TaskType taskType = mock(TaskType.class);
    when(taskType.getTitle()).thenReturn(null);
    when(task.getType()).thenReturn(taskType);

    // When
    boolean result = service.accept(task);

    // Then
    assertFalse(result);
  }

  @Test
  @DisplayName("accept returns false for other task type")
  void acceptReturnsFalseForOtherTaskType() {
    // Given
    Task task = mock(Task.class);
    TaskType taskType = mock(TaskType.class);
    when(taskType.getTitle()).thenReturn("SQL");
    when(task.getType()).thenReturn(taskType);

    // When
    boolean result = service.accept(task);

    // Then
    assertFalse(result);
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
    properties.put(DomainConstants.Tasks.PROPERTY_PARAMETERS, List.of(param1));
    properties.put("scope", "API");
    properties.put("command", "https://api.example.com/info");

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
    assertEquals("API", result.getScope());
    assertEquals("https://api.example.com/info", result.getCommand());
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
    properties.put("scope", "WMS");
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
    properties.put("scope", "");
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
    properties.put("scope", "   ");
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
    properties.put("scope", "SQL");
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
    properties.put("scope", "SQL");
    when(task.getProperties()).thenReturn(properties);
    when(application.getId()).thenReturn(0);
    when(territory.getId()).thenReturn(0);

    // When
    TaskDto result = service.map(task, application, territory);

    // Then
    assertEquals("/proxy/0/0/SQL/100", result.getUrl());
  }

  @Test
  @DisplayName("map maps parameters entries by label")
  void mapMapsParametersEntriesByLabel() {
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
    properties.put(DomainConstants.Tasks.PROPERTY_PARAMETERS, List.of(param1, param2));
    when(task.getProperties()).thenReturn(properties);

    // When
    TaskDto result = service.map(task, application, territory);

    // Then
    assertNotNull(result.getParameters());
    assertEquals(2, result.getParameters().size());
    assertEquals(param1, result.getParameters().get("city"));
    assertEquals(param2, result.getParameters().get("country"));
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
    properties.put(DomainConstants.Tasks.PROPERTY_PARAMETERS, Collections.emptyList());
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
    properties.put(DomainConstants.Tasks.PROPERTY_PARAMETERS, List.of(param1));
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
    properties.put("command", null);
    when(task.getProperties()).thenReturn(properties);

    // When
    TaskDto result = service.map(task, application, territory);

    // Then
    assertNull(result.getCommand());
  }
}
