package org.sitmun.domain.task;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.sitmun.domain.DomainConstants.Tasks.*;

import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.domain.task.relation.TaskRelation;
import org.sitmun.domain.task.type.TaskType;

@DisplayName("MoreInfoTaskResolver unit tests")
class MoreInfoTaskResolverTest {

  private MoreInfoTaskResolver resolver;

  @BeforeEach
  void setUp() {
    resolver = new MoreInfoTaskResolver();
  }

  @Test
  @DisplayName("findRelatedQueryTask returns empty for null task")
  void findRelatedQueryTaskReturnsEmptyForNullTask() {
    // When
    Optional<Task> result = resolver.findRelatedQueryTask(null);

    // Then
    assertTrue(result.isEmpty());
  }

  @Test
  @DisplayName("findRelatedQueryTask returns empty when task has no relations")
  void findRelatedQueryTaskReturnsEmptyWhenNoRelations() {
    // Given
    Task task = mock(Task.class);
    when(task.getRelations()).thenReturn(null);

    // When
    Optional<Task> result = resolver.findRelatedQueryTask(task);

    // Then
    assertTrue(result.isEmpty());
  }

  @Test
  @DisplayName("findRelatedQueryTask returns empty when no query-task relation exists")
  void findRelatedQueryTaskReturnsEmptyWhenNoQueryTaskRelation() {
    // Given
    Task task = mock(Task.class);
    TaskRelation otherRelation = TaskRelation.builder().relationType("other-relation-type").build();
    when(task.getRelations()).thenReturn(Set.of(otherRelation));

    // When
    Optional<Task> result = resolver.findRelatedQueryTask(task);

    // Then
    assertTrue(result.isEmpty());
  }

  @Test
  @DisplayName("findRelatedQueryTask returns the related query task")
  void findRelatedQueryTaskReturnsRelatedQueryTask() {
    // Given
    Task moreInfoTask = mock(Task.class);
    Task queryTask = mock(Task.class);

    TaskRelation queryRelation =
        TaskRelation.builder()
            .relationType(RELATION_TYPE_QUERY_TASK)
            .relatedTask(queryTask)
            .build();

    when(moreInfoTask.getRelations()).thenReturn(Set.of(queryRelation));

    // When
    Optional<Task> result = resolver.findRelatedQueryTask(moreInfoTask);

    // Then
    assertTrue(result.isPresent());
    assertSame(queryTask, result.get());
  }

  @Test
  @DisplayName("findRelatedQueryTask is case-insensitive for relation type")
  void findRelatedQueryTaskIsCaseInsensitive() {
    // Given
    Task moreInfoTask = mock(Task.class);
    Task queryTask = mock(Task.class);

    TaskRelation queryRelation =
        TaskRelation.builder().relationType("QUERY-TASK").relatedTask(queryTask).build();

    when(moreInfoTask.getRelations()).thenReturn(Set.of(queryRelation));

    // When
    Optional<Task> result = resolver.findRelatedQueryTask(moreInfoTask);

    // Then
    assertTrue(result.isPresent());
    assertSame(queryTask, result.get());
  }

  @Test
  @DisplayName("findRelatedQueryTask filters out null relations")
  void findRelatedQueryTaskFiltersOutNullRelations() {
    // Given
    Task moreInfoTask = mock(Task.class);
    Task queryTask = mock(Task.class);

    TaskRelation queryRelation =
        TaskRelation.builder()
            .relationType(RELATION_TYPE_QUERY_TASK)
            .relatedTask(queryTask)
            .build();

    Set<TaskRelation> relations = new java.util.HashSet<>();
    relations.add(null);
    relations.add(queryRelation);
    when(moreInfoTask.getRelations()).thenReturn(relations);

    // When
    Optional<Task> result = resolver.findRelatedQueryTask(moreInfoTask);

    // Then
    assertTrue(result.isPresent());
    assertSame(queryTask, result.get());
  }

  @Test
  @DisplayName("resolveOrSelf returns input task when task is null")
  void resolveOrSelfReturnsNullWhenTaskIsNull() {
    // When
    Task result = resolver.resolveOrSelf(null);

    // Then
    assertNull(result);
  }

  @Test
  @DisplayName("resolveOrSelf returns input task when not a more-info task")
  void resolveOrSelfReturnsInputTaskWhenNotMoreInfo() {
    // Given
    Task queryTask = mock(Task.class);
    TaskType taskType = mock(TaskType.class);
    when(taskType.getId()).thenReturn(TASK_TYPE_ID_QUERY);
    when(queryTask.getType()).thenReturn(taskType);

    // When
    Task result = resolver.resolveOrSelf(queryTask);

    // Then
    assertSame(queryTask, result);
  }

  @Test
  @DisplayName("resolveOrSelf returns input task when no related query task exists")
  void resolveOrSelfReturnsInputTaskWhenNoRelatedQueryTask() {
    // Given
    Task moreInfoTask = mock(Task.class);
    TaskType taskType = mock(TaskType.class);
    when(taskType.getId()).thenReturn(TASK_TYPE_ID_MORE_INFO);
    when(moreInfoTask.getType()).thenReturn(taskType);
    when(moreInfoTask.getRelations()).thenReturn(Set.of());

    // When
    Task result = resolver.resolveOrSelf(moreInfoTask);

    // Then
    assertSame(moreInfoTask, result);
  }

  @Test
  @DisplayName("resolveOrSelf returns the related query task for more-info tasks")
  void resolveOrSelfReturnsRelatedQueryTask() {
    // Given
    Task moreInfoTask = mock(Task.class);
    Task queryTask = mock(Task.class);

    TaskType moreInfoType = mock(TaskType.class);
    when(moreInfoType.getId()).thenReturn(TASK_TYPE_ID_MORE_INFO);
    when(moreInfoTask.getType()).thenReturn(moreInfoType);

    TaskRelation queryRelation =
        TaskRelation.builder()
            .relationType(RELATION_TYPE_QUERY_TASK)
            .relatedTask(queryTask)
            .build();

    when(moreInfoTask.getRelations()).thenReturn(Set.of(queryRelation));

    // When
    Task result = resolver.resolveOrSelf(moreInfoTask);

    // Then
    assertSame(queryTask, result);
  }
}
