package org.sitmun.administration.service.template.export;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.sitmun.authorization.client.service.AuthorizationService;
import org.sitmun.domain.DomainConstants;
import org.sitmun.domain.task.Task;
import org.sitmun.domain.task.TaskRepository;
import org.sitmun.domain.task.type.TaskType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class TemplateExportAuthorizationServiceTest {

  private final AuthorizationService authorizationService = mock(AuthorizationService.class);
  private final TaskRepository taskRepository = mock(TaskRepository.class);
  private final TemplateExportAuthorizationService service =
      new TemplateExportAuthorizationService(authorizationService, taskRepository);

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void authorizesAccessibleDocumentExportAndTemplateTasks() {
    authenticateAsUser();
    Task exportTask = task(17, DomainConstants.Tasks.TASK_TYPE_ID_DOCUMENT_EXPORT);
    Task templateTask = task(15, DomainConstants.Tasks.TASK_TYPE_ID_TEMPLATE);
    when(authorizationService.findTasksByUserApplicationAndTerritory("user", 7, 11))
        .thenReturn(List.of(exportTask, templateTask));

    TemplateExportAuthorizationService.AuthorizedTasks result =
        service.authorize(17, 15, 7, 11);

    assertThat(result.exportTask()).isSameAs(exportTask);
    assertThat(result.templateTask()).isSameAs(templateTask);
    verify(authorizationService).findTasksByUserApplicationAndTerritory("user", 7, 11);
  }

  @Test
  void rejectsTasksOutsideCurrentProfile() {
    authenticateAsUser();
    Task exportTask = task(17, DomainConstants.Tasks.TASK_TYPE_ID_DOCUMENT_EXPORT);
    when(authorizationService.findTasksByUserApplicationAndTerritory("user", 7, 11))
        .thenReturn(List.of());

    assertThatThrownBy(() -> service.authorize(17, null, 7, 11))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void authorizesPublicTasksFromPublicProfileRolesAndTerritory() {
    SecurityContextHolder.getContext()
        .setAuthentication(new TestingAuthenticationToken("public", null, "ROLE_PUBLIC"));
    Task exportTask = task(17, DomainConstants.Tasks.TASK_TYPE_ID_DOCUMENT_EXPORT);
    when(authorizationService.findTasksByUserApplicationAndTerritory("public", 7, 11))
        .thenReturn(List.of(exportTask));

    TemplateExportAuthorizationService.AuthorizedTasks result =
        service.authorize(17, null, 7, 11);

    assertThat(result.exportTask()).isSameAs(exportTask);
    verify(authorizationService).findTasksByUserApplicationAndTerritory("public", 7, 11);
  }

  @Test
  void rejectsNonDocumentExportTask() {
    authenticateAsUser();
    when(authorizationService.findTasksByUserApplicationAndTerritory("user", 7, 11))
        .thenReturn(List.of(task(17, DomainConstants.Tasks.TASK_TYPE_ID_QUERY)));

    assertThatThrownBy(() -> service.authorize(17, null, 7, 11))
        .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
        .hasMessageContaining("document export task");
  }

  @Test
  void requiresProfileContextForNonAdminUsers() {
    authenticateAsUser();

    assertThatThrownBy(() -> service.authorize(17, null, null, 11))
        .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
        .hasMessageContaining("applicationId and territoryId are required");
  }

  @Test
  void rejectsTemplateTaskOutsideCurrentProfile() {
    authenticateAsUser();
    Task exportTask = task(17, DomainConstants.Tasks.TASK_TYPE_ID_DOCUMENT_EXPORT);
    when(authorizationService.findTasksByUserApplicationAndTerritory("user", 7, 11))
        .thenReturn(List.of(exportTask));

    assertThatThrownBy(() -> service.authorize(17, 15, 7, 11))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("task 15");
  }

  @Test
  void adminUsesGlobalTaskLookupWithoutProfileContext() {
    SecurityContextHolder.getContext()
        .setAuthentication(new TestingAuthenticationToken("admin", null, "ROLE_ADMIN"));
    Task exportTask = task(17, DomainConstants.Tasks.TASK_TYPE_ID_DOCUMENT_EXPORT);
    when(taskRepository.findById(17)).thenReturn(Optional.of(exportTask));

    TemplateExportAuthorizationService.AuthorizedTasks result =
        service.authorize(17, null, null, null);

    assertThat(result.exportTask()).isSameAs(exportTask);
  }

  private static void authenticateAsUser() {
    SecurityContextHolder.getContext()
        .setAuthentication(new TestingAuthenticationToken("user", null, "ROLE_USER"));
  }

  private static Task task(int id, int typeId) {
    return Task.builder().id(id).type(TaskType.builder().id(typeId).build()).build();
  }
}
