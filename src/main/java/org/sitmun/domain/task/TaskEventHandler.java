package org.sitmun.domain.task;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.HandleBeforeSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.stereotype.Component;

@Component
@RepositoryEventHandler
@Slf4j
public class TaskEventHandler {

  private final List<TaskValidator> taskValidator;

  TaskEventHandler(List<TaskValidator> taskValidator) {
    this.taskValidator = taskValidator;
  }

  @HandleBeforeSave
  @HandleBeforeCreate
  public void handleTaskCreate(@NotNull Task task) {

    for (TaskValidator validator : taskValidator) {
      if (validator.accept(task)) {
        validator.validate(task);
      }
    }
  }
}
