package org.sitmun.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.sitmun.domain.task.Task;
import org.sitmun.domain.task.type.TaskType;

class DomainConstantsMapImageTaskTest {

  @Test
  void mapImageTaskTypeIdIsEighteen() {
    assertEquals(18, DomainConstants.Tasks.TASK_TYPE_ID_MAP_IMAGE);
  }

  @Test
  void detectsMapImageTask() {
    Task task = new Task();
    TaskType taskType = new TaskType();
    taskType.setId(DomainConstants.Tasks.TASK_TYPE_ID_MAP_IMAGE);
    task.setType(taskType);

    assertTrue(DomainConstants.Tasks.isMapImageTask(task));
  }
}
