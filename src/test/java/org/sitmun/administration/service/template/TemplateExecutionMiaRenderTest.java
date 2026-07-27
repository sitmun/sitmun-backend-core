package org.sitmun.administration.service.template;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.Test;
import org.sitmun.administration.controller.dto.MoreInfoAdvancedRenderRequestDto;
import org.sitmun.administration.controller.dto.MoreInfoAdvancedRenderResponseDto;
import org.sitmun.administration.controller.dto.TemplatePreviewResponseDto;
import org.sitmun.administration.service.database.DatabaseConnectionService;
import org.sitmun.administration.service.extractor.HttpClientFactory;
import org.sitmun.administration.service.i18n.CurrentRequestLanguageResolver;
import org.sitmun.administration.service.i18n.LiteralTranslationResolver;
import org.sitmun.administration.service.template.childdata.TemplateChildDataService;
import org.sitmun.authorization.access.UserApplicationAccessPolicy;
import org.sitmun.authorization.proxy.dto.ConfigProxyDto;
import org.sitmun.authorization.proxy.protocols.wms.WmsPayloadDto;
import org.sitmun.authorization.proxy.service.ProxyConfigurationService;
import org.sitmun.authorization.proxy.service.RequestCoordinates;
import org.sitmun.domain.DomainConstants;
import org.sitmun.domain.role.Role;
import org.sitmun.domain.role.RoleRepository;
import org.sitmun.domain.task.Task;
import org.sitmun.domain.task.TaskRepository;
import org.sitmun.domain.task.relation.TaskRelation;
import org.sitmun.domain.task.relation.TaskRelationRepository;
import org.sitmun.domain.task.type.TaskType;
import org.sitmun.domain.task.ui.TaskUI;
import org.sitmun.domain.territory.Territory;
import org.sitmun.infrastructure.variables.SystemVariableResolver;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

class TemplateExecutionMiaRenderTest extends TemplateExecutionServiceTestFixtures {
  @Test
  void renderMoreInfoAdvancedUsesPassedLanguageForNoDataWithoutLangQueryParam() {
    TaskRepository taskRepository = mock(TaskRepository.class);
    TemplateRequestCoordinatesService coordinatesService =
        mock(TemplateRequestCoordinatesService.class);
    when(coordinatesService.build(any(), any()))
        .thenReturn(requestCoordinatesWithUserPermission(7));
    TemplateExecutionService service =
        newService(
            taskRepository,
            mock(TaskRelationRepository.class),
            mock(ProxyConfigurationService.class),
            mock(DatabaseConnectionService.class),
            mock(HttpClientFactory.class),
            mock(SystemVariableResolver.class),
            mock(TemplateRenderService.class),
            coordinatesService,
            new ObjectMapper());
    Task accessibleParent = Task.builder().id(16).build();
    when(taskRepository.findByRolesAndTerritory(any(), eq(7)))
        .thenReturn(List.of(accessibleParent));

    Task miaTask = mock(Task.class);
    when(miaTask.getId()).thenReturn(16);
    when(miaTask.getName()).thenReturn("MIA parent");
    when(miaTask.getType())
        .thenReturn(
            TaskType.builder().id(DomainConstants.Tasks.TASK_TYPE_ID_MORE_INFO_ADVANCED).build());
    when(miaTask.getProperties())
        .thenReturn(
            Map.of(
                DomainConstants.Tasks.PROPERTY_PARAMETERS,
                List.of(
                    Map.of(
                        "name",
                        "includedTasks",
                        "type",
                        DomainConstants.Tasks.TYPE_ARRAY,
                        "value",
                        "[{\"id\":101,\"name\":\"Document\",\"order\":0,\"childType\":\"query\"}]"))));

    Task childTask = mock(Task.class);
    when(childTask.getId()).thenReturn(101);

    when(taskRepository.findById(16)).thenReturn(Optional.of(miaTask));
    when(taskRepository.findById(101)).thenReturn(Optional.of(childTask));

    RequestContextHolder.resetRequestAttributes();
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken("viewer", "n/a", List.of()));
    try {
      MoreInfoAdvancedRenderRequestDto renderRequest = new MoreInfoAdvancedRenderRequestDto();
      renderRequest.setMiaTaskIds(List.of(16));
      renderRequest.setAppId(5);
      renderRequest.setTerId(7);
      renderRequest.setParameters(Map.of("id", "A-1"));

      MoreInfoAdvancedRenderResponseDto result =
          service.renderMoreInfoAdvanced(renderRequest, "en");

      assertThat(result.getTasks()).hasSize(1);
      assertThat(result.getTasks().get(0).getHtml()).contains("No data").doesNotContain("A-1");
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  @Test
  void renderMoreInfoAdvancedShowsTranslatedNoDataWhenChildTaskIsUnauthorized() {
    TaskRepository taskRepository = mock(TaskRepository.class);
    TemplateRequestCoordinatesService coordinatesService =
        mock(TemplateRequestCoordinatesService.class);
    when(coordinatesService.build(any(), any()))
        .thenReturn(requestCoordinatesWithUserPermission(7));
    TemplateExecutionService service =
        newService(
            taskRepository,
            mock(TaskRelationRepository.class),
            mock(ProxyConfigurationService.class),
            mock(DatabaseConnectionService.class),
            mock(HttpClientFactory.class),
            mock(SystemVariableResolver.class),
            mock(TemplateRenderService.class),
            coordinatesService,
            new ObjectMapper());
    Task accessibleParent = Task.builder().id(16).build();
    when(taskRepository.findByRolesAndTerritory(any(), eq(7)))
        .thenReturn(List.of(accessibleParent));

    Task miaTask = mock(Task.class);
    when(miaTask.getId()).thenReturn(16);
    when(miaTask.getName()).thenReturn("MIA parent");
    when(miaTask.getType())
        .thenReturn(
            TaskType.builder().id(DomainConstants.Tasks.TASK_TYPE_ID_MORE_INFO_ADVANCED).build());
    when(miaTask.getProperties())
        .thenReturn(
            Map.of(
                DomainConstants.Tasks.PROPERTY_PARAMETERS,
                List.of(
                    Map.of(
                        "name",
                        "includedTasks",
                        "type",
                        DomainConstants.Tasks.TYPE_ARRAY,
                        "value",
                        "[{\"id\":101,\"name\":\"Document\",\"order\":0,\"childType\":\"query\"}]"))));

    Task childTask = mock(Task.class);
    when(childTask.getId()).thenReturn(101);

    when(taskRepository.findById(16)).thenReturn(Optional.of(miaTask));
    when(taskRepository.findById(101)).thenReturn(Optional.of(childTask));

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setParameter("lang", "en");
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken("viewer", "n/a", List.of()));
    try {
      MoreInfoAdvancedRenderRequestDto renderRequest = new MoreInfoAdvancedRenderRequestDto();
      renderRequest.setMiaTaskIds(List.of(16));
      renderRequest.setAppId(5);
      renderRequest.setTerId(7);
      renderRequest.setParameters(Map.of("id", "A-1"));

      MoreInfoAdvancedRenderResponseDto result =
          service.renderMoreInfoAdvanced(renderRequest, "en");

      assertThat(result.getTasks()).hasSize(1);
      assertThat(result.getTasks().get(0).getHtml()).contains("No data").doesNotContain("A-1");
    } finally {
      RequestContextHolder.resetRequestAttributes();
      SecurityContextHolder.clearContext();
    }
  }

  @Test
  void renderMoreInfoAdvancedLocalizesDefaultTabTitleAndInvalidChildMessage() {
    TaskRepository taskRepository = mock(TaskRepository.class);
    TemplateRequestCoordinatesService coordinatesService =
        mock(TemplateRequestCoordinatesService.class);
    when(coordinatesService.build(any(), any()))
        .thenReturn(requestCoordinatesWithUserPermission(7));
    TemplateExecutionService service =
        newService(
            taskRepository,
            mock(TaskRelationRepository.class),
            mock(ProxyConfigurationService.class),
            mock(DatabaseConnectionService.class),
            mock(HttpClientFactory.class),
            mock(SystemVariableResolver.class),
            mock(TemplateRenderService.class),
            coordinatesService,
            new ObjectMapper());
    when(taskRepository.findByRolesAndTerritory(any(), eq(7)))
        .thenReturn(List.of(Task.builder().id(16).build()));

    Task miaTask = mock(Task.class);
    when(miaTask.getId()).thenReturn(16);
    when(miaTask.getName()).thenReturn("MIA parent");
    when(miaTask.getType())
        .thenReturn(
            TaskType.builder().id(DomainConstants.Tasks.TASK_TYPE_ID_MORE_INFO_ADVANCED).build());
    when(miaTask.getProperties())
        .thenReturn(
            Map.of(
                DomainConstants.Tasks.PROPERTY_PARAMETERS,
                List.of(
                    Map.of(
                        "name",
                        "includedTasks",
                        "type",
                        DomainConstants.Tasks.TYPE_ARRAY,
                        "value",
                        "[{\"id\":101,\"order\":0,\"childType\":\"query\"},"
                            + "{\"id\":\"bad\",\"order\":1,\"childType\":\"query\"}]"))));
    when(taskRepository.findById(16)).thenReturn(Optional.of(miaTask));
    when(taskRepository.findById(101)).thenReturn(Optional.of(Task.builder().id(101).build()));

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setParameter("lang", "en");
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken("viewer", "n/a", List.of()));
    try {
      MoreInfoAdvancedRenderRequestDto renderRequest = new MoreInfoAdvancedRenderRequestDto();
      renderRequest.setMiaTaskIds(List.of(16));
      renderRequest.setAppId(5);
      renderRequest.setTerId(7);
      renderRequest.setParameters(Map.of());

      MoreInfoAdvancedRenderResponseDto result =
          service.renderMoreInfoAdvanced(renderRequest, "en");

      assertThat(result.getTasks()).hasSize(1);
      assertThat(result.getTasks().get(0).getHtml())
          .contains(">Query 1<")
          .contains(">Query 2<")
          .contains("Invalid child task id")
          .doesNotContain(">Consulta ");
    } finally {
      RequestContextHolder.resetRequestAttributes();
      SecurityContextHolder.clearContext();
    }
  }

  @Test
  void renderMoreInfoAdvancedKeepsTemplateHtmlWithTranslatedNoDataWhenNestedChildIsUnauthorized() {
    TaskRepository taskRepository = mock(TaskRepository.class);
    TaskRelationRepository taskRelationRepository = mock(TaskRelationRepository.class);
    TemplateRenderService templateRenderService = mock(TemplateRenderService.class);
    TemplateRequestCoordinatesService coordinatesService =
        mock(TemplateRequestCoordinatesService.class);
    when(coordinatesService.build(any(), any()))
        .thenReturn(requestCoordinatesWithUserPermission(7));
    TemplateExecutionService service =
        newService(
            taskRepository,
            taskRelationRepository,
            mock(ProxyConfigurationService.class),
            mock(DatabaseConnectionService.class),
            mock(HttpClientFactory.class),
            mock(SystemVariableResolver.class),
            templateRenderService,
            coordinatesService,
            new ObjectMapper());

    Task miaTask = mock(Task.class);
    when(miaTask.getId()).thenReturn(16);
    when(miaTask.getName()).thenReturn("MIA parent");
    when(miaTask.getType())
        .thenReturn(
            TaskType.builder().id(DomainConstants.Tasks.TASK_TYPE_ID_MORE_INFO_ADVANCED).build());
    when(miaTask.getProperties())
        .thenReturn(
            Map.of(
                DomainConstants.Tasks.PROPERTY_PARAMETERS,
                List.of(
                    Map.of(
                        "name",
                        "includedTasks",
                        "type",
                        DomainConstants.Tasks.TYPE_ARRAY,
                        "value",
                        "[{\"id\":401,\"name\":\"Plantilla\",\"order\":0,\"childType\":\"template\"}]"))));

    Task templateTask =
        Task.builder()
            .id(401)
            .properties(
                Map.of(
                    DomainConstants.Tasks.PROPERTY_TEMPLATE_HTML,
                    "<article>{{consulta_api.html}} {{consulta_api.value}}</article>"))
            .type(
                org.sitmun.domain.task.type.TaskType.builder()
                    .id(DomainConstants.Tasks.TASK_TYPE_ID_TEMPLATE)
                    .build())
            .build();
    Task apiTask = Task.builder().id(402).build();
    when(taskRepository.findByRolesAndTerritory(any(), eq(7)))
        .thenReturn(List.of(Task.builder().id(16).build(), templateTask));

    when(taskRepository.findById(16)).thenReturn(Optional.of(miaTask));
    when(taskRepository.findById(401)).thenReturn(Optional.of(templateTask));
    when(taskRelationRepository.findByTaskId(401))
        .thenReturn(
            List.of(
                TaskRelation.builder()
                    .id(1)
                    .task(templateTask)
                    .relationType("template-task")
                    .referenceAlias("consulta_api")
                    .relatedTask(apiTask)
                    .build()));
    when(templateRenderService.renderPreview(
            eq("<article>{{consulta_api.html}} {{consulta_api.value}}</article>"),
            any(),
            eq(List.of()),
            eq("en")))
        .thenAnswer(
            invocation -> {
              Map<String, Object> context = invocation.getArgument(1);
              Map<String, Object> childContext = (Map<String, Object>) context.get("consulta_api");
              return TemplatePreviewResponseDto.builder()
                  .html(
                      "<article>"
                          + childContext.get("html")
                          + " "
                          + childContext.get("value")
                          + "</article>")
                  .placeholders(List.of())
                  .build();
            });

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setParameter("lang", "en");
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken("viewer", "n/a", List.of()));
    try {
      MoreInfoAdvancedRenderRequestDto renderRequest = new MoreInfoAdvancedRenderRequestDto();
      renderRequest.setMiaTaskIds(List.of(16));
      renderRequest.setAppId(5);
      renderRequest.setTerId(7);
      renderRequest.setParameters(Map.of());

      MoreInfoAdvancedRenderResponseDto result =
          service.renderMoreInfoAdvanced(renderRequest, "en");

      assertThat(result.getTasks()).hasSize(1);
      assertThat(result.getTasks().get(0).getHtml())
          .contains("<article>")
          .contains("No data")
          .doesNotContain("sitmun-template-child-error");
    } finally {
      RequestContextHolder.resetRequestAttributes();
      SecurityContextHolder.clearContext();
    }
  }

  @Test
  void renderMoreInfoAdvancedDeniesParentWhenCoordinatesAreIncomplete() {
    TaskRepository taskRepository = mock(TaskRepository.class);
    TemplateRequestCoordinatesService coordinatesService =
        mock(TemplateRequestCoordinatesService.class);
    RequestCoordinates unresolvedCoordinates = new RequestCoordinates();
    unresolvedCoordinates.setTerritory(Territory.builder().id(7).build());
    when(coordinatesService.build(any(), any())).thenReturn(unresolvedCoordinates);
    TemplateExecutionService service =
        newService(
            taskRepository,
            mock(TaskRelationRepository.class),
            mock(ProxyConfigurationService.class),
            mock(DatabaseConnectionService.class),
            mock(HttpClientFactory.class),
            mock(SystemVariableResolver.class),
            mock(TemplateRenderService.class),
            coordinatesService,
            new ObjectMapper());

    Task miaTask = mock(Task.class);
    when(miaTask.getId()).thenReturn(16);
    when(miaTask.getType())
        .thenReturn(
            TaskType.builder().id(DomainConstants.Tasks.TASK_TYPE_ID_MORE_INFO_ADVANCED).build());
    when(taskRepository.findById(16)).thenReturn(Optional.of(miaTask));

    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken("viewer@example.org", "n/a", List.of()));
    try {
      MoreInfoAdvancedRenderRequestDto renderRequest = new MoreInfoAdvancedRenderRequestDto();
      renderRequest.setMiaTaskIds(List.of(16));
      renderRequest.setAppId(5);
      renderRequest.setTerId(7);
      renderRequest.setParameters(Map.of("id", "A-1"));

      assertThatThrownBy(() -> service.renderMoreInfoAdvanced(renderRequest, "en"))
          .isInstanceOf(ResponseStatusException.class)
          .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
          .isEqualTo(HttpStatus.FORBIDDEN);
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  @Test
  void renderMoreInfoAdvancedResolvesChildOrderInBackend() {
    TaskRepository taskRepository = mock(TaskRepository.class);
    TemplateRequestCoordinatesService coordinatesService =
        mock(TemplateRequestCoordinatesService.class);
    when(coordinatesService.build(any(), any()))
        .thenReturn(requestCoordinatesWithUserPermission(7));
    TemplateExecutionService service =
        newService(
            taskRepository,
            mock(TaskRelationRepository.class),
            mock(ProxyConfigurationService.class),
            mock(DatabaseConnectionService.class),
            mock(HttpClientFactory.class),
            mock(SystemVariableResolver.class),
            mock(TemplateRenderService.class),
            coordinatesService,
            new ObjectMapper());

    Task miaTask = mock(Task.class);
    when(miaTask.getId()).thenReturn(16);
    when(miaTask.getName()).thenReturn("MIA parent");
    when(miaTask.getType())
        .thenReturn(
            TaskType.builder().id(DomainConstants.Tasks.TASK_TYPE_ID_MORE_INFO_ADVANCED).build());
    when(miaTask.getProperties())
        .thenReturn(Map.of("parentLayout", "scroll", "childTaskOrderIds", List.of(101)));

    Task childTask = mock(Task.class);
    when(childTask.getId()).thenReturn(101);
    when(childTask.getName()).thenReturn("Document");
    when(childTask.getProperties())
        .thenReturn(
            Map.of(
                DomainConstants.Tasks.PROPERTY_SCOPE,
                DomainConstants.Tasks.SCOPE_URL,
                DomainConstants.Tasks.PROPERTY_COMMAND,
                "https://example.org/doc/{code}",
                DomainConstants.Tasks.PROPERTY_PARAMETERS,
                List.of(Map.of("name", "code", "value", "id"))));

    when(taskRepository.findById(16)).thenReturn(Optional.of(miaTask));
    when(taskRepository.findById(101)).thenReturn(Optional.of(childTask));

    MoreInfoAdvancedRenderRequestDto request = new MoreInfoAdvancedRenderRequestDto();
    request.setAppId(5);
    request.setTerId(7);
    request.setMiaTaskIds(List.of(16));
    request.setParameters(Map.of("id", "A-1"));

    MoreInfoAdvancedRenderResponseDto result = service.renderMoreInfoAdvanced(request, "en");

    assertThat(result.getTasks()).hasSize(1);
    assertThat(result.getTasks().get(0).getHtml()).contains("https://example.org/doc/A-1");
  }

  @Test
  void renderMoreInfoAdvancedRejectsBasicViewerHookTask() {
    TaskRepository taskRepository = mock(TaskRepository.class);
    TemplateRequestCoordinatesService coordinatesService =
        mock(TemplateRequestCoordinatesService.class);
    when(coordinatesService.build(any(), any()))
        .thenReturn(requestCoordinatesWithUserPermission(7));
    TemplateExecutionService service =
        newService(
            taskRepository,
            mock(TaskRelationRepository.class),
            mock(ProxyConfigurationService.class),
            mock(DatabaseConnectionService.class),
            mock(HttpClientFactory.class),
            mock(SystemVariableResolver.class),
            mock(TemplateRenderService.class),
            coordinatesService,
            new ObjectMapper());

    Task basicHookTask = mock(Task.class);
    when(basicHookTask.getType())
        .thenReturn(TaskType.builder().id(DomainConstants.Tasks.TASK_TYPE_ID_BASIC).build());
    when(basicHookTask.getUi()).thenReturn(TaskUI.builder().name("sitna.moreInfoAdvanced").build());
    when(taskRepository.findById(32306)).thenReturn(Optional.of(basicHookTask));

    MoreInfoAdvancedRenderRequestDto request = new MoreInfoAdvancedRenderRequestDto();
    request.setAppId(5);
    request.setTerId(7);
    request.setMiaTaskIds(List.of(32306));

    assertThatThrownBy(() -> service.renderMoreInfoAdvanced(request, "en"))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(
            exception -> {
              ResponseStatusException responseStatusException = (ResponseStatusException) exception;
              assertThat(responseStatusException.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
              assertThat(responseStatusException.getReason()).isEqualTo("Task is not a MIA task");
            });
  }

  @Test
  void renderMoreInfoAdvancedResolvesNestedTemplateUrlTaskParametersFromFeatureAttributes() {
    TaskRepository taskRepository = mock(TaskRepository.class);
    TaskRelationRepository taskRelationRepository = mock(TaskRelationRepository.class);
    TemplateRenderService templateRenderService = mock(TemplateRenderService.class);
    TemplateRequestCoordinatesService coordinatesService =
        mock(TemplateRequestCoordinatesService.class);
    SystemVariableResolver systemVariableResolver = mock(SystemVariableResolver.class);
    when(coordinatesService.build(any(), any()))
        .thenReturn(requestCoordinatesWithUserPermission(7));
    when(systemVariableResolver.resolve(eq("https://www.google.com/search?q={dificultat}"), any()))
        .thenReturn("https://www.google.com/search?q={dificultat}");
    TemplateExecutionService service =
        newService(
            taskRepository,
            taskRelationRepository,
            mock(ProxyConfigurationService.class),
            mock(DatabaseConnectionService.class),
            mock(HttpClientFactory.class),
            systemVariableResolver,
            templateRenderService,
            coordinatesService,
            new ObjectMapper());

    Task miaTask = mock(Task.class);
    when(miaTask.getId()).thenReturn(16);
    when(miaTask.getName()).thenReturn("MIA parent");
    when(miaTask.getType())
        .thenReturn(
            TaskType.builder().id(DomainConstants.Tasks.TASK_TYPE_ID_MORE_INFO_ADVANCED).build());
    when(miaTask.getProperties())
        .thenReturn(
            Map.of(
                DomainConstants.Tasks.PROPERTY_PARAMETERS,
                List.of(
                    Map.of(
                        "name",
                        "includedTasks",
                        "type",
                        DomainConstants.Tasks.TYPE_ARRAY,
                        "value",
                        "[{\"id\":201,\"name\":\"Plantilla\",\"order\":0,\"childType\":\"template\"}]"))));

    Task templateTask =
        Task.builder()
            .id(201)
            .properties(
                Map.of(DomainConstants.Tasks.PROPERTY_TEMPLATE_HTML, "<a>{{consulta_url.url}}</a>"))
            .type(
                org.sitmun.domain.task.type.TaskType.builder()
                    .id(DomainConstants.Tasks.TASK_TYPE_ID_TEMPLATE)
                    .build())
            .build();
    Task urlTask =
        Task.builder()
            .id(202)
            .properties(
                Map.of(
                    DomainConstants.Tasks.PROPERTY_SCOPE,
                    DomainConstants.Tasks.SCOPE_URL_QUERY,
                    DomainConstants.Tasks.PROPERTY_COMMAND,
                    "https://www.google.com/search?q={dificultat}",
                    DomainConstants.Tasks.PROPERTY_PARAMETERS,
                    List.of(
                        new LinkedHashMap<>(
                            Map.of("name", "dificultat", "type", "Query parameter")))))
            .build();

    when(taskRepository.findById(16)).thenReturn(Optional.of(miaTask));
    when(taskRepository.findById(201)).thenReturn(Optional.of(templateTask));
    when(taskRelationRepository.findByTaskId(201))
        .thenReturn(
            List.of(
                TaskRelation.builder()
                    .id(1)
                    .task(templateTask)
                    .relationType("template-task")
                    .referenceAlias("consulta_url")
                    .relatedTask(urlTask)
                    .build()));
    when(templateRenderService.renderPreview(
            eq("<a>{{consulta_url.url}}</a>"), any(), any(), any()))
        .thenAnswer(
            invocation -> {
              Map<String, Object> context = invocation.getArgument(1);
              Map<String, Object> childContext = (Map<String, Object>) context.get("consulta_url");
              return TemplatePreviewResponseDto.builder()
                  .html("<a>" + childContext.get("url") + "</a>")
                  .placeholders(List.of())
                  .build();
            });

    MoreInfoAdvancedRenderRequestDto request = new MoreInfoAdvancedRenderRequestDto();
    request.setAppId(5);
    request.setTerId(7);
    request.setMiaTaskIds(List.of(16));
    request.setParameters(Map.of("dificultat", "Mitjana"));

    MoreInfoAdvancedRenderResponseDto result = service.renderMoreInfoAdvanced(request, "en");

    assertThat(result.getTasks()).hasSize(1);
    assertThat(result.getTasks().get(0).getHtml())
        .contains("https://www.google.com/search?q=Mitjana");
  }

  @Test
  void renderMoreInfoAdvancedResolvesTemplateParametersAndNestedChildParametersFromMappings() {
    TaskRepository taskRepository = mock(TaskRepository.class);
    TaskRelationRepository taskRelationRepository = mock(TaskRelationRepository.class);
    TemplateRenderService templateRenderService = mock(TemplateRenderService.class);
    TemplateRequestCoordinatesService coordinatesService =
        mock(TemplateRequestCoordinatesService.class);
    SystemVariableResolver systemVariableResolver = mock(SystemVariableResolver.class);
    when(coordinatesService.build(any(), any()))
        .thenReturn(requestCoordinatesWithUserPermission(7));
    when(systemVariableResolver.resolve(eq("https://example.org/layers/{innerParam}"), any()))
        .thenReturn("https://example.org/layers/{innerParam}");
    TemplateExecutionService service =
        newService(
            taskRepository,
            taskRelationRepository,
            mock(ProxyConfigurationService.class),
            mock(DatabaseConnectionService.class),
            mock(HttpClientFactory.class),
            systemVariableResolver,
            templateRenderService,
            coordinatesService,
            new ObjectMapper());

    Task miaTask = mock(Task.class);
    when(miaTask.getId()).thenReturn(16);
    when(miaTask.getName()).thenReturn("MIA parent");
    when(miaTask.getType())
        .thenReturn(
            TaskType.builder().id(DomainConstants.Tasks.TASK_TYPE_ID_MORE_INFO_ADVANCED).build());
    when(miaTask.getProperties())
        .thenReturn(
            Map.of(
                "parentLayout",
                "scroll",
                "childTaskOrderIds",
                List.of(401),
                "childTaskParameters",
                Map.of("401", Map.of("title", "titleAttr")),
                "templateChildTaskParameters",
                Map.of("401", Map.of("402", Map.of("innerParam", "layerid")))));

    Task templateTask =
        Task.builder()
            .id(401)
            .name("Plantilla")
            .properties(
                Map.of(
                    DomainConstants.Tasks.PROPERTY_TEMPLATE_HTML,
                    "<h1>{{$title}}</h1><a>{{consulta_url.url}}</a>"))
            .type(TaskType.builder().id(DomainConstants.Tasks.TASK_TYPE_ID_TEMPLATE).build())
            .build();
    Task urlTask =
        Task.builder()
            .id(402)
            .properties(
                Map.of(
                    DomainConstants.Tasks.PROPERTY_SCOPE,
                    DomainConstants.Tasks.SCOPE_URL_QUERY,
                    DomainConstants.Tasks.PROPERTY_COMMAND,
                    "https://example.org/layers/{innerParam}"))
            .build();

    when(taskRepository.findById(16)).thenReturn(Optional.of(miaTask));
    when(taskRepository.findById(401)).thenReturn(Optional.of(templateTask));
    when(taskRelationRepository.findByTaskId(401))
        .thenReturn(
            List.of(
                TaskRelation.builder()
                    .id(1)
                    .task(templateTask)
                    .relationType("template-task")
                    .referenceAlias("consulta_url")
                    .relatedTask(urlTask)
                    .build()));
    when(templateRenderService.renderPreview(
            eq("<h1>{{$title}}</h1><a>{{consulta_url.url}}</a>"), any(), any(), any()))
        .thenAnswer(
            invocation -> {
              Map<String, Object> context = invocation.getArgument(1);
              Map<String, Object> childContext = (Map<String, Object>) context.get("consulta_url");
              return TemplatePreviewResponseDto.builder()
                  .html(
                      "<h1>"
                          + context.get("$title")
                          + "</h1><a>"
                          + childContext.get("url")
                          + "</a>")
                  .placeholders(List.of())
                  .build();
            });

    MoreInfoAdvancedRenderRequestDto request = new MoreInfoAdvancedRenderRequestDto();
    request.setAppId(5);
    request.setTerId(7);
    request.setMiaTaskIds(List.of(16));
    request.setParameters(Map.of("titleAttr", "Layer title", "layerid", "roads"));

    MoreInfoAdvancedRenderResponseDto result = service.renderMoreInfoAdvanced(request, "en");

    assertThat(result.getTasks()).hasSize(1);
    assertThat(result.getTasks().get(0).getHtml())
        .contains("<h1>Layer title</h1>")
        .contains("https://example.org/layers/roads");
  }

  @Test
  void renderMoreInfoAdvancedKeepsTemplateDefaultValueWhenNoDirectMappingExists() {
    TaskRepository taskRepository = mock(TaskRepository.class);
    TemplateRenderService templateRenderService = mock(TemplateRenderService.class);
    TemplateRequestCoordinatesService coordinatesService =
        mock(TemplateRequestCoordinatesService.class);
    when(coordinatesService.build(any(), any()))
        .thenReturn(requestCoordinatesWithUserPermission(7));

    TemplateExecutionService service =
        newService(
            taskRepository,
            mock(TaskRelationRepository.class),
            mock(ProxyConfigurationService.class),
            mock(DatabaseConnectionService.class),
            mock(HttpClientFactory.class),
            mock(SystemVariableResolver.class),
            templateRenderService,
            coordinatesService,
            new ObjectMapper());

    Task miaTask = mock(Task.class);
    when(miaTask.getId()).thenReturn(16);
    when(miaTask.getName()).thenReturn("MIA parent");
    when(miaTask.getType())
        .thenReturn(
            TaskType.builder().id(DomainConstants.Tasks.TASK_TYPE_ID_MORE_INFO_ADVANCED).build());
    when(miaTask.getProperties())
        .thenReturn(Map.of("parentLayout", "scroll", "childTaskOrderIds", List.of(701)));

    Task templateTask =
        Task.builder()
            .id(701)
            .name("Plantilla")
            .properties(
                Map.of(
                    DomainConstants.Tasks.PROPERTY_TEMPLATE_HTML,
                    "<h1>{{$title}}</h1>",
                    DomainConstants.Tasks.PROPERTY_PARAMETERS,
                    List.of(
                        Map.of(
                            "name",
                            "title",
                            "type",
                            DomainConstants.Tasks.TYPE_STRING,
                            "value",
                            "Default title"))))
            .type(TaskType.builder().id(DomainConstants.Tasks.TASK_TYPE_ID_TEMPLATE).build())
            .build();

    when(taskRepository.findById(16)).thenReturn(Optional.of(miaTask));
    when(taskRepository.findById(701)).thenReturn(Optional.of(templateTask));
    when(templateRenderService.renderPreview(eq("<h1>{{$title}}</h1>"), any(), any(), any()))
        .thenAnswer(
            invocation -> {
              Map<String, Object> context = invocation.getArgument(1);
              return TemplatePreviewResponseDto.builder()
                  .html("<h1>" + context.get("$title") + "</h1>")
                  .placeholders(List.of())
                  .build();
            });

    MoreInfoAdvancedRenderRequestDto request = new MoreInfoAdvancedRenderRequestDto();
    request.setAppId(5);
    request.setTerId(7);
    request.setMiaTaskIds(List.of(16));
    request.setParameters(Map.of("Default title", "Wrong feature value"));

    MoreInfoAdvancedRenderResponseDto result = service.renderMoreInfoAdvanced(request, "en");

    assertThat(result.getTasks()).hasSize(1);
    assertThat(result.getTasks().get(0).getHtml()).contains("<h1>Default title</h1>");
  }

  @Test
  void renderMoreInfoAdvancedPrefersExplicitMappingOverTemplateDefaultValue() {
    TaskRepository taskRepository = mock(TaskRepository.class);
    TemplateRenderService templateRenderService = mock(TemplateRenderService.class);
    TemplateRequestCoordinatesService coordinatesService =
        mock(TemplateRequestCoordinatesService.class);
    when(coordinatesService.build(any(), any()))
        .thenReturn(requestCoordinatesWithUserPermission(7));

    TemplateExecutionService service =
        newService(
            taskRepository,
            mock(TaskRelationRepository.class),
            mock(ProxyConfigurationService.class),
            mock(DatabaseConnectionService.class),
            mock(HttpClientFactory.class),
            mock(SystemVariableResolver.class),
            templateRenderService,
            coordinatesService,
            new ObjectMapper());

    Task miaTask = mock(Task.class);
    when(miaTask.getId()).thenReturn(16);
    when(miaTask.getName()).thenReturn("MIA parent");
    when(miaTask.getType())
        .thenReturn(
            TaskType.builder().id(DomainConstants.Tasks.TASK_TYPE_ID_MORE_INFO_ADVANCED).build());
    when(miaTask.getProperties())
        .thenReturn(
            Map.of(
                "parentLayout",
                "scroll",
                "childTaskOrderIds",
                List.of(702),
                "childTaskParameters",
                Map.of("702", Map.of("title", "featureTitle"))));

    Task templateTask =
        Task.builder()
            .id(702)
            .name("Plantilla")
            .properties(
                Map.of(
                    DomainConstants.Tasks.PROPERTY_TEMPLATE_HTML,
                    "<h1>{{$title}}</h1>",
                    DomainConstants.Tasks.PROPERTY_PARAMETERS,
                    List.of(
                        Map.of(
                            "name",
                            "title",
                            "type",
                            DomainConstants.Tasks.TYPE_STRING,
                            "value",
                            "Default title"))))
            .type(TaskType.builder().id(DomainConstants.Tasks.TASK_TYPE_ID_TEMPLATE).build())
            .build();

    when(taskRepository.findById(16)).thenReturn(Optional.of(miaTask));
    when(taskRepository.findById(702)).thenReturn(Optional.of(templateTask));
    when(templateRenderService.renderPreview(eq("<h1>{{$title}}</h1>"), any(), any(), any()))
        .thenAnswer(
            invocation -> {
              Map<String, Object> context = invocation.getArgument(1);
              return TemplatePreviewResponseDto.builder()
                  .html("<h1>" + context.get("$title") + "</h1>")
                  .placeholders(List.of())
                  .build();
            });

    MoreInfoAdvancedRenderRequestDto request = new MoreInfoAdvancedRenderRequestDto();
    request.setAppId(5);
    request.setTerId(7);
    request.setMiaTaskIds(List.of(16));
    request.setParameters(
        Map.of("featureTitle", "Mapped title", "Default title", "Wrong feature value"));

    MoreInfoAdvancedRenderResponseDto result = service.renderMoreInfoAdvanced(request, "en");

    assertThat(result.getTasks()).hasSize(1);
    assertThat(result.getTasks().get(0).getHtml())
        .contains("<h1>Mapped title</h1>")
        .doesNotContain("Wrong feature value");
  }

  @Test
  void renderMoreInfoAdvancedAcceptsNumericTemplateChildTaskParameterKeys() {
    TaskRepository taskRepository = mock(TaskRepository.class);
    TaskRelationRepository taskRelationRepository = mock(TaskRelationRepository.class);
    TemplateRenderService templateRenderService = mock(TemplateRenderService.class);
    TemplateRequestCoordinatesService coordinatesService =
        mock(TemplateRequestCoordinatesService.class);
    SystemVariableResolver systemVariableResolver = mock(SystemVariableResolver.class);
    when(coordinatesService.build(any(), any()))
        .thenReturn(requestCoordinatesWithUserPermission(7));
    when(systemVariableResolver.resolve(eq("https://example.org/items/{innerParam}"), any()))
        .thenReturn("https://example.org/items/{innerParam}");
    TemplateExecutionService service =
        newService(
            taskRepository,
            taskRelationRepository,
            mock(ProxyConfigurationService.class),
            mock(DatabaseConnectionService.class),
            mock(HttpClientFactory.class),
            systemVariableResolver,
            templateRenderService,
            coordinatesService,
            new ObjectMapper());

    Task miaTask = mock(Task.class);
    when(miaTask.getId()).thenReturn(16);
    when(miaTask.getName()).thenReturn("MIA parent");
    when(miaTask.getType())
        .thenReturn(
            TaskType.builder().id(DomainConstants.Tasks.TASK_TYPE_ID_MORE_INFO_ADVANCED).build());
    when(miaTask.getProperties())
        .thenReturn(
            Map.of(
                "parentLayout",
                "scroll",
                "childTaskOrderIds",
                List.of(501),
                "templateChildTaskParameters",
                Map.of(501, Map.of(502, Map.of("innerParam", "featureCode")))));

    Task templateTask =
        Task.builder()
            .id(501)
            .name("Plantilla")
            .properties(
                Map.of(DomainConstants.Tasks.PROPERTY_TEMPLATE_HTML, "<a>{{consulta_url.url}}</a>"))
            .type(TaskType.builder().id(DomainConstants.Tasks.TASK_TYPE_ID_TEMPLATE).build())
            .build();
    Task urlTask =
        Task.builder()
            .id(502)
            .properties(
                Map.of(
                    DomainConstants.Tasks.PROPERTY_SCOPE,
                    DomainConstants.Tasks.SCOPE_URL_QUERY,
                    DomainConstants.Tasks.PROPERTY_COMMAND,
                    "https://example.org/items/{innerParam}"))
            .build();

    when(taskRepository.findById(16)).thenReturn(Optional.of(miaTask));
    when(taskRepository.findById(501)).thenReturn(Optional.of(templateTask));
    when(taskRelationRepository.findByTaskId(501))
        .thenReturn(
            List.of(
                TaskRelation.builder()
                    .id(1)
                    .task(templateTask)
                    .relationType("template-task")
                    .referenceAlias("consulta_url")
                    .relatedTask(urlTask)
                    .build()));
    when(templateRenderService.renderPreview(
            eq("<a>{{consulta_url.url}}</a>"), any(), any(), any()))
        .thenAnswer(
            invocation -> {
              Map<String, Object> context = invocation.getArgument(1);
              Map<String, Object> childContext = (Map<String, Object>) context.get("consulta_url");
              return TemplatePreviewResponseDto.builder()
                  .html("<a>" + childContext.get("url") + "</a>")
                  .placeholders(List.of())
                  .build();
            });

    MoreInfoAdvancedRenderRequestDto request = new MoreInfoAdvancedRenderRequestDto();
    request.setAppId(5);
    request.setTerId(7);
    request.setMiaTaskIds(List.of(16));
    request.setParameters(Map.of("featureCode", "abc"));

    MoreInfoAdvancedRenderResponseDto result = service.renderMoreInfoAdvanced(request, "en");

    assertThat(result.getTasks().get(0).getHtml()).contains("https://example.org/items/abc");
  }

  @Test
  void renderMoreInfoAdvancedKeepsExplicitInnerChildMappingOverNestedMapping() {
    TaskRepository taskRepository = mock(TaskRepository.class);
    TaskRelationRepository taskRelationRepository = mock(TaskRelationRepository.class);
    TemplateRenderService templateRenderService = mock(TemplateRenderService.class);
    TemplateRequestCoordinatesService coordinatesService =
        mock(TemplateRequestCoordinatesService.class);
    SystemVariableResolver systemVariableResolver = mock(SystemVariableResolver.class);
    when(coordinatesService.build(any(), any()))
        .thenReturn(requestCoordinatesWithUserPermission(7));
    when(systemVariableResolver.resolve(eq("https://example.org/items/{innerParam}"), any()))
        .thenReturn("https://example.org/items/{innerParam}");
    TemplateExecutionService service =
        newService(
            taskRepository,
            taskRelationRepository,
            mock(ProxyConfigurationService.class),
            mock(DatabaseConnectionService.class),
            mock(HttpClientFactory.class),
            systemVariableResolver,
            templateRenderService,
            coordinatesService,
            new ObjectMapper());

    Task miaTask = mock(Task.class);
    when(miaTask.getId()).thenReturn(16);
    when(miaTask.getName()).thenReturn("MIA parent");
    when(miaTask.getType())
        .thenReturn(
            TaskType.builder().id(DomainConstants.Tasks.TASK_TYPE_ID_MORE_INFO_ADVANCED).build());
    when(miaTask.getProperties())
        .thenReturn(
            Map.of(
                DomainConstants.Tasks.PROPERTY_PARAMETERS,
                List.of(
                    Map.of(
                        "name",
                        "includedTasks",
                        "type",
                        DomainConstants.Tasks.TYPE_ARRAY,
                        "value",
                        "[{\"id\":601,\"name\":\"Plantilla\",\"order\":0,\"childType\":\"template\","
                            + "\"childTaskParameters\":{\"602\":{\"innerParam\":\"explicitAttr\"}},"
                            + "\"templateChildTaskParameters\":{\"602\":{\"innerParam\":\"nestedAttr\"}}}]"))));

    Task templateTask =
        Task.builder()
            .id(601)
            .properties(
                Map.of(DomainConstants.Tasks.PROPERTY_TEMPLATE_HTML, "<a>{{consulta_url.url}}</a>"))
            .type(TaskType.builder().id(DomainConstants.Tasks.TASK_TYPE_ID_TEMPLATE).build())
            .build();
    Task urlTask =
        Task.builder()
            .id(602)
            .properties(
                Map.of(
                    DomainConstants.Tasks.PROPERTY_SCOPE,
                    DomainConstants.Tasks.SCOPE_URL_QUERY,
                    DomainConstants.Tasks.PROPERTY_COMMAND,
                    "https://example.org/items/{innerParam}"))
            .build();

    when(taskRepository.findById(16)).thenReturn(Optional.of(miaTask));
    when(taskRepository.findById(601)).thenReturn(Optional.of(templateTask));
    when(taskRelationRepository.findByTaskId(601))
        .thenReturn(
            List.of(
                TaskRelation.builder()
                    .id(1)
                    .task(templateTask)
                    .relationType("template-task")
                    .referenceAlias("consulta_url")
                    .relatedTask(urlTask)
                    .build()));
    when(templateRenderService.renderPreview(
            eq("<a>{{consulta_url.url}}</a>"), any(), any(), any()))
        .thenAnswer(
            invocation -> {
              Map<String, Object> context = invocation.getArgument(1);
              Map<String, Object> childContext = (Map<String, Object>) context.get("consulta_url");
              return TemplatePreviewResponseDto.builder()
                  .html("<a>" + childContext.get("url") + "</a>")
                  .placeholders(List.of())
                  .build();
            });

    MoreInfoAdvancedRenderRequestDto request = new MoreInfoAdvancedRenderRequestDto();
    request.setAppId(5);
    request.setTerId(7);
    request.setMiaTaskIds(List.of(16));
    request.setParameters(Map.of("explicitAttr", "explicit-value", "nestedAttr", "nested-value"));

    MoreInfoAdvancedRenderResponseDto result = service.renderMoreInfoAdvanced(request, "en");

    assertThat(result.getTasks().get(0).getHtml())
        .contains("https://example.org/items/explicit-value")
        .doesNotContain("nested-value");
  }

  @Test
  void renderMoreInfoAdvancedResolvesUrlParametersInsideRecursiveNestedTemplate() {
    TaskRepository taskRepository = mock(TaskRepository.class);
    TaskRelationRepository taskRelationRepository = mock(TaskRelationRepository.class);
    TemplateRenderService templateRenderService = mock(TemplateRenderService.class);
    TemplateRequestCoordinatesService coordinatesService =
        mock(TemplateRequestCoordinatesService.class);
    SystemVariableResolver systemVariableResolver = mock(SystemVariableResolver.class);
    when(coordinatesService.build(any(), any()))
        .thenReturn(requestCoordinatesWithUserPermission(7));
    when(systemVariableResolver.resolve(eq("https://www.google.com/search?q={dificultat}"), any()))
        .thenReturn("https://www.google.com/search?q={dificultat}");
    TemplateExecutionService service =
        newService(
            taskRepository,
            taskRelationRepository,
            mock(ProxyConfigurationService.class),
            mock(DatabaseConnectionService.class),
            mock(HttpClientFactory.class),
            systemVariableResolver,
            templateRenderService,
            coordinatesService,
            new ObjectMapper());

    Task miaTask = mock(Task.class);
    when(miaTask.getId()).thenReturn(16);
    when(miaTask.getName()).thenReturn("MIA parent");
    when(miaTask.getType())
        .thenReturn(
            TaskType.builder().id(DomainConstants.Tasks.TASK_TYPE_ID_MORE_INFO_ADVANCED).build());
    when(miaTask.getProperties())
        .thenReturn(
            Map.of(
                DomainConstants.Tasks.PROPERTY_PARAMETERS,
                List.of(
                    Map.of(
                        "name",
                        "includedTasks",
                        "type",
                        DomainConstants.Tasks.TYPE_ARRAY,
                        "value",
                        "[{\"id\":301,\"name\":\"Plantilla\",\"order\":0,\"childType\":\"template\"}]"))));

    Task parentTemplate =
        Task.builder()
            .id(301)
            .properties(
                Map.of(
                    DomainConstants.Tasks.PROPERTY_TEMPLATE_HTML,
                    "<section>{{plantilla_hija.html}}</section>"))
            .type(
                org.sitmun.domain.task.type.TaskType.builder()
                    .id(DomainConstants.Tasks.TASK_TYPE_ID_TEMPLATE)
                    .build())
            .build();
    Task nestedTemplate =
        Task.builder()
            .id(302)
            .properties(
                Map.of(DomainConstants.Tasks.PROPERTY_TEMPLATE_HTML, "<a>{{consulta_url.url}}</a>"))
            .type(
                org.sitmun.domain.task.type.TaskType.builder()
                    .id(DomainConstants.Tasks.TASK_TYPE_ID_TEMPLATE)
                    .build())
            .build();
    Task urlTask =
        Task.builder()
            .id(303)
            .properties(
                Map.of(
                    DomainConstants.Tasks.PROPERTY_SCOPE,
                    DomainConstants.Tasks.SCOPE_URL_QUERY,
                    DomainConstants.Tasks.PROPERTY_COMMAND,
                    "https://www.google.com/search?q={dificultat}",
                    DomainConstants.Tasks.PROPERTY_PARAMETERS,
                    List.of(
                        new LinkedHashMap<>(
                            Map.of("name", "dificultat", "type", "Query parameter")))))
            .build();

    when(taskRepository.findById(16)).thenReturn(Optional.of(miaTask));
    when(taskRepository.findById(301)).thenReturn(Optional.of(parentTemplate));
    when(taskRelationRepository.findByTaskId(301))
        .thenReturn(
            List.of(
                TaskRelation.builder()
                    .id(1)
                    .task(parentTemplate)
                    .relationType("template-nested")
                    .referenceAlias("plantilla_hija")
                    .relatedTask(nestedTemplate)
                    .build()));
    when(taskRelationRepository.findByTaskId(302))
        .thenReturn(
            List.of(
                TaskRelation.builder()
                    .id(2)
                    .task(nestedTemplate)
                    .relationType("template-task")
                    .referenceAlias("consulta_url")
                    .relatedTask(urlTask)
                    .build()));
    when(templateRenderService.renderPreview(
            eq("<a>{{consulta_url.url}}</a>"), any(), any(), any()))
        .thenAnswer(
            invocation -> {
              Map<String, Object> context = invocation.getArgument(1);
              Map<String, Object> childContext = (Map<String, Object>) context.get("consulta_url");
              return TemplatePreviewResponseDto.builder()
                  .html("<a>" + childContext.get("url") + "</a>")
                  .placeholders(List.of())
                  .build();
            });
    when(templateRenderService.renderPreview(
            eq("<section>{{plantilla_hija.html}}</section>"), any(), any(), any()))
        .thenAnswer(
            invocation -> {
              Map<String, Object> context = invocation.getArgument(1);
              Map<String, Object> childContext =
                  (Map<String, Object>) context.get("plantilla_hija");
              return TemplatePreviewResponseDto.builder()
                  .html("<section>" + childContext.get("html") + "</section>")
                  .placeholders(List.of())
                  .build();
            });

    MoreInfoAdvancedRenderRequestDto request = new MoreInfoAdvancedRenderRequestDto();
    request.setAppId(5);
    request.setTerId(7);
    request.setMiaTaskIds(List.of(16));
    request.setParameters(Map.of("dificultat", "Mitjana"));

    MoreInfoAdvancedRenderResponseDto result = service.renderMoreInfoAdvanced(request, "en");

    assertThat(result.getTasks()).hasSize(1);
    assertThat(result.getTasks().get(0).getHtml())
        .contains("https://www.google.com/search?q=Mitjana");
  }

  @Test
  void renderMoreInfoAdvancedKeepsTemplateHtmlWhenLinkedApiChildFails() throws IOException {
    TaskRepository taskRepository = mock(TaskRepository.class);
    TaskRelationRepository taskRelationRepository = mock(TaskRelationRepository.class);
    ProxyConfigurationService proxyConfigurationService = mock(ProxyConfigurationService.class);
    HttpClientFactory httpClientFactory = mock(HttpClientFactory.class);
    SystemVariableResolver systemVariableResolver = mock(SystemVariableResolver.class);
    TemplateRenderService templateRenderService = mock(TemplateRenderService.class);
    TemplateRequestCoordinatesService coordinatesService =
        mock(TemplateRequestCoordinatesService.class);
    when(coordinatesService.build(any(), any()))
        .thenReturn(requestCoordinatesWithUserPermission(7));
    when(systemVariableResolver.resolve(eq("https://api.example.org/items"), any()))
        .thenReturn("https://api.example.org/items");
    TemplateExecutionService service =
        newService(
            taskRepository,
            taskRelationRepository,
            proxyConfigurationService,
            mock(DatabaseConnectionService.class),
            httpClientFactory,
            systemVariableResolver,
            templateRenderService,
            coordinatesService,
            new ObjectMapper());

    Task miaTask = mock(Task.class);
    when(miaTask.getId()).thenReturn(16);
    when(miaTask.getName()).thenReturn("MIA parent");
    when(miaTask.getType())
        .thenReturn(
            TaskType.builder().id(DomainConstants.Tasks.TASK_TYPE_ID_MORE_INFO_ADVANCED).build());
    when(miaTask.getProperties())
        .thenReturn(
            Map.of(
                DomainConstants.Tasks.PROPERTY_PARAMETERS,
                List.of(
                    Map.of(
                        "name",
                        "includedTasks",
                        "type",
                        DomainConstants.Tasks.TYPE_ARRAY,
                        "value",
                        "[{\"id\":401,\"name\":\"Plantilla 1\",\"order\":0,\"childType\":\"template\"}]"))));

    Task templateTask =
        Task.builder()
            .id(401)
            .properties(
                Map.of(
                    DomainConstants.Tasks.PROPERTY_TEMPLATE_HTML,
                    "<article>{{consulta_api.html}} {{consulta_api.value}}</article>"))
            .type(
                org.sitmun.domain.task.type.TaskType.builder()
                    .id(DomainConstants.Tasks.TASK_TYPE_ID_TEMPLATE)
                    .build())
            .build();
    Task apiTask =
        Task.builder()
            .id(402)
            .properties(
                Map.of(
                    DomainConstants.Tasks.PROPERTY_SCOPE,
                    DomainConstants.Tasks.SCOPE_WEB_API_QUERY,
                    DomainConstants.Tasks.PROPERTY_COMMAND,
                    "https://api.example.org/items"))
            .build();

    when(taskRepository.findById(16)).thenReturn(Optional.of(miaTask));
    when(taskRepository.findById(401)).thenReturn(Optional.of(templateTask));
    when(taskRelationRepository.findByTaskId(401))
        .thenReturn(
            List.of(
                TaskRelation.builder()
                    .id(1)
                    .task(templateTask)
                    .relationType("template-task")
                    .referenceAlias("consulta_api")
                    .relatedTask(apiTask)
                    .build()));
    WmsPayloadDto payload =
        WmsPayloadDto.builder().uri("https://api.example.org/items").method("GET").build();
    ConfigProxyDto config = ConfigProxyDto.builder().type("API").payload(payload).build();
    when(proxyConfigurationService.getConfiguration(any(), eq(0L), any())).thenReturn(config);
    Response response =
        new Response.Builder()
            .request(new Request.Builder().url("https://api.example.org/items").build())
            .protocol(Protocol.HTTP_1_1)
            .code(500)
            .message("Internal Server Error")
            .body(
                ResponseBody.create(
                    "{\"error\":\"upstream failed\"}", okhttp3.MediaType.parse("application/json")))
            .build();
    when(httpClientFactory.executeRequest(any())).thenReturn(response);
    when(templateRenderService.renderPreview(
            eq("<article>{{consulta_api.html}} {{consulta_api.value}}</article>"),
            any(),
            any(),
            any()))
        .thenAnswer(
            invocation -> {
              Map<String, Object> context = invocation.getArgument(1);
              Map<String, Object> childContext = (Map<String, Object>) context.get("consulta_api");
              return TemplatePreviewResponseDto.builder()
                  .html(
                      "<article>"
                          + childContext.get("html")
                          + " "
                          + childContext.get("value")
                          + "</article>")
                  .placeholders(List.of())
                  .build();
            });

    MoreInfoAdvancedRenderRequestDto request = new MoreInfoAdvancedRenderRequestDto();
    request.setAppId(5);
    request.setTerId(7);
    request.setMiaTaskIds(List.of(16));
    request.setParameters(Map.of());

    MoreInfoAdvancedRenderResponseDto result = service.renderMoreInfoAdvanced(request, "en");

    assertThat(result.getTasks()).hasSize(1);
    assertThat(result.getTasks().get(0).getHtml())
        .contains("sitmun-template-child-error")
        .contains("API task returned HTTP 500")
        .contains("[error: API task returned HTTP 500]")
        .doesNotContain("Error ejecutando tarea: Plantilla 1");
  }

  @Test
  void renderMoreInfoAdvancedKeepsTemplateHtmlWhenLinkedChildHasBadRequestFailure() {
    TaskRepository taskRepository = mock(TaskRepository.class);
    TaskRelationRepository taskRelationRepository = mock(TaskRelationRepository.class);
    TemplateRenderService templateRenderService = mock(TemplateRenderService.class);
    TemplateRequestCoordinatesService coordinatesService =
        mock(TemplateRequestCoordinatesService.class);
    when(coordinatesService.build(any(), any()))
        .thenReturn(requestCoordinatesWithUserPermission(7));
    TemplateExecutionService service =
        newService(
            taskRepository,
            taskRelationRepository,
            mock(ProxyConfigurationService.class),
            mock(DatabaseConnectionService.class),
            mock(HttpClientFactory.class),
            mock(SystemVariableResolver.class),
            templateRenderService,
            coordinatesService,
            new ObjectMapper());

    Task miaTask = mock(Task.class);
    when(miaTask.getId()).thenReturn(16);
    when(miaTask.getName()).thenReturn("MIA parent");
    when(miaTask.getType())
        .thenReturn(
            TaskType.builder().id(DomainConstants.Tasks.TASK_TYPE_ID_MORE_INFO_ADVANCED).build());
    when(miaTask.getProperties())
        .thenReturn(
            Map.of(
                DomainConstants.Tasks.PROPERTY_PARAMETERS,
                List.of(
                    Map.of(
                        "name",
                        "includedTasks",
                        "type",
                        DomainConstants.Tasks.TYPE_ARRAY,
                        "value",
                        "[{\"id\":451,\"name\":\"Plantilla BAD_REQUEST\",\"order\":0,\"childType\":\"template\"}]"))));

    Task templateTask =
        Task.builder()
            .id(451)
            .properties(
                Map.of(
                    DomainConstants.Tasks.PROPERTY_TEMPLATE_HTML,
                    "<article>{{bad_child.html}} {{bad_child.value}}</article>"))
            .type(
                org.sitmun.domain.task.type.TaskType.builder()
                    .id(DomainConstants.Tasks.TASK_TYPE_ID_TEMPLATE)
                    .build())
            .build();
    Task unsupportedTask =
        Task.builder()
            .id(452)
            .name("<img src=x onerror=alert(1)>")
            .properties(Map.of(DomainConstants.Tasks.PROPERTY_SCOPE, "unsupported<script>"))
            .build();

    when(taskRepository.findById(16)).thenReturn(Optional.of(miaTask));
    when(taskRepository.findById(451)).thenReturn(Optional.of(templateTask));
    when(taskRelationRepository.findByTaskId(451))
        .thenReturn(
            List.of(
                TaskRelation.builder()
                    .id(1)
                    .task(templateTask)
                    .relationType("template-task")
                    .referenceAlias("bad_child")
                    .relatedTask(unsupportedTask)
                    .build()));
    when(templateRenderService.renderPreview(
            eq("<article>{{bad_child.html}} {{bad_child.value}}</article>"), any(), any(), any()))
        .thenAnswer(
            invocation -> {
              Map<String, Object> context = invocation.getArgument(1);
              Map<String, Object> childContext = (Map<String, Object>) context.get("bad_child");
              return TemplatePreviewResponseDto.builder()
                  .html(
                      "<article>"
                          + childContext.get("html")
                          + " "
                          + childContext.get("value")
                          + "</article>")
                  .placeholders(List.of())
                  .build();
            });

    MoreInfoAdvancedRenderRequestDto request = new MoreInfoAdvancedRenderRequestDto();
    request.setAppId(5);
    request.setTerId(7);
    request.setMiaTaskIds(List.of(16));
    request.setParameters(Map.of());

    MoreInfoAdvancedRenderResponseDto result = service.renderMoreInfoAdvanced(request, "en");

    assertThat(result.getTasks()).hasSize(1);
    assertThat(result.getTasks().get(0).getHtml())
        .contains("sitmun-template-child-error")
        .contains("&lt;img src&#x3D;x onerror&#x3D;alert(1)&gt;")
        .contains("Unsupported linked task scope: unsupported&lt;script&gt;")
        .contains("[error: Unsupported linked task scope: unsupported&lt;script&gt;]")
        .doesNotContain("<img src=x onerror=alert(1)>")
        .doesNotContain("unsupported<script>")
        .doesNotContain("Error ejecutando tarea: Plantilla BAD_REQUEST");
  }

  @Test
  void renderMoreInfoAdvancedPropagatesParentTemplateRenderBadRequest() {
    TaskRepository taskRepository = mock(TaskRepository.class);
    TaskRelationRepository taskRelationRepository = mock(TaskRelationRepository.class);
    TemplateRenderService templateRenderService = mock(TemplateRenderService.class);
    TemplateRequestCoordinatesService coordinatesService =
        mock(TemplateRequestCoordinatesService.class);
    when(coordinatesService.build(any(), any()))
        .thenReturn(requestCoordinatesWithUserPermission(7));
    TemplateExecutionService service =
        newService(
            taskRepository,
            taskRelationRepository,
            mock(ProxyConfigurationService.class),
            mock(DatabaseConnectionService.class),
            mock(HttpClientFactory.class),
            mock(SystemVariableResolver.class),
            templateRenderService,
            coordinatesService,
            new ObjectMapper());

    Task miaTask = mock(Task.class);
    when(miaTask.getId()).thenReturn(16);
    when(miaTask.getName()).thenReturn("MIA parent");
    when(miaTask.getType())
        .thenReturn(
            TaskType.builder().id(DomainConstants.Tasks.TASK_TYPE_ID_MORE_INFO_ADVANCED).build());
    when(miaTask.getProperties())
        .thenReturn(
            Map.of(
                DomainConstants.Tasks.PROPERTY_PARAMETERS,
                List.of(
                    Map.of(
                        "name",
                        "includedTasks",
                        "type",
                        DomainConstants.Tasks.TYPE_ARRAY,
                        "value",
                        "[{\"id\":471,\"name\":\"Plantilla invalida\",\"order\":0,\"childType\":\"template\"}]"))));
    Task templateTask =
        Task.builder()
            .id(471)
            .properties(Map.of(DomainConstants.Tasks.PROPERTY_TEMPLATE_HTML, "{{#if broken}}"))
            .type(
                org.sitmun.domain.task.type.TaskType.builder()
                    .id(DomainConstants.Tasks.TASK_TYPE_ID_TEMPLATE)
                    .build())
            .build();

    when(taskRepository.findById(16)).thenReturn(Optional.of(miaTask));
    when(taskRepository.findById(471)).thenReturn(Optional.of(templateTask));
    when(taskRelationRepository.findByTaskId(471)).thenReturn(List.of());
    when(templateRenderService.renderPreview(eq("{{#if broken}}"), any(), any(), any()))
        .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid template syntax"));

    MoreInfoAdvancedRenderRequestDto request = new MoreInfoAdvancedRenderRequestDto();
    request.setAppId(5);
    request.setTerId(7);
    request.setMiaTaskIds(List.of(16));
    request.setParameters(Map.of());

    assertThatThrownBy(() -> service.renderMoreInfoAdvanced(request, "en"))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(
            exception -> {
              ResponseStatusException responseStatusException = (ResponseStatusException) exception;
              assertThat(responseStatusException.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
              assertThat(responseStatusException.getReason()).isEqualTo("Invalid template syntax");
            });
  }

  @Test
  void renderMoreInfoAdvancedRequiresAppIdAndTerId() {
    TemplateRequestCoordinatesService coordinatesService =
        mock(TemplateRequestCoordinatesService.class);
    when(coordinatesService.build(any(), any()))
        .thenThrow(
            new ResponseStatusException(HttpStatus.BAD_REQUEST, "appId and terId are required"));
    TemplateExecutionService service =
        newService(
            mock(TaskRepository.class),
            mock(TaskRelationRepository.class),
            mock(ProxyConfigurationService.class),
            mock(DatabaseConnectionService.class),
            mock(HttpClientFactory.class),
            mock(SystemVariableResolver.class),
            mock(TemplateRenderService.class),
            coordinatesService,
            new ObjectMapper());

    MoreInfoAdvancedRenderRequestDto request = new MoreInfoAdvancedRenderRequestDto();
    request.setMiaTaskIds(List.of(42));
    request.setParameters(Map.of("featureId", "123"));

    assertThatThrownBy(() -> service.renderMoreInfoAdvanced(request, "en"))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
        .isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void renderMoreInfoAdvancedReturnsForbiddenWhenParentIsDenied() {
    TaskRepository taskRepository = mock(TaskRepository.class);
    TemplateRequestCoordinatesService coordinatesService =
        mock(TemplateRequestCoordinatesService.class);
    when(coordinatesService.build(any(), any()))
        .thenReturn(requestCoordinatesWithUserPermission(7));
    TemplateExecutionService service =
        newService(
            taskRepository,
            mock(TaskRelationRepository.class),
            mock(ProxyConfigurationService.class),
            mock(DatabaseConnectionService.class),
            mock(HttpClientFactory.class),
            mock(SystemVariableResolver.class),
            mock(TemplateRenderService.class),
            coordinatesService,
            new ObjectMapper());
    when(taskRepository.findByRolesAndTerritory(any(), eq(7))).thenReturn(List.of());

    Task miaTask =
        Task.builder()
            .id(42)
            .type(
                TaskType.builder()
                    .id(DomainConstants.Tasks.TASK_TYPE_ID_MORE_INFO_ADVANCED)
                    .build())
            .properties(Map.of())
            .build();
    when(taskRepository.findById(42)).thenReturn(Optional.of(miaTask));

    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken("viewer", "n/a", List.of()));
    try {
      MoreInfoAdvancedRenderRequestDto request = new MoreInfoAdvancedRenderRequestDto();
      request.setMiaTaskIds(List.of(42));
      request.setAppId(5);
      request.setTerId(7);
      request.setParameters(Map.of());

      assertThatThrownBy(() -> service.renderMoreInfoAdvanced(request, "en"))
          .isInstanceOf(ResponseStatusException.class)
          .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
          .isEqualTo(HttpStatus.FORBIDDEN);
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  @Test
  void renderMoreInfoAdvancedReturnsForbiddenForPublicPrivateApp() {
    TaskRepository taskRepository = mock(TaskRepository.class);
    TemplateRequestCoordinatesService coordinatesService =
        mock(TemplateRequestCoordinatesService.class);
    when(coordinatesService.build(any(), any()))
        .thenReturn(requestCoordinatesWithUserPermission(7));
    RoleRepository roleRepository = mock(RoleRepository.class);
    when(roleRepository.findRolesByApplicationAndUserAndTerritory(any(), any(), any()))
        .thenReturn(List.of(Role.builder().id(3).build()));
    UserApplicationAccessPolicy accessPolicy = mock(UserApplicationAccessPolicy.class);
    when(accessPolicy.isPrivateAppDeniedForPublic(any(), eq(5))).thenReturn(true);
    ProxyConfigurationService proxyConfigurationService = mock(ProxyConfigurationService.class);
    when(proxyConfigurationService.validateUserAccess(any(), any())).thenReturn(true);
    LiteralTranslationResolver literalTranslationResolver = chromeLiteralResolver();
    CurrentRequestLanguageResolver currentRequestLanguageResolver =
        mock(CurrentRequestLanguageResolver.class);
    TemplateChildDataService childDataService =
        new TemplateChildDataService(
            proxyConfigurationService,
            mock(DatabaseConnectionService.class),
            mock(HttpClientFactory.class),
            mock(SystemVariableResolver.class),
            literalTranslationResolver,
            currentRequestLanguageResolver,
            new ObjectMapper());
    TemplateExecutionService service =
        new TemplateExecutionService(
            taskRepository,
            roleRepository,
            mock(TaskRelationRepository.class),
            mock(TemplateRenderService.class),
            coordinatesService,
            accessPolicy,
            childDataService,
            literalTranslationResolver,
            currentRequestLanguageResolver,
            new MiaHtmlRenderer(),
            mock(org.sitmun.administration.service.mapimage.MapImageTaskExecutionService.class),
            new ObjectMapper());

    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                "public", "n/a", List.of(new SimpleGrantedAuthority("ROLE_PUBLIC"))));
    try {
      MoreInfoAdvancedRenderRequestDto request = new MoreInfoAdvancedRenderRequestDto();
      request.setMiaTaskIds(List.of(42));
      request.setAppId(5);
      request.setTerId(7);

      assertThatThrownBy(() -> service.renderMoreInfoAdvanced(request, "en"))
          .isInstanceOf(ResponseStatusException.class)
          .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
          .isEqualTo(HttpStatus.FORBIDDEN);
      verify(taskRepository, never()).findById(any());
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  @Test
  void renderMoreInfoAdvancedAdminDeniedWhenParentNotAvailable() {
    TaskRepository taskRepository = mock(TaskRepository.class);
    TemplateRequestCoordinatesService coordinatesService =
        mock(TemplateRequestCoordinatesService.class);
    when(coordinatesService.build(any(), any()))
        .thenReturn(requestCoordinatesWithUserPermission(7));
    TemplateExecutionService service =
        newService(
            taskRepository,
            mock(TaskRelationRepository.class),
            mock(ProxyConfigurationService.class),
            mock(DatabaseConnectionService.class),
            mock(HttpClientFactory.class),
            mock(SystemVariableResolver.class),
            mock(TemplateRenderService.class),
            coordinatesService,
            new ObjectMapper());
    when(taskRepository.findByRolesAndTerritory(any(), eq(7))).thenReturn(List.of());

    Task miaTask =
        Task.builder()
            .id(42)
            .type(
                TaskType.builder()
                    .id(DomainConstants.Tasks.TASK_TYPE_ID_MORE_INFO_ADVANCED)
                    .build())
            .properties(Map.of())
            .build();
    when(taskRepository.findById(42)).thenReturn(Optional.of(miaTask));

    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                "admin", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    try {
      MoreInfoAdvancedRenderRequestDto request = new MoreInfoAdvancedRenderRequestDto();
      request.setMiaTaskIds(List.of(42));
      request.setAppId(5);
      request.setTerId(7);

      assertThatThrownBy(() -> service.renderMoreInfoAdvanced(request, "en"))
          .isInstanceOf(ResponseStatusException.class)
          .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
          .isEqualTo(HttpStatus.FORBIDDEN);
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  @Test
  void renderMoreInfoAdvancedAdminChildRespectsValidateUserAccess() {
    TaskRepository taskRepository = mock(TaskRepository.class);
    TemplateRequestCoordinatesService coordinatesService =
        mock(TemplateRequestCoordinatesService.class);
    when(coordinatesService.build(any(), any()))
        .thenReturn(requestCoordinatesWithUserPermission(7));
    ProxyConfigurationService proxyConfigurationService = mock(ProxyConfigurationService.class);
    DatabaseConnectionService databaseConnectionService = mock(DatabaseConnectionService.class);

    Task childTask =
        Task.builder()
            .id(101)
            .name("SQL child")
            .properties(
                Map.of(DomainConstants.Tasks.PROPERTY_SCOPE, DomainConstants.Tasks.SCOPE_SQL_QUERY))
            .build();
    Task miaTask =
        Task.builder()
            .id(42)
            .name("MIA parent")
            .type(
                TaskType.builder()
                    .id(DomainConstants.Tasks.TASK_TYPE_ID_MORE_INFO_ADVANCED)
                    .build())
            .properties(Map.of("parentLayout", "scroll", "childTaskOrderIds", List.of(101)))
            .build();
    when(taskRepository.findById(42)).thenReturn(Optional.of(miaTask));
    when(taskRepository.findById(101)).thenReturn(Optional.of(childTask));
    when(taskRepository.findByRolesAndTerritory(any(), eq(7)))
        .thenReturn(List.of(miaTask, childTask));

    TemplateExecutionService service =
        newService(
            taskRepository,
            mock(TaskRelationRepository.class),
            proxyConfigurationService,
            databaseConnectionService,
            mock(HttpClientFactory.class),
            mock(SystemVariableResolver.class),
            mock(TemplateRenderService.class),
            coordinatesService,
            new ObjectMapper());
    // newService stubs VA=true; render path must still honor a later deny for ADMIN callers
    when(proxyConfigurationService.validateUserAccess(any(), any())).thenReturn(false);

    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                "admin", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    try {
      MoreInfoAdvancedRenderRequestDto request = new MoreInfoAdvancedRenderRequestDto();
      request.setMiaTaskIds(List.of(42));
      request.setAppId(5);
      request.setTerId(7);
      request.setParameters(Map.of());

      MoreInfoAdvancedRenderResponseDto response = service.renderMoreInfoAdvanced(request, "en");

      assertThat(response.getTasks()).hasSize(1);
      assertThat(response.getTasks().get(0).getHtml()).contains("sitmun-mia-empty");
      verify(proxyConfigurationService).validateUserAccess(any(), any());
      verify(databaseConnectionService, never()).executeQuery(any(), any(), any());
    } finally {
      SecurityContextHolder.clearContext();
    }
  }
}
