package org.sitmun.domain.task;

import java.util.Objects;
import java.util.Optional;
import org.sitmun.domain.DomainConstants;
import org.sitmun.domain.task.relation.TaskRelation;
import org.springframework.stereotype.Component;

/**
 * Resolves the execution task for more-info tasks. More-info tasks can delegate their actual
 * execution (SQL, API call, etc.) to a linked query task via a {@code query-task} relation.
 */
@Component
public class MoreInfoTaskResolver {

  /**
   * Finds the related query task linked via a {@code query-task} relation.
   *
   * @param task the more-info task
   * @return the related query task, or empty if no such relation exists
   */
  public Optional<Task> findRelatedQueryTask(Task task) {
    if (task == null || task.getRelations() == null) {
      return Optional.empty();
    }
    return task.getRelations().stream()
        .filter(Objects::nonNull)
        .filter(
            r ->
                DomainConstants.Tasks.RELATION_TYPE_QUERY_TASK.equalsIgnoreCase(
                    r.getRelationType()))
        .map(TaskRelation::getRelatedTask)
        .filter(Objects::nonNull)
        .findFirst();
  }

  /**
   * Resolves the execution task: if the input is a more-info task with a linked query task, returns
   * the query task; otherwise returns the input unchanged.
   *
   * @param task the task to resolve
   * @return the execution task (query task if linked, or the input task itself)
   */
  public Task resolveOrSelf(Task task) {
    if (task == null) {
      return null;
    }
    // Both more-info tasks and locator tasks delegate execution to a linked query task.
    if (!DomainConstants.Tasks.isMoreInfoTask(task) && !DomainConstants.Tasks.isLocatorTask(task)) {
      return task;
    }
    return findRelatedQueryTask(task).orElse(task);
  }
}
