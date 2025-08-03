package org.sitmun.domain.task;

import java.util.Objects;
import org.springframework.data.rest.core.RepositoryConstraintViolationException;
import org.springframework.stereotype.Component;

@Component
public class TaskQueryValidator implements TaskValidator {

  @Override
  public boolean accept(Task task) {
    if (task == null || task.getType() == null) {
      return false;
    }
    return Objects.equals("Query", task.getType().getTitle());
  }

  @Override
  public void validate(Task task) throws RepositoryConstraintViolationException {
    // TODO: Implement validation logic for Query tasks
  }
}
