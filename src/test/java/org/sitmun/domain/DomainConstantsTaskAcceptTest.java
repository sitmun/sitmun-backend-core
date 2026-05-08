package org.sitmun.domain;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.sitmun.domain.DomainConstants.Tasks.*;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.domain.cartography.Cartography;
import org.sitmun.domain.database.DatabaseConnection;
import org.sitmun.domain.task.Task;
import org.sitmun.domain.task.type.TaskType;

@DisplayName("DomainConstants Task Accept Rules")
class DomainConstantsTaskAcceptTest {

  @Test
  @DisplayName("isSqlQueryTask returns true for Query task with connection and sql-query scope")
  void isSqlQueryTaskReturnsTrueForValidTask() {
    Task task = mock(Task.class);
    TaskType taskType = mock(TaskType.class);
    DatabaseConnection connection = mock(DatabaseConnection.class);

    when(taskType.getId()).thenReturn(TASK_TYPE_ID_QUERY);
    when(task.getType()).thenReturn(taskType);
    when(task.getConnection()).thenReturn(connection);

    Map<String, Object> properties = new HashMap<>();
    properties.put(PROPERTY_SCOPE, SCOPE_SQL_QUERY);
    when(task.getProperties()).thenReturn(properties);

    assertTrue(isSqlQueryTask(task));
  }

  @Test
  @DisplayName("isSqlQueryTask returns false for Query task with connection but wrong scope")
  void isSqlQueryTaskReturnsFalseForWrongScope() {
    Task task = mock(Task.class);
    TaskType taskType = mock(TaskType.class);
    DatabaseConnection connection = mock(DatabaseConnection.class);

    when(taskType.getId()).thenReturn(TASK_TYPE_ID_QUERY);
    when(task.getType()).thenReturn(taskType);
    when(task.getConnection()).thenReturn(connection);

    Map<String, Object> properties = new HashMap<>();
    properties.put(PROPERTY_SCOPE, SCOPE_WEB_API_QUERY);
    when(task.getProperties()).thenReturn(properties);

    assertFalse(isSqlQueryTask(task));
  }

  @Test
  @DisplayName("isSqlQueryTask returns true for legacy task with connection but no scope")
  void isSqlQueryTaskReturnsTrueForLegacyTask() {
    Task task = mock(Task.class);
    TaskType taskType = mock(TaskType.class);
    DatabaseConnection connection = mock(DatabaseConnection.class);

    when(taskType.getId()).thenReturn(TASK_TYPE_ID_QUERY);
    when(task.getType()).thenReturn(taskType);
    when(task.getConnection()).thenReturn(connection);
    when(task.getProperties()).thenReturn(null);

    assertTrue(isSqlQueryTask(task));
  }

  @Test
  @DisplayName(
      "isCartographyQueryTask returns true for Query task with cartography and cartography-query scope")
  void isCartographyQueryTaskReturnsTrueForValidTask() {
    Task task = mock(Task.class);
    TaskType taskType = mock(TaskType.class);
    Cartography cartography = mock(Cartography.class);

    when(taskType.getId()).thenReturn(TASK_TYPE_ID_QUERY);
    when(task.getType()).thenReturn(taskType);
    when(task.getCartography()).thenReturn(cartography);

    Map<String, Object> properties = new HashMap<>();
    properties.put(PROPERTY_SCOPE, SCOPE_CARTOGRAPHY_QUERY);
    when(task.getProperties()).thenReturn(properties);

    assertTrue(isCartographyQueryTask(task));
  }

  @Test
  @DisplayName(
      "isCartographyQueryTask returns false for Query task with cartography but wrong scope")
  void isCartographyQueryTaskReturnsFalseForWrongScope() {
    Task task = mock(Task.class);
    TaskType taskType = mock(TaskType.class);
    Cartography cartography = mock(Cartography.class);

    when(taskType.getId()).thenReturn(TASK_TYPE_ID_QUERY);
    when(task.getType()).thenReturn(taskType);
    when(task.getCartography()).thenReturn(cartography);

    Map<String, Object> properties = new HashMap<>();
    properties.put(PROPERTY_SCOPE, SCOPE_SQL_QUERY);
    when(task.getProperties()).thenReturn(properties);

    assertFalse(isCartographyQueryTask(task));
  }

  @Test
  @DisplayName("isCartographyQueryTask returns true for legacy task with cartography but no scope")
  void isCartographyQueryTaskReturnsTrueForLegacyTask() {
    Task task = mock(Task.class);
    TaskType taskType = mock(TaskType.class);
    Cartography cartography = mock(Cartography.class);

    when(taskType.getId()).thenReturn(TASK_TYPE_ID_QUERY);
    when(task.getType()).thenReturn(taskType);
    when(task.getCartography()).thenReturn(cartography);
    when(task.getProperties()).thenReturn(null);

    assertTrue(isCartographyQueryTask(task));
  }

  @Test
  @DisplayName("isWebApiQuery returns true for Query task with web-api-query scope and no FKs")
  void isWebApiQueryReturnsTrueForValidTask() {
    Task task = mock(Task.class);
    TaskType taskType = mock(TaskType.class);

    when(taskType.getId()).thenReturn(TASK_TYPE_ID_QUERY);
    when(task.getType()).thenReturn(taskType);
    when(task.getConnection()).thenReturn(null);
    when(task.getCartography()).thenReturn(null);
    when(task.getService()).thenReturn(null);

    Map<String, Object> properties = new HashMap<>();
    properties.put(PROPERTY_SCOPE, SCOPE_WEB_API_QUERY);
    when(task.getProperties()).thenReturn(properties);

    assertTrue(isWebApiQuery(task));
  }

  @Test
  @DisplayName(
      "isWebApiQuery returns false for Query task with web-api-query scope but has connection")
  void isWebApiQueryReturnsFalseWhenConnectionPresent() {
    Task task = mock(Task.class);
    TaskType taskType = mock(TaskType.class);
    DatabaseConnection connection = mock(DatabaseConnection.class);

    when(taskType.getId()).thenReturn(TASK_TYPE_ID_QUERY);
    when(task.getType()).thenReturn(taskType);
    when(task.getConnection()).thenReturn(connection);

    Map<String, Object> properties = new HashMap<>();
    properties.put(PROPERTY_SCOPE, SCOPE_WEB_API_QUERY);
    when(task.getProperties()).thenReturn(properties);

    assertFalse(isWebApiQuery(task));
  }

  @Test
  @DisplayName("isUrlQueryTask returns true for Query task with external-link scope and no FKs")
  void isUrlQueryTaskReturnsTrueForValidTask() {
    Task task = mock(Task.class);
    TaskType taskType = mock(TaskType.class);

    when(taskType.getId()).thenReturn(TASK_TYPE_ID_QUERY);
    when(task.getType()).thenReturn(taskType);
    when(task.getConnection()).thenReturn(null);
    when(task.getCartography()).thenReturn(null);

    Map<String, Object> properties = new HashMap<>();
    properties.put(PROPERTY_SCOPE, SCOPE_URL_QUERY);
    when(task.getProperties()).thenReturn(properties);

    assertTrue(isUrlQueryTask(task));
  }

  @Test
  @DisplayName(
      "isUrlQueryTask returns false for Query task with external-link scope but has cartography")
  void isUrlQueryTaskReturnsFalseWhenCartographyPresent() {
    Task task = mock(Task.class);
    TaskType taskType = mock(TaskType.class);
    Cartography cartography = mock(Cartography.class);

    when(taskType.getId()).thenReturn(TASK_TYPE_ID_QUERY);
    when(task.getType()).thenReturn(taskType);
    when(task.getConnection()).thenReturn(null);
    when(task.getCartography()).thenReturn(cartography);

    Map<String, Object> properties = new HashMap<>();
    properties.put(PROPERTY_SCOPE, SCOPE_URL_QUERY);
    when(task.getProperties()).thenReturn(properties);

    assertFalse(isUrlQueryTask(task));
  }

  @Test
  @DisplayName("isQueryTask returns false for null task")
  void isQueryTaskReturnsFalseWhenTaskNull() {
    assertFalse(isQueryTask(null));
  }

  @Test
  @DisplayName("isQueryTask returns true when type id matches TASK_TYPE_ID_QUERY")
  void isQueryTaskReturnsTrueWhenTypeIdIsQuery() {
    Task task = mock(Task.class);
    TaskType taskType = mock(TaskType.class);
    when(taskType.getId()).thenReturn(TASK_TYPE_ID_QUERY);
    when(task.getType()).thenReturn(taskType);

    assertTrue(isQueryTask(task));
  }

  @Test
  @DisplayName("isLegacyBasicTask returns false for null task")
  void isLegacyBasicTaskReturnsFalseWhenTaskNull() {
    assertFalse(isLegacyBasicTask(null));
  }

  @Test
  @DisplayName("isLegacyBasicTask returns false when Basic task properties contain scope")
  void isLegacyBasicTaskReturnsFalseWhenScopePresent() {
    Task task = mock(Task.class);
    TaskType taskType = mock(TaskType.class);
    when(taskType.getId()).thenReturn(TASK_TYPE_ID_BASIC);
    when(task.getType()).thenReturn(taskType);
    when(task.getProperties()).thenReturn(Map.of(PROPERTY_SCOPE, SCOPE_SQL_QUERY));

    assertFalse(isLegacyBasicTask(task));
  }

  @Test
  @DisplayName("isLegacyBasicTask returns true for Basic task without scope in properties")
  void isLegacyBasicTaskReturnsTrueForBasicWithoutScope() {
    Task task = mock(Task.class);
    TaskType taskType = mock(TaskType.class);
    when(taskType.getId()).thenReturn(TASK_TYPE_ID_BASIC);
    when(task.getType()).thenReturn(taskType);
    when(task.getProperties()).thenReturn(null);

    assertTrue(isLegacyBasicTask(task));
  }
}
