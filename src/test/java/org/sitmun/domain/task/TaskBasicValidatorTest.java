package org.sitmun.domain.task;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.sitmun.domain.DomainConstants.Tasks.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sitmun.authorization.client.service.support.BasicParameterValueConverter;
import org.sitmun.domain.task.type.TaskType;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaskBasicValidator unit tests")
class TaskBasicValidatorTest {

  private TaskBasicValidator validator;

  @BeforeEach
  void setUp() {
    BasicParameterValueConverter parameterValueConverter = new BasicParameterValueConverter();
    validator = new TaskBasicValidator(parameterValueConverter);
  }

  @Test
  @DisplayName("accept is false when task is null")
  void acceptReturnsFalseWhenTaskNull() {
    assertFalse(validator.accept(null));
  }

  @Test
  @DisplayName("accept is false when task type is not Basic")
  void acceptReturnsFalseWhenNotBasicType() {
    Task task = mock(Task.class);
    TaskType taskType = mock(TaskType.class);
    when(taskType.getId()).thenReturn(TASK_TYPE_ID_QUERY);
    when(task.getType()).thenReturn(taskType);

    assertFalse(validator.accept(task));
  }

  @Test
  @DisplayName("accept is false for Basic task when properties include scope")
  void acceptReturnsFalseWhenBasicHasScope() {
    Task task = mock(Task.class);
    TaskType taskType = mock(TaskType.class);
    when(taskType.getId()).thenReturn(TASK_TYPE_ID_BASIC);
    when(task.getType()).thenReturn(taskType);
    when(task.getProperties())
        .thenReturn(
            Map.of(PROPERTY_SCOPE, "any", PROPERTY_PARAMETERS, List.<Map<String, Object>>of()));

    assertFalse(validator.accept(task));
  }

  @Test
  @DisplayName("accept is true for Basic task without scope (legacy payload shape)")
  void acceptReturnsTrueForBasicWithoutScope() {
    Task task = mock(Task.class);
    TaskType taskType = mock(TaskType.class);
    when(taskType.getId()).thenReturn(TASK_TYPE_ID_BASIC);
    when(task.getType()).thenReturn(taskType);
    when(task.getProperties()).thenReturn(new HashMap<>());

    assertTrue(validator.accept(task));
  }

  @Test
  @DisplayName("validate passes for Basic three-field string parameter")
  void validateAcceptsStringParameter() {
    Task task = mock(Task.class);

    Map<String, Object> param =
        Map.of(PARAMETERS_NAME, "x", PARAMETERS_TYPE, TYPE_STRING, PARAMETERS_VALUE, "hi");
    when(task.getProperties()).thenReturn(Map.of(PROPERTY_PARAMETERS, List.of(param)));

    assertDoesNotThrow(() -> validator.validate(task));
  }
}
