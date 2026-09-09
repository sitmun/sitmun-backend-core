package org.sitmun.administration.service.template.export;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.sitmun.authorization.client.service.AuthorizationService;
import org.sitmun.domain.DomainConstants;
import org.sitmun.domain.task.Task;
import org.sitmun.domain.task.TaskRepository;
import org.sitmun.infrastructure.security.core.SecurityRole;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class TemplateExportAuthorizationService {

  private final AuthorizationService authorizationService;
  private final TaskRepository taskRepository;

  public AuthorizedTasks authorize(
      Integer exportTaskId,
      Integer templateTaskId,
      Integer applicationId,
      Integer territoryId) {
    if (SecurityRole.isAdmin()) {
      Task exportTask = findTask(exportTaskId);
      Task templateTask = findTask(templateTaskId);
      validateTaskTypes(exportTask, templateTask);
      return new AuthorizedTasks(exportTask, templateTask);
    }
    if (exportTaskId == null || applicationId == null || territoryId == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "taskId, applicationId and territoryId are required for template export");
    }

    String username = currentUsername();
    List<Task> accessibleTasks =
        authorizationService.findTasksByUserApplicationAndTerritory(
            username, applicationId, territoryId);
    Task exportTask = findAccessibleTask(exportTaskId, accessibleTasks);
    Task templateTask = findAccessibleTask(templateTaskId, accessibleTasks);
    validateTaskTypes(exportTask, templateTask);
    return new AuthorizedTasks(exportTask, templateTask);
  }

  private Task findTask(Integer taskId) {
    if (taskId == null) {
      return null;
    }
    return taskRepository
        .findById(taskId)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found: " + taskId));
  }

  private static void validateTaskTypes(Task exportTask, Task templateTask) {
    if (exportTask != null && !DomainConstants.Tasks.isDocumentExportTask(exportTask)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "taskId must identify a document export task");
    }
    if (templateTask != null && !DomainConstants.Tasks.isTemplateTask(templateTask)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "templateTaskId must identify a template task");
    }
  }

  private static Task findAccessibleTask(Integer taskId, List<Task> accessibleTasks) {
    if (taskId == null) {
      return null;
    }
    return accessibleTasks.stream()
        .filter(candidate -> taskId.equals(candidate.getId()))
        .findFirst()
        .orElseThrow(() -> new AccessDeniedException("Access denied to task " + taskId));
  }

  private static String currentUsername() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || authentication.getName() == null) {
      throw new AccessDeniedException("Authenticated user is required");
    }
    return authentication.getName();
  }

  public record AuthorizedTasks(Task exportTask, Task templateTask) {}
}
