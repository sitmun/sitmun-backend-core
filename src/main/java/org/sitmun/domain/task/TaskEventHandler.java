package org.sitmun.domain.task;

import lombok.extern.slf4j.Slf4j;
import org.sitmun.domain.task.type.TaskType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.core.RepositoryConstraintViolationException;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.HandleBeforeSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.stereotype.Component;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

@Component
@RepositoryEventHandler
@Slf4j
public class TaskEventHandler {

  private final List<TaskValidator> taskValidator;

  TaskEventHandler(@Autowired List<TaskValidator> taskValidator) {
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
