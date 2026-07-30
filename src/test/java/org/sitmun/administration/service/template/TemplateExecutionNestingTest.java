package org.sitmun.administration.service.template;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.sitmun.administration.controller.dto.TemplatePreviewResponseDto;
import org.sitmun.administration.controller.dto.TemplateTaskExecutionRequestDto;
import org.sitmun.administration.controller.dto.TemplateTaskExecutionResponseDto;
import org.sitmun.administration.service.database.DatabaseConnectionService;
import org.sitmun.administration.service.extractor.HttpClientFactory;
import org.sitmun.authorization.proxy.dto.ConfigProxyDto;
import org.sitmun.authorization.proxy.dto.ConfigProxyRequestDto;
import org.sitmun.authorization.proxy.protocols.jdbc.JdbcPayloadDto;
import org.sitmun.authorization.proxy.service.ProxyConfigurationService;
import org.sitmun.domain.DomainConstants;
import org.sitmun.domain.task.Task;
import org.sitmun.domain.task.TaskRepository;
import org.sitmun.domain.task.relation.TaskRelation;
import org.sitmun.domain.task.relation.TaskRelationRepository;
import org.sitmun.domain.task.type.TaskType;
import org.sitmun.infrastructure.variables.SystemVariableResolver;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

class TemplateExecutionNestingTest extends TemplateExecutionServiceTestFixtures {
  @Test
  void executeLinkedTaskRendersNestedTemplatesCompletelyInBackend() {
    TaskRepository taskRepository = mock(TaskRepository.class);
    TaskRelationRepository taskRelationRepository = mock(TaskRelationRepository.class);
    TemplateRenderService templateRenderService = mock(TemplateRenderService.class);
    TemplateRequestCoordinatesService coordinatesService =
        mock(TemplateRequestCoordinatesService.class);
    SystemVariableResolver systemVariableResolver = mock(SystemVariableResolver.class);
    when(coordinatesService.build(any(), any()))
        .thenReturn(requestCoordinatesWithUserPermission(7));
    when(systemVariableResolver.resolve(eq("https://example.com/{slug}"), any()))
        .thenReturn("https://example.com/{slug}");

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

    Task parentTemplate =
        Task.builder()
            .id(200)
            .properties(
                Map.of(
                    DomainConstants.Tasks.PROPERTY_TEMPLATE_HTML,
                    "<div>{{plantilla_hija.html}}</div>"))
            .type(
                org.sitmun.domain.task.type.TaskType.builder()
                    .id(DomainConstants.Tasks.TASK_TYPE_ID_TEMPLATE)
                    .build())
            .build();
    Task childTemplate =
        Task.builder()
            .id(201)
            .properties(
                Map.of(DomainConstants.Tasks.PROPERTY_TEMPLATE_HTML, "<p>{{consulta_url.url}}</p>"))
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
                    "https://example.com/{slug}"))
            .build();

    TemplateTaskExecutionRequestDto requestDto = new TemplateTaskExecutionRequestDto();
    requestDto.setAppId(5);
    requestDto.setTerId(7);
    requestDto.setLinkedTaskId(200);
    requestDto.setChildTaskParameters(Map.of("202", Map.of("slug", "abc")));

    when(taskRepository.findById(200)).thenReturn(Optional.of(parentTemplate));
    when(taskRelationRepository.findByTaskId(201))
        .thenReturn(
            List.of(
                TaskRelation.builder()
                    .id(2)
                    .task(childTemplate)
                    .relationType("template-task")
                    .referenceAlias("consulta_url")
                    .relatedTask(urlTask)
                    .build()));
    when(taskRelationRepository.findByTaskId(200))
        .thenReturn(
            List.of(
                TaskRelation.builder()
                    .id(1)
                    .task(parentTemplate)
                    .relationType("template-nested")
                    .referenceAlias("plantilla_hija")
                    .relatedTask(childTemplate)
                    .build()));
    when(templateRenderService.renderPreview(
            eq("<p>{{consulta_url.url}}</p>"), any(), any(), any(), any(), any()))
        .thenReturn(
            TemplatePreviewResponseDto.builder()
                .html("<p>https://example.com/abc</p>")
                .placeholders(List.of())
                .build());
    when(templateRenderService.renderPreview(
            eq("<div>{{plantilla_hija.html}}</div>"), any(), any(), any(), any(), any()))
        .thenReturn(
            TemplatePreviewResponseDto.builder()
                .html("<div><p>https://example.com/abc</p></div>")
                .placeholders(List.of())
                .build());

    TemplateTaskExecutionResponseDto result = service.executeLinkedTask(requestDto);

    assertThat(result.getResultType()).isEqualTo("template");
    assertThat(result.getContext())
        .containsEntry("html", "<div><p>https://example.com/abc</p></div>");
  }

  @Test
  void executeLinkedTaskPassesRequestLanguageToNestedTemplateRenders() {
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

    Task parentTemplate =
        Task.builder()
            .id(900)
            .properties(
                Map.of(
                    DomainConstants.Tasks.PROPERTY_TEMPLATE_HTML,
                    "<div>{{plantilla_hija.html}}</div>"))
            .type(TaskType.builder().id(DomainConstants.Tasks.TASK_TYPE_ID_TEMPLATE).build())
            .build();
    Task childTemplate =
        Task.builder()
            .id(901)
            .properties(Map.of(DomainConstants.Tasks.PROPERTY_TEMPLATE_HTML, "<p><t>Hola</t></p>"))
            .type(TaskType.builder().id(DomainConstants.Tasks.TASK_TYPE_ID_TEMPLATE).build())
            .build();

    when(taskRepository.findById(900)).thenReturn(Optional.of(parentTemplate));
    when(taskRelationRepository.findByTaskId(900))
        .thenReturn(
            List.of(
                TaskRelation.builder()
                    .id(1)
                    .task(parentTemplate)
                    .relationType("template-nested")
                    .referenceAlias("plantilla_hija")
                    .relatedTask(childTemplate)
                    .build()));
    when(taskRelationRepository.findByTaskId(901)).thenReturn(List.of());
    when(templateRenderService.renderPreview(
            eq("<p><t>Hola</t></p>"), any(), eq(List.of()), eq("fr"), any(), any()))
        .thenReturn(
            TemplatePreviewResponseDto.builder()
                .html("<p>Bonjour</p>")
                .placeholders(List.of())
                .build());
    when(templateRenderService.renderPreview(
            eq("<div>{{plantilla_hija.html}}</div>"), any(), eq(List.of()), eq("fr"), any(), any()))
        .thenReturn(
            TemplatePreviewResponseDto.builder()
                .html("<div><p>Bonjour</p></div>")
                .placeholders(List.of())
                .build());

    TemplateTaskExecutionRequestDto requestDto = new TemplateTaskExecutionRequestDto();
    requestDto.setAppId(5);
    requestDto.setTerId(7);
    requestDto.setLinkedTaskId(900);
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setParameter("lang", "fr");
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    try {
      TemplateTaskExecutionResponseDto result = service.executeLinkedTask(requestDto);

      assertThat(result.getContext()).containsEntry("html", "<div><p>Bonjour</p></div>");
      verify(templateRenderService)
          .renderPreview(eq("<p><t>Hola</t></p>"), any(), eq(List.of()), eq("fr"), any(), any());
      verify(templateRenderService)
          .renderPreview(
              eq("<div>{{plantilla_hija.html}}</div>"),
              any(),
              eq(List.of()),
              eq("fr"),
              any(),
              any());
    } finally {
      RequestContextHolder.resetRequestAttributes();
    }
  }

  @Test
  void executeLinkedTaskRejectsTemplateNestingDeeperThanThreeLevels() {
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

    Task template1 =
        Task.builder()
            .id(301)
            .properties(Map.of(DomainConstants.Tasks.PROPERTY_TEMPLATE_HTML, "{{task_302.html}}"))
            .type(
                org.sitmun.domain.task.type.TaskType.builder()
                    .id(DomainConstants.Tasks.TASK_TYPE_ID_TEMPLATE)
                    .build())
            .build();
    Task template2 =
        Task.builder()
            .id(302)
            .properties(Map.of(DomainConstants.Tasks.PROPERTY_TEMPLATE_HTML, "{{task_303.html}}"))
            .type(
                org.sitmun.domain.task.type.TaskType.builder()
                    .id(DomainConstants.Tasks.TASK_TYPE_ID_TEMPLATE)
                    .build())
            .build();
    Task template3 =
        Task.builder()
            .id(303)
            .properties(Map.of(DomainConstants.Tasks.PROPERTY_TEMPLATE_HTML, "{{task_304.html}}"))
            .type(
                org.sitmun.domain.task.type.TaskType.builder()
                    .id(DomainConstants.Tasks.TASK_TYPE_ID_TEMPLATE)
                    .build())
            .build();
    Task template4 =
        Task.builder()
            .id(304)
            .properties(Map.of(DomainConstants.Tasks.PROPERTY_TEMPLATE_HTML, "<p>too deep</p>"))
            .type(
                org.sitmun.domain.task.type.TaskType.builder()
                    .id(DomainConstants.Tasks.TASK_TYPE_ID_TEMPLATE)
                    .build())
            .build();

    TemplateTaskExecutionRequestDto requestDto = new TemplateTaskExecutionRequestDto();
    requestDto.setAppId(5);
    requestDto.setTerId(7);
    requestDto.setLinkedTaskId(301);

    when(taskRepository.findById(301)).thenReturn(Optional.of(template1));
    when(taskRelationRepository.findByTaskId(301))
        .thenReturn(
            List.of(
                TaskRelation.builder()
                    .id(1)
                    .task(template1)
                    .relationType("template-nested")
                    .relatedTask(template2)
                    .build()));
    when(taskRelationRepository.findByTaskId(302))
        .thenReturn(
            List.of(
                TaskRelation.builder()
                    .id(2)
                    .task(template2)
                    .relationType("template-nested")
                    .relatedTask(template3)
                    .build()));
    when(taskRelationRepository.findByTaskId(303))
        .thenReturn(
            List.of(
                TaskRelation.builder()
                    .id(3)
                    .task(template3)
                    .relationType("template-nested")
                    .relatedTask(template4)
                    .build()));

    assertThatThrownBy(() -> service.executeLinkedTask(requestDto))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(
            exception -> {
              ResponseStatusException responseStatusException = (ResponseStatusException) exception;
              assertThat(responseStatusException.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
              assertThat(responseStatusException.getReason())
                  .contains("Template nesting depth exceeded")
                  .contains("3");
            });
  }

  @Test
  void executeLinkedTaskForwardsRootTemplateDefaultsIntoNestedSqlChild() throws Exception {
    ProxyConfigurationService proxyConfigurationService = mock(ProxyConfigurationService.class);
    DatabaseConnectionService databaseConnectionService = mock(DatabaseConnectionService.class);
    TemplateRenderService templateRenderService = mock(TemplateRenderService.class);
    TemplateRequestCoordinatesService coordinatesService =
        mock(TemplateRequestCoordinatesService.class);
    when(coordinatesService.build(any(), any()))
        .thenReturn(requestCoordinatesWithUserPermission(7));
    stubSqlChildExecution(proxyConfigurationService, databaseConnectionService);
    when(templateRenderService.renderPreview(any(), any(), any(), any(), any(), any()))
        .thenReturn(
            TemplatePreviewResponseDto.builder().html("<p>ok</p>").placeholders(List.of()).build());

    TaskRepository taskRepository = mock(TaskRepository.class);
    TaskRelationRepository taskRelationRepository = mock(TaskRelationRepository.class);
    TemplateExecutionService service =
        newService(
            taskRepository,
            taskRelationRepository,
            proxyConfigurationService,
            databaseConnectionService,
            mock(HttpClientFactory.class),
            mock(SystemVariableResolver.class),
            templateRenderService,
            coordinatesService,
            new ObjectMapper());

    stubRootNestedSqlGraph(taskRepository, taskRelationRepository, true);

    TemplateTaskExecutionRequestDto requestDto = new TemplateTaskExecutionRequestDto();
    requestDto.setAppId(5);
    requestDto.setTerId(7);
    requestDto.setLinkedTaskId(201);
    requestDto.setTemplateTaskId(100);
    requestDto.setChildTaskParameters(Map.of());

    service.executeLinkedTask(requestDto);

    ArgumentCaptor<ConfigProxyRequestDto> requestCaptor =
        ArgumentCaptor.forClass(ConfigProxyRequestDto.class);
    verify(proxyConfigurationService).getConfiguration(requestCaptor.capture(), anyLong(), any());
    assertThat(requestCaptor.getValue().getParameters()).containsEntry("nameFilter", "%sitna%");

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<String, Object>> contextCaptor = ArgumentCaptor.forClass(Map.class);
    verify(templateRenderService)
        .renderPreview(any(), contextCaptor.capture(), any(), any(), any(), any());
    assertThat(contextCaptor.getValue()).containsEntry("$nameFilter", "%sitna%");
  }

  @Test
  void executeLinkedTaskChildTaskParametersOverrideRootTemplateDefaults() throws Exception {
    ProxyConfigurationService proxyConfigurationService = mock(ProxyConfigurationService.class);
    DatabaseConnectionService databaseConnectionService = mock(DatabaseConnectionService.class);
    TemplateRenderService templateRenderService = mock(TemplateRenderService.class);
    TemplateRequestCoordinatesService coordinatesService =
        mock(TemplateRequestCoordinatesService.class);
    when(coordinatesService.build(any(), any()))
        .thenReturn(requestCoordinatesWithUserPermission(7));
    stubSqlChildExecution(proxyConfigurationService, databaseConnectionService);
    when(templateRenderService.renderPreview(any(), any(), any(), any(), any(), any()))
        .thenReturn(
            TemplatePreviewResponseDto.builder().html("<p>ok</p>").placeholders(List.of()).build());

    TaskRepository taskRepository = mock(TaskRepository.class);
    TaskRelationRepository taskRelationRepository = mock(TaskRelationRepository.class);
    TemplateExecutionService service =
        newService(
            taskRepository,
            taskRelationRepository,
            proxyConfigurationService,
            databaseConnectionService,
            mock(HttpClientFactory.class),
            mock(SystemVariableResolver.class),
            templateRenderService,
            coordinatesService,
            new ObjectMapper());

    stubRootNestedSqlGraph(taskRepository, taskRelationRepository, true);

    TemplateTaskExecutionRequestDto requestDto = new TemplateTaskExecutionRequestDto();
    requestDto.setAppId(5);
    requestDto.setTerId(7);
    requestDto.setLinkedTaskId(201);
    requestDto.setTemplateTaskId(100);
    requestDto.setChildTaskParameters(Map.of("202", Map.of("nameFilter", "%other%")));

    service.executeLinkedTask(requestDto);

    ArgumentCaptor<ConfigProxyRequestDto> requestCaptor =
        ArgumentCaptor.forClass(ConfigProxyRequestDto.class);
    verify(proxyConfigurationService).getConfiguration(requestCaptor.capture(), anyLong(), any());
    assertThat(requestCaptor.getValue().getParameters()).containsEntry("nameFilter", "%other%");
  }

  @Test
  void executeLinkedTaskDropsUndeclaredParametersFromPlantillaContext() {
    TemplateRenderService templateRenderService = mock(TemplateRenderService.class);
    TemplateRequestCoordinatesService coordinatesService =
        mock(TemplateRequestCoordinatesService.class);
    when(coordinatesService.build(any(), any()))
        .thenReturn(requestCoordinatesWithUserPermission(7));
    when(templateRenderService.renderPreview(any(), any(), any(), any(), any(), any()))
        .thenReturn(
            TemplatePreviewResponseDto.builder().html("<p>ok</p>").placeholders(List.of()).build());

    TaskRepository taskRepository = mock(TaskRepository.class);
    TaskRelationRepository taskRelationRepository = mock(TaskRelationRepository.class);
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

    Task rootTemplate =
        Task.builder()
            .id(100)
            .properties(
                Map.of(
                    DomainConstants.Tasks.PROPERTY_PARAMETERS,
                    List.of(Map.of("name", "nameFilter", "value", "%sitna%", "type", "string")),
                    DomainConstants.Tasks.PROPERTY_TEMPLATE_HTML,
                    "<p>{{$nameFilter}}</p>"))
            .type(TaskType.builder().id(DomainConstants.Tasks.TASK_TYPE_ID_TEMPLATE).build())
            .build();
    when(taskRepository.findById(100)).thenReturn(Optional.of(rootTemplate));
    when(taskRelationRepository.findByTaskId(100)).thenReturn(List.of());

    TemplateTaskExecutionRequestDto requestDto = new TemplateTaskExecutionRequestDto();
    requestDto.setAppId(5);
    requestDto.setTerId(7);
    requestDto.setLinkedTaskId(100);
    requestDto.setTemplateTaskId(100);
    requestDto.setParameters(Map.of("nameFilter", "%x%", "undeclared", "nope"));

    service.executeLinkedTask(requestDto);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<String, Object>> contextCaptor = ArgumentCaptor.forClass(Map.class);
    verify(templateRenderService)
        .renderPreview(any(), contextCaptor.capture(), any(), any(), any(), any());
    assertThat(contextCaptor.getValue())
        .containsEntry("$nameFilter", "%x%")
        .doesNotContainKey("$undeclared");
  }

  @Test
  void executeLinkedTaskAppliesDefaultWhenInvokeValueBlank() {
    TemplateRenderService templateRenderService = mock(TemplateRenderService.class);
    TemplateRequestCoordinatesService coordinatesService =
        mock(TemplateRequestCoordinatesService.class);
    when(coordinatesService.build(any(), any()))
        .thenReturn(requestCoordinatesWithUserPermission(7));
    when(templateRenderService.renderPreview(any(), any(), any(), any(), any(), any()))
        .thenReturn(
            TemplatePreviewResponseDto.builder().html("<p>ok</p>").placeholders(List.of()).build());

    TaskRepository taskRepository = mock(TaskRepository.class);
    TaskRelationRepository taskRelationRepository = mock(TaskRelationRepository.class);
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

    Task rootTemplate =
        Task.builder()
            .id(100)
            .properties(
                Map.of(
                    DomainConstants.Tasks.PROPERTY_PARAMETERS,
                    List.of(Map.of("name", "nameFilter", "value", "%sitna%", "type", "string")),
                    DomainConstants.Tasks.PROPERTY_TEMPLATE_HTML,
                    "<p>{{$nameFilter}}</p>"))
            .type(TaskType.builder().id(DomainConstants.Tasks.TASK_TYPE_ID_TEMPLATE).build())
            .build();
    when(taskRepository.findById(100)).thenReturn(Optional.of(rootTemplate));
    when(taskRelationRepository.findByTaskId(100)).thenReturn(List.of());

    TemplateTaskExecutionRequestDto requestDto = new TemplateTaskExecutionRequestDto();
    requestDto.setAppId(5);
    requestDto.setTerId(7);
    requestDto.setLinkedTaskId(100);
    requestDto.setTemplateTaskId(100);
    requestDto.setParameters(Map.of("nameFilter", ""));

    service.executeLinkedTask(requestDto);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<String, Object>> contextCaptor = ArgumentCaptor.forClass(Map.class);
    verify(templateRenderService)
        .renderPreview(any(), contextCaptor.capture(), any(), any(), any(), any());
    assertThat(contextCaptor.getValue()).containsEntry("$nameFilter", "%sitna%");
  }

  @Test
  void executeLinkedTaskDoesNotInjectUndeclaredParentParamsIntoEmptyNestedPlantilla()
      throws Exception {
    ProxyConfigurationService proxyConfigurationService = mock(ProxyConfigurationService.class);
    DatabaseConnectionService databaseConnectionService = mock(DatabaseConnectionService.class);
    TemplateRenderService templateRenderService = mock(TemplateRenderService.class);
    TemplateRequestCoordinatesService coordinatesService =
        mock(TemplateRequestCoordinatesService.class);
    when(coordinatesService.build(any(), any()))
        .thenReturn(requestCoordinatesWithUserPermission(7));
    stubSqlChildExecution(proxyConfigurationService, databaseConnectionService);
    when(templateRenderService.renderPreview(any(), any(), any(), any(), any(), any()))
        .thenReturn(
            TemplatePreviewResponseDto.builder().html("<p>ok</p>").placeholders(List.of()).build());

    TaskRepository taskRepository = mock(TaskRepository.class);
    TaskRelationRepository taskRelationRepository = mock(TaskRelationRepository.class);
    TemplateExecutionService service =
        newService(
            taskRepository,
            taskRelationRepository,
            proxyConfigurationService,
            databaseConnectionService,
            mock(HttpClientFactory.class),
            mock(SystemVariableResolver.class),
            templateRenderService,
            coordinatesService,
            new ObjectMapper());

    stubRootNestedSqlGraph(taskRepository, taskRelationRepository, false);

    TemplateTaskExecutionRequestDto requestDto = new TemplateTaskExecutionRequestDto();
    requestDto.setAppId(5);
    requestDto.setTerId(7);
    requestDto.setLinkedTaskId(201);
    requestDto.setTemplateTaskId(100);
    requestDto.setChildTaskParameters(Map.of());

    service.executeLinkedTask(requestDto);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<String, Object>> contextCaptor = ArgumentCaptor.forClass(Map.class);
    verify(templateRenderService)
        .renderPreview(any(), contextCaptor.capture(), any(), any(), any(), any());
    assertThat(contextCaptor.getValue()).doesNotContainKey("$nameFilter");

    ArgumentCaptor<ConfigProxyRequestDto> requestCaptor =
        ArgumentCaptor.forClass(ConfigProxyRequestDto.class);
    verify(proxyConfigurationService).getConfiguration(requestCaptor.capture(), anyLong(), any());
    // SQL child still declares nameFilter, so parent default may bind there.
    assertThat(requestCaptor.getValue().getParameters()).containsEntry("nameFilter", "%sitna%");
  }

  private static void stubSqlChildExecution(
      ProxyConfigurationService proxyConfigurationService,
      DatabaseConnectionService databaseConnectionService)
      throws Exception {
    JdbcPayloadDto payload =
        JdbcPayloadDto.builder()
            .driver("org.h2.Driver")
            .uri("jdbc:h2:mem:nesting-inherit")
            .user("sa")
            .password("")
            .sql("select 1")
            .build();
    when(proxyConfigurationService.getConfiguration(any(), anyLong(), any()))
        .thenReturn(ConfigProxyDto.builder().type("JDBC").payload(payload).build());
    when(databaseConnectionService.executeQuery(any(), any(), any())).thenReturn(List.of());
  }

  private static void stubRootNestedSqlGraph(
      TaskRepository taskRepository,
      TaskRelationRepository taskRelationRepository,
      boolean nestedDeclaresNameFilter) {
    Task rootTemplate =
        Task.builder()
            .id(100)
            .properties(
                Map.of(
                    DomainConstants.Tasks.PROPERTY_PARAMETERS,
                    List.of(Map.of("name", "nameFilter", "value", "%sitna%", "type", "string")),
                    DomainConstants.Tasks.PROPERTY_TEMPLATE_HTML,
                    "<div>{{fill.html}}</div>"))
            .type(TaskType.builder().id(DomainConstants.Tasks.TASK_TYPE_ID_TEMPLATE).build())
            .build();
    Map<String, Object> nestedProperties = new java.util.LinkedHashMap<>();
    nestedProperties.put(DomainConstants.Tasks.PROPERTY_TEMPLATE_HTML, "<p>{{$nameFilter}}</p>");
    if (nestedDeclaresNameFilter) {
      nestedProperties.put(
          DomainConstants.Tasks.PROPERTY_PARAMETERS,
          List.of(Map.of("name", "nameFilter", "type", "string")));
    } else {
      nestedProperties.put(DomainConstants.Tasks.PROPERTY_PARAMETERS, List.of());
    }
    Task nestedTemplate =
        Task.builder()
            .id(201)
            .properties(nestedProperties)
            .type(TaskType.builder().id(DomainConstants.Tasks.TASK_TYPE_ID_TEMPLATE).build())
            .build();
    Task sqlChild =
        Task.builder()
            .id(202)
            .properties(
                Map.of(
                    DomainConstants.Tasks.PROPERTY_PARAMETERS,
                    List.of(Map.of("name", "nameFilter", "type", "query")),
                    DomainConstants.Tasks.PROPERTY_SCOPE,
                    DomainConstants.Tasks.SCOPE_SQL_QUERY,
                    DomainConstants.Tasks.PROPERTY_COMMAND,
                    "SELECT 1 WHERE name ILIKE ${nameFilter}"))
            .build();

    when(taskRepository.findById(100)).thenReturn(Optional.of(rootTemplate));
    when(taskRepository.findById(201)).thenReturn(Optional.of(nestedTemplate));
    when(taskRelationRepository.findByTaskId(201))
        .thenReturn(
            List.of(
                TaskRelation.builder()
                    .id(1)
                    .task(nestedTemplate)
                    .relationType("template-task")
                    .referenceAlias("nested_sql")
                    .relatedTask(sqlChild)
                    .build()));
  }
}
