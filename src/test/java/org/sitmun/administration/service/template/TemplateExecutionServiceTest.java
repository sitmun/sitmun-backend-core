package org.sitmun.administration.service.template;

import com.fasterxml.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSource;
import okio.Okio;
import okio.Source;
import okio.Timeout;
import org.junit.jupiter.api.Test;
import org.sitmun.administration.controller.dto.MoreInfoAdvancedRenderRequestDto;
import org.sitmun.administration.controller.dto.MoreInfoAdvancedRenderResponseDto;
import org.sitmun.administration.controller.dto.TemplatePreviewResponseDto;
import org.sitmun.administration.controller.dto.TemplateTaskExecutionRequestDto;
import org.sitmun.administration.controller.dto.TemplateTaskExecutionResponseDto;
import org.sitmun.administration.service.database.DatabaseConnectionService;
import org.sitmun.administration.service.extractor.HttpClientFactory;
import org.sitmun.authorization.proxy.dto.ConfigProxyDto;
import org.sitmun.authorization.proxy.dto.HttpSecurityDto;
import org.sitmun.authorization.proxy.exception.BadRequestException;
import org.sitmun.authorization.proxy.protocols.jdbc.JdbcPayloadDto;
import org.sitmun.authorization.proxy.protocols.wms.WmsPayloadDto;
import org.sitmun.authorization.proxy.service.ProxyConfigurationService;
import org.sitmun.authorization.proxy.service.RequestCoordinates;
import org.sitmun.domain.DomainConstants;
import org.sitmun.domain.task.Task;
import org.sitmun.domain.task.TaskRepository;
import org.sitmun.domain.task.relation.TaskRelation;
import org.sitmun.domain.task.relation.TaskRelationRepository;
import org.sitmun.domain.task.type.TaskType;
import org.sitmun.domain.task.ui.TaskUI;
import org.sitmun.infrastructure.variables.SystemVariableResolver;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class TemplateExecutionServiceTest {

  @Test
  void renderMoreInfoAdvancedResolvesChildOrderInBackend() {
    TaskRepository taskRepository = mock(TaskRepository.class);
    TemplateRequestCoordinatesService coordinatesService = mock(TemplateRequestCoordinatesService.class);
    when(coordinatesService.build(any())).thenReturn(new RequestCoordinates());
    TemplateExecutionService service =
        new TemplateExecutionService(
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
    when(miaTask.getType()).thenReturn(TaskType.builder().id(DomainConstants.Tasks.TASK_TYPE_ID_MORE_INFO_ADVANCED).build());
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
    request.setMiaTaskIds(List.of(16));
    request.setParameters(Map.of("id", "A-1"));

    MoreInfoAdvancedRenderResponseDto result = service.renderMoreInfoAdvanced(request);

    assertThat(result.getTasks()).hasSize(1);
    assertThat(result.getTasks().get(0).getHtml()).contains("https://example.org/doc/A-1");
  }

  @Test
  void renderMoreInfoAdvancedRejectsBasicViewerHookTask() {
    TaskRepository taskRepository = mock(TaskRepository.class);
    TemplateRequestCoordinatesService coordinatesService = mock(TemplateRequestCoordinatesService.class);
    when(coordinatesService.build(any())).thenReturn(new RequestCoordinates());
    TemplateExecutionService service =
        new TemplateExecutionService(
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
    when(basicHookTask.getType()).thenReturn(TaskType.builder().id(DomainConstants.Tasks.TASK_TYPE_ID_BASIC).build());
    when(basicHookTask.getUi()).thenReturn(TaskUI.builder().name("sitna.moreInfoAdvanced").build());
    when(taskRepository.findById(32306)).thenReturn(Optional.of(basicHookTask));

    MoreInfoAdvancedRenderRequestDto request = new MoreInfoAdvancedRenderRequestDto();
    request.setMiaTaskIds(List.of(32306));

    assertThatThrownBy(() -> service.renderMoreInfoAdvanced(request))
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
    TemplateRequestCoordinatesService coordinatesService = mock(TemplateRequestCoordinatesService.class);
    SystemVariableResolver systemVariableResolver = mock(SystemVariableResolver.class);
    when(coordinatesService.build(any())).thenReturn(new RequestCoordinates());
    when(systemVariableResolver.resolve(eq("https://www.google.com/search?q={dificultat}"), any()))
        .thenReturn("https://www.google.com/search?q={dificultat}");
    TemplateExecutionService service =
        new TemplateExecutionService(
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
    when(miaTask.getType()).thenReturn(TaskType.builder().id(DomainConstants.Tasks.TASK_TYPE_ID_MORE_INFO_ADVANCED).build());
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
            .properties(Map.of(DomainConstants.Tasks.PROPERTY_TEMPLATE_HTML, "<a>{{consulta_url.url}}</a>"))
            .type(org.sitmun.domain.task.type.TaskType.builder().id(DomainConstants.Tasks.TASK_TYPE_ID_TEMPLATE).build())
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
                    List.of(new LinkedHashMap<>(Map.of("name", "dificultat", "type", "Query parameter")))))
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
    when(templateRenderService.renderPreview(eq("<a>{{consulta_url.url}}</a>"), any(), eq(201)))
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
    request.setMiaTaskIds(List.of(16));
    request.setParameters(Map.of("dificultat", "Mitjana"));

    MoreInfoAdvancedRenderResponseDto result = service.renderMoreInfoAdvanced(request);

    assertThat(result.getTasks()).hasSize(1);
    assertThat(result.getTasks().get(0).getHtml())
        .contains("https://www.google.com/search?q=Mitjana");
  }

  @Test
  void renderMoreInfoAdvancedResolvesTemplateParametersAndNestedChildParametersFromMappings() {
    TaskRepository taskRepository = mock(TaskRepository.class);
    TaskRelationRepository taskRelationRepository = mock(TaskRelationRepository.class);
    TemplateRenderService templateRenderService = mock(TemplateRenderService.class);
    TemplateRequestCoordinatesService coordinatesService = mock(TemplateRequestCoordinatesService.class);
    SystemVariableResolver systemVariableResolver = mock(SystemVariableResolver.class);
    when(coordinatesService.build(any())).thenReturn(new RequestCoordinates());
    when(systemVariableResolver.resolve(eq("https://example.org/layers/{innerParam}"), any()))
        .thenReturn("https://example.org/layers/{innerParam}");
    TemplateExecutionService service =
        new TemplateExecutionService(
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
    when(miaTask.getType()).thenReturn(TaskType.builder().id(DomainConstants.Tasks.TASK_TYPE_ID_MORE_INFO_ADVANCED).build());
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
            .properties(Map.of(DomainConstants.Tasks.PROPERTY_TEMPLATE_HTML, "<h1>{{$title}}</h1><a>{{consulta_url.url}}</a>"))
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
    when(templateRenderService.renderPreview(eq("<h1>{{$title}}</h1><a>{{consulta_url.url}}</a>"), any(), eq(401)))
        .thenAnswer(
            invocation -> {
              Map<String, Object> context = invocation.getArgument(1);
              Map<String, Object> childContext = (Map<String, Object>) context.get("consulta_url");
              return TemplatePreviewResponseDto.builder()
                  .html("<h1>" + context.get("$title") + "</h1><a>" + childContext.get("url") + "</a>")
                  .placeholders(List.of())
                  .build();
            });

    MoreInfoAdvancedRenderRequestDto request = new MoreInfoAdvancedRenderRequestDto();
    request.setMiaTaskIds(List.of(16));
    request.setParameters(Map.of("titleAttr", "Layer title", "layerid", "roads"));

    MoreInfoAdvancedRenderResponseDto result = service.renderMoreInfoAdvanced(request);

    assertThat(result.getTasks()).hasSize(1);
    assertThat(result.getTasks().get(0).getHtml())
        .contains("<h1>Layer title</h1>")
        .contains("https://example.org/layers/roads");
  }

  @Test
  void renderMoreInfoAdvancedKeepsTemplateDefaultValueWhenNoDirectMappingExists() {
    TaskRepository taskRepository = mock(TaskRepository.class);
    TemplateRenderService templateRenderService = mock(TemplateRenderService.class);
    TemplateRequestCoordinatesService coordinatesService = mock(TemplateRequestCoordinatesService.class);
    when(coordinatesService.build(any())).thenReturn(new RequestCoordinates());

    TemplateExecutionService service =
        new TemplateExecutionService(
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
    when(miaTask.getType()).thenReturn(TaskType.builder().id(DomainConstants.Tasks.TASK_TYPE_ID_MORE_INFO_ADVANCED).build());
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
                    List.of(Map.of("name", "title", "type", DomainConstants.Tasks.TYPE_STRING, "value", "Default title"))))
            .type(TaskType.builder().id(DomainConstants.Tasks.TASK_TYPE_ID_TEMPLATE).build())
            .build();

    when(taskRepository.findById(16)).thenReturn(Optional.of(miaTask));
    when(taskRepository.findById(701)).thenReturn(Optional.of(templateTask));
    when(templateRenderService.renderPreview(eq("<h1>{{$title}}</h1>"), any(), eq(701)))
        .thenAnswer(
            invocation -> {
              Map<String, Object> context = invocation.getArgument(1);
              return TemplatePreviewResponseDto.builder()
                  .html("<h1>" + context.get("$title") + "</h1>")
                  .placeholders(List.of())
                  .build();
            });

    MoreInfoAdvancedRenderRequestDto request = new MoreInfoAdvancedRenderRequestDto();
    request.setMiaTaskIds(List.of(16));
    request.setParameters(Map.of("Default title", "Wrong feature value"));

    MoreInfoAdvancedRenderResponseDto result = service.renderMoreInfoAdvanced(request);

    assertThat(result.getTasks()).hasSize(1);
    assertThat(result.getTasks().get(0).getHtml()).contains("<h1>Default title</h1>");
  }

  @Test
  void renderMoreInfoAdvancedPrefersExplicitMappingOverTemplateDefaultValue() {
    TaskRepository taskRepository = mock(TaskRepository.class);
    TemplateRenderService templateRenderService = mock(TemplateRenderService.class);
    TemplateRequestCoordinatesService coordinatesService = mock(TemplateRequestCoordinatesService.class);
    when(coordinatesService.build(any())).thenReturn(new RequestCoordinates());

    TemplateExecutionService service =
        new TemplateExecutionService(
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
    when(miaTask.getType()).thenReturn(TaskType.builder().id(DomainConstants.Tasks.TASK_TYPE_ID_MORE_INFO_ADVANCED).build());
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
                    List.of(Map.of("name", "title", "type", DomainConstants.Tasks.TYPE_STRING, "value", "Default title"))))
            .type(TaskType.builder().id(DomainConstants.Tasks.TASK_TYPE_ID_TEMPLATE).build())
            .build();

    when(taskRepository.findById(16)).thenReturn(Optional.of(miaTask));
    when(taskRepository.findById(702)).thenReturn(Optional.of(templateTask));
    when(templateRenderService.renderPreview(eq("<h1>{{$title}}</h1>"), any(), eq(702)))
        .thenAnswer(
            invocation -> {
              Map<String, Object> context = invocation.getArgument(1);
              return TemplatePreviewResponseDto.builder()
                  .html("<h1>" + context.get("$title") + "</h1>")
                  .placeholders(List.of())
                  .build();
            });

    MoreInfoAdvancedRenderRequestDto request = new MoreInfoAdvancedRenderRequestDto();
    request.setMiaTaskIds(List.of(16));
    request.setParameters(Map.of("featureTitle", "Mapped title", "Default title", "Wrong feature value"));

    MoreInfoAdvancedRenderResponseDto result = service.renderMoreInfoAdvanced(request);

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
    TemplateRequestCoordinatesService coordinatesService = mock(TemplateRequestCoordinatesService.class);
    SystemVariableResolver systemVariableResolver = mock(SystemVariableResolver.class);
    when(coordinatesService.build(any())).thenReturn(new RequestCoordinates());
    when(systemVariableResolver.resolve(eq("https://example.org/items/{innerParam}"), any()))
        .thenReturn("https://example.org/items/{innerParam}");
    TemplateExecutionService service =
        new TemplateExecutionService(
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
    when(miaTask.getType()).thenReturn(TaskType.builder().id(DomainConstants.Tasks.TASK_TYPE_ID_MORE_INFO_ADVANCED).build());
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
            .properties(Map.of(DomainConstants.Tasks.PROPERTY_TEMPLATE_HTML, "<a>{{consulta_url.url}}</a>"))
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
    when(templateRenderService.renderPreview(eq("<a>{{consulta_url.url}}</a>"), any(), eq(501)))
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
    request.setMiaTaskIds(List.of(16));
    request.setParameters(Map.of("featureCode", "abc"));

    MoreInfoAdvancedRenderResponseDto result = service.renderMoreInfoAdvanced(request);

    assertThat(result.getTasks().get(0).getHtml()).contains("https://example.org/items/abc");
  }

  @Test
  void renderMoreInfoAdvancedKeepsExplicitInnerChildMappingOverNestedMapping() {
    TaskRepository taskRepository = mock(TaskRepository.class);
    TaskRelationRepository taskRelationRepository = mock(TaskRelationRepository.class);
    TemplateRenderService templateRenderService = mock(TemplateRenderService.class);
    TemplateRequestCoordinatesService coordinatesService = mock(TemplateRequestCoordinatesService.class);
    SystemVariableResolver systemVariableResolver = mock(SystemVariableResolver.class);
    when(coordinatesService.build(any())).thenReturn(new RequestCoordinates());
    when(systemVariableResolver.resolve(eq("https://example.org/items/{innerParam}"), any()))
        .thenReturn("https://example.org/items/{innerParam}");
    TemplateExecutionService service =
        new TemplateExecutionService(
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
    when(miaTask.getType()).thenReturn(TaskType.builder().id(DomainConstants.Tasks.TASK_TYPE_ID_MORE_INFO_ADVANCED).build());
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
            .properties(Map.of(DomainConstants.Tasks.PROPERTY_TEMPLATE_HTML, "<a>{{consulta_url.url}}</a>"))
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
    when(templateRenderService.renderPreview(eq("<a>{{consulta_url.url}}</a>"), any(), eq(601)))
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
    request.setMiaTaskIds(List.of(16));
    request.setParameters(Map.of("explicitAttr", "explicit-value", "nestedAttr", "nested-value"));

    MoreInfoAdvancedRenderResponseDto result = service.renderMoreInfoAdvanced(request);

    assertThat(result.getTasks().get(0).getHtml())
        .contains("https://example.org/items/explicit-value")
        .doesNotContain("nested-value");
  }

  @Test
  void renderMoreInfoAdvancedResolvesUrlParametersInsideRecursiveNestedTemplate() {
    TaskRepository taskRepository = mock(TaskRepository.class);
    TaskRelationRepository taskRelationRepository = mock(TaskRelationRepository.class);
    TemplateRenderService templateRenderService = mock(TemplateRenderService.class);
    TemplateRequestCoordinatesService coordinatesService = mock(TemplateRequestCoordinatesService.class);
    SystemVariableResolver systemVariableResolver = mock(SystemVariableResolver.class);
    when(coordinatesService.build(any())).thenReturn(new RequestCoordinates());
    when(systemVariableResolver.resolve(eq("https://www.google.com/search?q={dificultat}"), any()))
        .thenReturn("https://www.google.com/search?q={dificultat}");
    TemplateExecutionService service =
        new TemplateExecutionService(
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
    when(miaTask.getType()).thenReturn(TaskType.builder().id(DomainConstants.Tasks.TASK_TYPE_ID_MORE_INFO_ADVANCED).build());
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
            .properties(Map.of(DomainConstants.Tasks.PROPERTY_TEMPLATE_HTML, "<section>{{plantilla_hija.html}}</section>"))
            .type(org.sitmun.domain.task.type.TaskType.builder().id(DomainConstants.Tasks.TASK_TYPE_ID_TEMPLATE).build())
            .build();
    Task nestedTemplate =
        Task.builder()
            .id(302)
            .properties(Map.of(DomainConstants.Tasks.PROPERTY_TEMPLATE_HTML, "<a>{{consulta_url.url}}</a>"))
            .type(org.sitmun.domain.task.type.TaskType.builder().id(DomainConstants.Tasks.TASK_TYPE_ID_TEMPLATE).build())
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
                    List.of(new LinkedHashMap<>(Map.of("name", "dificultat", "type", "Query parameter")))))
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
    when(templateRenderService.renderPreview(eq("<a>{{consulta_url.url}}</a>"), any(), eq(301)))
        .thenAnswer(
            invocation -> {
              Map<String, Object> context = invocation.getArgument(1);
              Map<String, Object> childContext = (Map<String, Object>) context.get("consulta_url");
              return TemplatePreviewResponseDto.builder()
                  .html("<a>" + childContext.get("url") + "</a>")
                  .placeholders(List.of())
                  .build();
            });
    when(templateRenderService.renderPreview(eq("<section>{{plantilla_hija.html}}</section>"), any(), eq(301)))
        .thenAnswer(
            invocation -> {
              Map<String, Object> context = invocation.getArgument(1);
              Map<String, Object> childContext = (Map<String, Object>) context.get("plantilla_hija");
              return TemplatePreviewResponseDto.builder()
                  .html("<section>" + childContext.get("html") + "</section>")
                  .placeholders(List.of())
                  .build();
            });

    MoreInfoAdvancedRenderRequestDto request = new MoreInfoAdvancedRenderRequestDto();
    request.setMiaTaskIds(List.of(16));
    request.setParameters(Map.of("dificultat", "Mitjana"));

    MoreInfoAdvancedRenderResponseDto result = service.renderMoreInfoAdvanced(request);

    assertThat(result.getTasks()).hasSize(1);
    assertThat(result.getTasks().get(0).getHtml())
        .contains("https://www.google.com/search?q=Mitjana");
  }

  @Test
  void renderMoreInfoAdvancedAnnotatesTemplateWrapperWithTemplateTaskId() {
    TaskRepository taskRepository = mock(TaskRepository.class);
    TemplateRequestCoordinatesService coordinatesService = mock(TemplateRequestCoordinatesService.class);
    TemplateRenderService templateRenderService = mock(TemplateRenderService.class);
    when(coordinatesService.build(any())).thenReturn(new RequestCoordinates());
    TemplateExecutionService service =
        new TemplateExecutionService(
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
    when(miaTask.getType()).thenReturn(TaskType.builder().id(DomainConstants.Tasks.TASK_TYPE_ID_MORE_INFO_ADVANCED).build());
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
            .name("Plantilla descarrega")
            .properties(Map.of(DomainConstants.Tasks.PROPERTY_TEMPLATE_HTML, "<p>Hola</p>"))
            .type(org.sitmun.domain.task.type.TaskType.builder().id(DomainConstants.Tasks.TASK_TYPE_ID_TEMPLATE).build())
            .build();

    when(taskRepository.findById(16)).thenReturn(Optional.of(miaTask));
    when(taskRepository.findById(201)).thenReturn(Optional.of(templateTask));
    when(templateRenderService.renderPreview(eq("<p>Hola</p>"), any(), eq(201)))
        .thenReturn(TemplatePreviewResponseDto.builder().html("<p>Hola</p>").placeholders(List.of()).build());

    MoreInfoAdvancedRenderRequestDto request = new MoreInfoAdvancedRenderRequestDto();
    request.setMiaTaskIds(List.of(16));

    MoreInfoAdvancedRenderResponseDto result = service.renderMoreInfoAdvanced(request);

    assertThat(result.getTasks()).hasSize(1);
    assertThat(result.getTasks().get(0).getHtml())
        .contains("data-mia-export-template=\"true\"")
        .contains("data-mia-template-task-id=\"201\"");
  }

  @Test
  void renderMoreInfoAdvancedKeepsTemplateHtmlWhenLinkedApiChildFails() throws IOException {
    TaskRepository taskRepository = mock(TaskRepository.class);
    TaskRelationRepository taskRelationRepository = mock(TaskRelationRepository.class);
    ProxyConfigurationService proxyConfigurationService = mock(ProxyConfigurationService.class);
    HttpClientFactory httpClientFactory = mock(HttpClientFactory.class);
    SystemVariableResolver systemVariableResolver = mock(SystemVariableResolver.class);
    TemplateRenderService templateRenderService = mock(TemplateRenderService.class);
    TemplateRequestCoordinatesService coordinatesService = mock(TemplateRequestCoordinatesService.class);
    when(coordinatesService.build(any())).thenReturn(new RequestCoordinates());
    when(systemVariableResolver.resolve(eq("https://api.example.org/items"), any()))
        .thenReturn("https://api.example.org/items");
    TemplateExecutionService service =
        new TemplateExecutionService(
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
    when(miaTask.getType()).thenReturn(TaskType.builder().id(DomainConstants.Tasks.TASK_TYPE_ID_MORE_INFO_ADVANCED).build());
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
            .properties(Map.of(DomainConstants.Tasks.PROPERTY_TEMPLATE_HTML, "<article>{{consulta_api.html}} {{consulta_api.value}}</article>"))
            .type(org.sitmun.domain.task.type.TaskType.builder().id(DomainConstants.Tasks.TASK_TYPE_ID_TEMPLATE).build())
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
    WmsPayloadDto payload = WmsPayloadDto.builder().uri("https://api.example.org/items").method("GET").build();
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
    when(templateRenderService.renderPreview(eq("<article>{{consulta_api.html}} {{consulta_api.value}}</article>"), any(), eq(401)))
        .thenAnswer(
            invocation -> {
              Map<String, Object> context = invocation.getArgument(1);
              Map<String, Object> childContext = (Map<String, Object>) context.get("consulta_api");
              return TemplatePreviewResponseDto.builder()
                  .html("<article>" + childContext.get("html") + " " + childContext.get("value") + "</article>")
                  .placeholders(List.of())
                  .build();
            });

    MoreInfoAdvancedRenderRequestDto request = new MoreInfoAdvancedRenderRequestDto();
    request.setMiaTaskIds(List.of(16));
    request.setParameters(Map.of());

    MoreInfoAdvancedRenderResponseDto result = service.renderMoreInfoAdvanced(request);

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
    TemplateRequestCoordinatesService coordinatesService = mock(TemplateRequestCoordinatesService.class);
    when(coordinatesService.build(any())).thenReturn(new RequestCoordinates());
    TemplateExecutionService service =
        new TemplateExecutionService(
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
    when(miaTask.getType()).thenReturn(TaskType.builder().id(DomainConstants.Tasks.TASK_TYPE_ID_MORE_INFO_ADVANCED).build());
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
            .properties(Map.of(DomainConstants.Tasks.PROPERTY_TEMPLATE_HTML, "<article>{{bad_child.html}} {{bad_child.value}}</article>"))
            .type(org.sitmun.domain.task.type.TaskType.builder().id(DomainConstants.Tasks.TASK_TYPE_ID_TEMPLATE).build())
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
    when(templateRenderService.renderPreview(eq("<article>{{bad_child.html}} {{bad_child.value}}</article>"), any(), eq(451)))
        .thenAnswer(
            invocation -> {
              Map<String, Object> context = invocation.getArgument(1);
              Map<String, Object> childContext = (Map<String, Object>) context.get("bad_child");
              return TemplatePreviewResponseDto.builder()
                  .html("<article>" + childContext.get("html") + " " + childContext.get("value") + "</article>")
                  .placeholders(List.of())
                  .build();
            });

    MoreInfoAdvancedRenderRequestDto request = new MoreInfoAdvancedRenderRequestDto();
    request.setMiaTaskIds(List.of(16));
    request.setParameters(Map.of());

    MoreInfoAdvancedRenderResponseDto result = service.renderMoreInfoAdvanced(request);

    assertThat(result.getTasks()).hasSize(1);
    assertThat(result.getTasks().get(0).getHtml())
        .contains("sitmun-template-child-error")
        .contains("&lt;img src=x onerror=alert(1)&gt;")
        .contains("Unsupported linked task scope: unsupported&lt;script&gt;")
        .contains("[error: Unsupported linked task scope: unsupported&lt;script&gt;]")
        .doesNotContain("<img src=x onerror=alert(1)>")
        .doesNotContain("unsupported<script>")
        .doesNotContain("Error ejecutando tarea: Plantilla BAD_REQUEST");
  }

  @Test
  void executeLinkedTaskPropagatesTemplateLinkedChildFailures() {
    TaskRepository taskRepository = mock(TaskRepository.class);
    TaskRelationRepository taskRelationRepository = mock(TaskRelationRepository.class);
    TemplateRequestCoordinatesService coordinatesService = mock(TemplateRequestCoordinatesService.class);
    when(coordinatesService.build(any())).thenReturn(new RequestCoordinates());
    TemplateExecutionService service =
        new TemplateExecutionService(
            taskRepository,
            taskRelationRepository,
            mock(ProxyConfigurationService.class),
            mock(DatabaseConnectionService.class),
            mock(HttpClientFactory.class),
            mock(SystemVariableResolver.class),
            mock(TemplateRenderService.class),
            coordinatesService,
            new ObjectMapper());

    Task templateTask =
        Task.builder()
            .id(461)
            .properties(Map.of(DomainConstants.Tasks.PROPERTY_TEMPLATE_HTML, "<article>{{bad_child.html}}</article>"))
            .type(org.sitmun.domain.task.type.TaskType.builder().id(DomainConstants.Tasks.TASK_TYPE_ID_TEMPLATE).build())
            .build();
    Task unsupportedTask =
        Task.builder()
            .id(462)
            .properties(Map.of(DomainConstants.Tasks.PROPERTY_SCOPE, "unsupported-scope"))
            .build();
    TemplateTaskExecutionRequestDto requestDto = new TemplateTaskExecutionRequestDto();
    requestDto.setLinkedTaskId(461);

    when(taskRepository.findById(461)).thenReturn(Optional.of(templateTask));
    when(taskRelationRepository.findByTaskId(461))
        .thenReturn(
            List.of(
                TaskRelation.builder()
                    .id(1)
                    .task(templateTask)
                    .relationType("template-task")
                    .referenceAlias("bad_child")
                    .relatedTask(unsupportedTask)
                    .build()));

    assertThatThrownBy(() -> service.executeLinkedTask(requestDto))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(
            exception -> {
              ResponseStatusException responseStatusException = (ResponseStatusException) exception;
              assertThat(responseStatusException.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
              assertThat(responseStatusException.getReason())
                  .isEqualTo("Unsupported linked task scope: unsupported-scope");
            });
  }

  @Test
  void renderMoreInfoAdvancedPropagatesParentTemplateRenderBadRequest() {
    TaskRepository taskRepository = mock(TaskRepository.class);
    TaskRelationRepository taskRelationRepository = mock(TaskRelationRepository.class);
    TemplateRenderService templateRenderService = mock(TemplateRenderService.class);
    TemplateRequestCoordinatesService coordinatesService = mock(TemplateRequestCoordinatesService.class);
    when(coordinatesService.build(any())).thenReturn(new RequestCoordinates());
    TemplateExecutionService service =
        new TemplateExecutionService(
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
    when(miaTask.getType()).thenReturn(TaskType.builder().id(DomainConstants.Tasks.TASK_TYPE_ID_MORE_INFO_ADVANCED).build());
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
            .type(org.sitmun.domain.task.type.TaskType.builder().id(DomainConstants.Tasks.TASK_TYPE_ID_TEMPLATE).build())
            .build();

    when(taskRepository.findById(16)).thenReturn(Optional.of(miaTask));
    when(taskRepository.findById(471)).thenReturn(Optional.of(templateTask));
    when(taskRelationRepository.findByTaskId(471)).thenReturn(List.of());
    when(templateRenderService.renderPreview(eq("{{#if broken}}"), any(), eq(471)))
        .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid template syntax"));

    MoreInfoAdvancedRenderRequestDto request = new MoreInfoAdvancedRenderRequestDto();
    request.setMiaTaskIds(List.of(16));
    request.setParameters(Map.of());

    assertThatThrownBy(() -> service.renderMoreInfoAdvanced(request))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(
            exception -> {
              ResponseStatusException responseStatusException = (ResponseStatusException) exception;
              assertThat(responseStatusException.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
              assertThat(responseStatusException.getReason()).isEqualTo("Invalid template syntax");
            });
  }

  @Test
  void executeLinkedTaskMapsInvalidSqlTaskConfigurationToBadRequest() {
    TaskRepository taskRepository = mock(TaskRepository.class);
    ProxyConfigurationService proxyConfigurationService = mock(ProxyConfigurationService.class);
    TemplateRequestCoordinatesService coordinatesService = mock(TemplateRequestCoordinatesService.class);
    when(coordinatesService.build(any())).thenReturn(new RequestCoordinates());
    TemplateExecutionService service =
        new TemplateExecutionService(
            taskRepository,
            mock(TaskRelationRepository.class),
            proxyConfigurationService,
            mock(DatabaseConnectionService.class),
            mock(HttpClientFactory.class),
            mock(SystemVariableResolver.class),
            mock(TemplateRenderService.class),
            coordinatesService,
            new ObjectMapper());

    Task task =
        Task.builder()
            .id(32285)
            .properties(Map.of(DomainConstants.Tasks.PROPERTY_SCOPE, DomainConstants.Tasks.SCOPE_SQL_QUERY))
            .build();
    TemplateTaskExecutionRequestDto requestDto = new TemplateTaskExecutionRequestDto();
    requestDto.setLinkedTaskId(32285);

    when(taskRepository.findById(32285)).thenReturn(Optional.of(task));
    when(proxyConfigurationService.getConfiguration(any(), eq(0L), any()))
        .thenThrow(new BadRequestException("Bad request"));

    assertThatThrownBy(() -> service.executeLinkedTask(requestDto))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(
            exception -> {
              ResponseStatusException responseStatusException = (ResponseStatusException) exception;
              assertThat(responseStatusException.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
              assertThat(responseStatusException.getReason()).isEqualTo("Bad request");
            });
  }

  @Test
  void executeLinkedSqlTaskIncludesRowsInTemplateContextForIteration() {
    TaskRepository taskRepository = mock(TaskRepository.class);
    ProxyConfigurationService proxyConfigurationService = mock(ProxyConfigurationService.class);
    DatabaseConnectionService databaseConnectionService = mock(DatabaseConnectionService.class);
    TemplateRequestCoordinatesService coordinatesService = mock(TemplateRequestCoordinatesService.class);
    when(coordinatesService.build(any())).thenReturn(new RequestCoordinates());

    TemplateExecutionService service =
        new TemplateExecutionService(
            taskRepository,
            mock(TaskRelationRepository.class),
            proxyConfigurationService,
            databaseConnectionService,
            mock(HttpClientFactory.class),
            mock(SystemVariableResolver.class),
            mock(TemplateRenderService.class),
            coordinatesService,
            new ObjectMapper());

    Task task =
        Task.builder()
            .id(32281)
            .properties(Map.of(DomainConstants.Tasks.PROPERTY_SCOPE, DomainConstants.Tasks.SCOPE_SQL_QUERY))
            .build();
    TemplateTaskExecutionRequestDto requestDto = new TemplateTaskExecutionRequestDto();
    requestDto.setLinkedTaskId(32281);
    JdbcPayloadDto payload = JdbcPayloadDto.builder()
        .driver("org.postgresql.Driver")
        .uri("jdbc:postgresql://localhost/example")
        .user("user")
        .password("password")
        .sql("select * from layers")
        .build();
    List<Map<String, Object>> rows = List.of(
        Map.of("tui_tooltip", "Layer", "tui_id", 35),
        Map.of("tui_tooltip", "Other", "tui_id", 36));

    when(taskRepository.findById(32281)).thenReturn(Optional.of(task));
    when(proxyConfigurationService.getConfiguration(any(), eq(0L), any()))
        .thenReturn(ConfigProxyDto.builder().type("JDBC").payload(payload).build());
    when(databaseConnectionService.executeQuery(any(), eq("select * from layers"), any()))
        .thenReturn(rows);

    TemplateTaskExecutionResponseDto result = service.executeLinkedTask(requestDto);

    assertThat(result.getRows()).isEqualTo(rows);
    assertThat(result.getContext()).containsEntry("rows", rows);
    assertThat(result.getContext()).containsEntry("tui_tooltip", "Layer");
  }

  @Test
  void executeLinkedTaskBuildsApiUrlByResolvingPathTemplatesAndEncodingQueryValues()
      throws IOException {
    TaskRepository taskRepository = mock(TaskRepository.class);
    ProxyConfigurationService proxyConfigurationService = mock(ProxyConfigurationService.class);
    HttpClientFactory httpClientFactory = mock(HttpClientFactory.class);
    SystemVariableResolver systemVariableResolver = mock(SystemVariableResolver.class);
    TemplateRequestCoordinatesService coordinatesService = mock(TemplateRequestCoordinatesService.class);
    when(coordinatesService.build(any())).thenReturn(new RequestCoordinates());

    TemplateExecutionService service =
        new TemplateExecutionService(
            taskRepository,
            mock(TaskRelationRepository.class),
            proxyConfigurationService,
            mock(DatabaseConnectionService.class),
            httpClientFactory,
            systemVariableResolver,
            mock(TemplateRenderService.class),
            coordinatesService,
            new ObjectMapper());

    Task task =
        Task.builder()
            .id(32292)
            .properties(
                Map.of(DomainConstants.Tasks.PROPERTY_SCOPE, DomainConstants.Tasks.SCOPE_WEB_API_QUERY))
            .build();
    TemplateTaskExecutionRequestDto requestDto = new TemplateTaskExecutionRequestDto();
    requestDto.setLinkedTaskId(32292);
    LinkedHashMap<String, Object> requestParameters = new LinkedHashMap<>();
    requestParameters.put("capa", "agol_precio_m2");
    requestParameters.put("f", "pjson");
    requestParameters.put("returnGeometry", "false");
    requestParameters.put("where", "1=1");
    requestDto.setParameters(requestParameters);

    LinkedHashMap<String, String> payloadParameters = new LinkedHashMap<>();
    payloadParameters.put("f", "pjson");
    payloadParameters.put("returnGeometry", "false");
    payloadParameters.put("where", "1=1");

    WmsPayloadDto payload =
        WmsPayloadDto.builder()
            .uri(
                "https://services-eu1.arcgis.com/UpPGybwp9RK4YtZj/ArcGIS/rest/services/{capa}/FeatureServer/3/query")
            .method("GET")
            .parameters(payloadParameters)
            .build();
    ConfigProxyDto config = ConfigProxyDto.builder().type("API").payload(payload).build();

    when(taskRepository.findById(32292)).thenReturn(Optional.of(task));
    when(proxyConfigurationService.getConfiguration(any(), eq(0L), any())).thenReturn(config);
    when(systemVariableResolver.resolve(startsWith("https://services-eu1.arcgis.com/"), any()))
        .thenAnswer(invocation -> invocation.getArgument(0));
    Response response =
        new Response.Builder()
            .request(new Request.Builder().url("https://example.com").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(
                ResponseBody.create(
                    "{\"objectIdFieldName\":\"objectid\",\"uniqueIdField\":{\"name\":\"objectid\",\"isSystemMaintained\":true},\"features\":[{\"attributes\":{\"name_prov\":\"A Coruña\"}}],\"error\":{\"details\":[\"where invalid\"]}}",
                    okhttp3.MediaType.parse("application/json")))
            .build();
    when(httpClientFactory.executeRequest(any())).thenReturn(response);

    TemplateTaskExecutionResponseDto result = service.executeLinkedTask(requestDto);

    verify(httpClientFactory)
        .executeRequest(
            org.mockito.ArgumentMatchers.argThat(
                request -> {
                  String url = request.url().toString();
                  return url.startsWith(
                          "https://services-eu1.arcgis.com/UpPGybwp9RK4YtZj/ArcGIS/rest/services/agol_precio_m2/FeatureServer/3/query?")
                      && url.contains("f=pjson")
                      && url.contains("returnGeometry=false")
                      && url.contains("where=1%3D1")
                      && !url.contains("capa=");
                }));
    assertThat(result.getRows())
        .contains(
            Map.of("field", "objectIdFieldName", "value", "objectid"),
            Map.of("field", "uniqueIdField.name", "value", "objectid"),
            Map.of("field", "uniqueIdField.isSystemMaintained", "value", true),
            Map.of("field", "features[0].attributes.name_prov", "value", "A Coruña"),
            Map.of("field", "error.details[0]", "value", "where invalid"));
  }

  @Test
  void executeLinkedTaskExpandsApiPathTemplateParametersFromTaskCommandWhenPayloadUriIsNormalized()
      throws IOException {
    TaskRepository taskRepository = mock(TaskRepository.class);
    ProxyConfigurationService proxyConfigurationService = mock(ProxyConfigurationService.class);
    HttpClientFactory httpClientFactory = mock(HttpClientFactory.class);
    SystemVariableResolver systemVariableResolver = mock(SystemVariableResolver.class);
    TemplateRequestCoordinatesService coordinatesService = mock(TemplateRequestCoordinatesService.class);
    when(coordinatesService.build(any())).thenReturn(new RequestCoordinates());

    TemplateExecutionService service =
        new TemplateExecutionService(
            taskRepository,
            mock(TaskRelationRepository.class),
            proxyConfigurationService,
            mock(DatabaseConnectionService.class),
            httpClientFactory,
            systemVariableResolver,
            mock(TemplateRenderService.class),
            coordinatesService,
            new ObjectMapper());

    String command = "https://ide.cime.es/api_ide/Mobilitat/stops/distance/{longitud}/{latitud}";
    Task task =
        Task.builder()
            .id(32282)
            .properties(
                Map.of(
                    DomainConstants.Tasks.PROPERTY_SCOPE,
                    DomainConstants.Tasks.SCOPE_WEB_API_QUERY,
                    DomainConstants.Tasks.PROPERTY_COMMAND,
                    command))
            .build();
    TemplateTaskExecutionRequestDto requestDto = new TemplateTaskExecutionRequestDto();
    requestDto.setLinkedTaskId(32282);
    LinkedHashMap<String, Object> requestParameters = new LinkedHashMap<>();
    requestParameters.put("longitud", "4.2429139999999999");
    requestParameters.put("latitud", "39.869439999999997");
    requestDto.setParameters(requestParameters);

    WmsPayloadDto payload =
        WmsPayloadDto.builder()
            .uri("https://ide.cime.es/api_ide/Mobilitat/stops/distance/")
            .method("GET")
            .parameters(new LinkedHashMap<>())
            .build();
    ConfigProxyDto config = ConfigProxyDto.builder().type("API").payload(payload).build();

    when(taskRepository.findById(32282)).thenReturn(Optional.of(task));
    when(proxyConfigurationService.getConfiguration(any(), eq(0L), any())).thenReturn(config);
    when(systemVariableResolver.resolve(eq(command), any())).thenReturn(command);
    Response response =
        new Response.Builder()
            .request(new Request.Builder().url("https://ide.cime.es").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(ResponseBody.create("[]", okhttp3.MediaType.parse("application/json")))
            .build();
    when(httpClientFactory.executeRequest(any())).thenReturn(response);

    service.executeLinkedTask(requestDto);

    verify(httpClientFactory)
        .executeRequest(
            org.mockito.ArgumentMatchers.argThat(
                request -> {
                  String url = request.url().toString();
                  return url.equals(
                          "https://ide.cime.es/api_ide/Mobilitat/stops/distance/4.2429139999999999/39.869439999999997")
                      && !url.contains("?longitud=")
                      && !url.contains("?latitud=");
                }));
  }

  @Test
  void executeLinkedTaskRejectsUnsuccessfulApiResponse() throws IOException {
    TaskRepository taskRepository = mock(TaskRepository.class);
    ProxyConfigurationService proxyConfigurationService = mock(ProxyConfigurationService.class);
    HttpClientFactory httpClientFactory = mock(HttpClientFactory.class);
    SystemVariableResolver systemVariableResolver = mock(SystemVariableResolver.class);
    TemplateRequestCoordinatesService coordinatesService = mock(TemplateRequestCoordinatesService.class);
    when(coordinatesService.build(any())).thenReturn(new RequestCoordinates());

    TemplateExecutionService service =
        new TemplateExecutionService(
            taskRepository,
            mock(TaskRelationRepository.class),
            proxyConfigurationService,
            mock(DatabaseConnectionService.class),
            httpClientFactory,
            systemVariableResolver,
            mock(TemplateRenderService.class),
            coordinatesService,
            new ObjectMapper());

    Task task =
        Task.builder()
            .id(32282)
            .properties(
                Map.of(DomainConstants.Tasks.PROPERTY_SCOPE, DomainConstants.Tasks.SCOPE_WEB_API_QUERY))
            .build();
    TemplateTaskExecutionRequestDto requestDto = new TemplateTaskExecutionRequestDto();
    requestDto.setLinkedTaskId(32282);

    WmsPayloadDto payload =
        WmsPayloadDto.builder().uri("https://api.example.org/items").method("GET").build();
    ConfigProxyDto config = ConfigProxyDto.builder().type("API").payload(payload).build();

    when(taskRepository.findById(32282)).thenReturn(Optional.of(task));
    when(proxyConfigurationService.getConfiguration(any(), eq(0L), any())).thenReturn(config);
    when(systemVariableResolver.resolve(eq("https://api.example.org/items"), any()))
        .thenReturn("https://api.example.org/items");
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

    assertThatThrownBy(() -> service.executeLinkedTask(requestDto))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(
            exception -> {
              ResponseStatusException responseStatusException = (ResponseStatusException) exception;
              assertThat(responseStatusException.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
              assertThat(responseStatusException.getReason()).isEqualTo("API task returned HTTP 500");
            });
  }

  @Test
  void renderMoreInfoAdvancedKeepsRenderingWhenOneApiChildFails() throws IOException {
    TaskRepository taskRepository = mock(TaskRepository.class);
    ProxyConfigurationService proxyConfigurationService = mock(ProxyConfigurationService.class);
    HttpClientFactory httpClientFactory = mock(HttpClientFactory.class);
    TemplateRequestCoordinatesService coordinatesService = mock(TemplateRequestCoordinatesService.class);
    when(coordinatesService.build(any())).thenReturn(new RequestCoordinates());

    TemplateExecutionService service =
        new TemplateExecutionService(
            taskRepository,
            mock(TaskRelationRepository.class),
            proxyConfigurationService,
            mock(DatabaseConnectionService.class),
            httpClientFactory,
            mock(SystemVariableResolver.class),
            mock(TemplateRenderService.class),
            coordinatesService,
            new ObjectMapper());

    Task miaTask = mock(Task.class);
    when(miaTask.getId()).thenReturn(32310);
    when(miaTask.getName()).thenReturn("Tasca MIA prova");
    when(miaTask.getType())
        .thenReturn(TaskType.builder().id(DomainConstants.Tasks.TASK_TYPE_ID_MORE_INFO_ADVANCED).build());
    when(miaTask.getProperties()).thenReturn(Map.of("parentLayout", "scroll", "childTaskOrderIds", List.of(32308)));

    Task apiTask =
        Task.builder()
            .id(32308)
            .name("Consulta API")
            .properties(Map.of(DomainConstants.Tasks.PROPERTY_SCOPE, DomainConstants.Tasks.SCOPE_WEB_API_QUERY))
            .build();

    WmsPayloadDto payload =
        WmsPayloadDto.builder().uri("https://api.example.org/items").method("GET").build();
    ConfigProxyDto config = ConfigProxyDto.builder().type("API").payload(payload).build();
    Response response =
        new Response.Builder()
            .request(new Request.Builder().url("https://api.example.org/items").build())
            .protocol(Protocol.HTTP_1_1)
            .code(429)
            .message("Too Many Requests")
            .body(
                ResponseBody.create(
                    "{\"error\":\"rate limited\"}", okhttp3.MediaType.parse("application/json")))
            .build();

    when(taskRepository.findById(32310)).thenReturn(Optional.of(miaTask));
    when(taskRepository.findById(32308)).thenReturn(Optional.of(apiTask));
    when(proxyConfigurationService.getConfiguration(any(), eq(0L), any())).thenReturn(config);
    when(httpClientFactory.executeRequest(any())).thenReturn(response);

    MoreInfoAdvancedRenderRequestDto request = new MoreInfoAdvancedRenderRequestDto();
    request.setMiaTaskIds(List.of(32310));

    MoreInfoAdvancedRenderResponseDto result = service.renderMoreInfoAdvanced(request);

    assertThat(result.getTasks()).hasSize(1);
    assertThat(result.getTasks().get(0).getHtml()).contains("API task returned HTTP 429");
  }

  @Test
  void executeLinkedTaskFlattensJsonArrayApiResponse() throws IOException {
    TaskRepository taskRepository = mock(TaskRepository.class);
    ProxyConfigurationService proxyConfigurationService = mock(ProxyConfigurationService.class);
    HttpClientFactory httpClientFactory = mock(HttpClientFactory.class);
    SystemVariableResolver systemVariableResolver = mock(SystemVariableResolver.class);
    TemplateRequestCoordinatesService coordinatesService = mock(TemplateRequestCoordinatesService.class);
    when(coordinatesService.build(any())).thenReturn(new RequestCoordinates());

    TemplateExecutionService service =
        new TemplateExecutionService(
            taskRepository,
            mock(TaskRelationRepository.class),
            proxyConfigurationService,
            mock(DatabaseConnectionService.class),
            httpClientFactory,
            systemVariableResolver,
            mock(TemplateRenderService.class),
            coordinatesService,
            new ObjectMapper());

    Task task =
        Task.builder()
            .id(32282)
            .properties(
                Map.of(DomainConstants.Tasks.PROPERTY_SCOPE, DomainConstants.Tasks.SCOPE_WEB_API_QUERY))
            .build();
    TemplateTaskExecutionRequestDto requestDto = new TemplateTaskExecutionRequestDto();
    requestDto.setLinkedTaskId(32282);

    WmsPayloadDto payload =
        WmsPayloadDto.builder().uri("https://api.example.org/items").method("GET").build();
    ConfigProxyDto config = ConfigProxyDto.builder().type("API").payload(payload).build();

    when(taskRepository.findById(32282)).thenReturn(Optional.of(task));
    when(proxyConfigurationService.getConfiguration(any(), eq(0L), any())).thenReturn(config);
    when(systemVariableResolver.resolve(eq("https://api.example.org/items"), any()))
        .thenReturn("https://api.example.org/items");
    Response response =
        new Response.Builder()
            .request(new Request.Builder().url("https://api.example.org/items").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(
                ResponseBody.create(
                    "[{\"distance\":0,\"id\":705}]", okhttp3.MediaType.parse("application/json")))
            .build();
    when(httpClientFactory.executeRequest(any())).thenReturn(response);

    TemplateTaskExecutionResponseDto result = service.executeLinkedTask(requestDto);

    assertThat(result.getRows())
        .contains(
            Map.of("field", "items[0].distance", "value", 0),
            Map.of("field", "items[0].id", "value", 705));
  }

  @Test
  void executeLinkedTaskReturnsBinaryMetadataForConfiguredPdfApiResponse() throws IOException {
    TaskRepository taskRepository = mock(TaskRepository.class);
    ProxyConfigurationService proxyConfigurationService = mock(ProxyConfigurationService.class);
    HttpClientFactory httpClientFactory = mock(HttpClientFactory.class);
    SystemVariableResolver systemVariableResolver = mock(SystemVariableResolver.class);
    TemplateRequestCoordinatesService coordinatesService = mock(TemplateRequestCoordinatesService.class);
    when(coordinatesService.build(any())).thenReturn(new RequestCoordinates());

    TemplateExecutionService service =
        new TemplateExecutionService(
            taskRepository,
            mock(TaskRelationRepository.class),
            proxyConfigurationService,
            mock(DatabaseConnectionService.class),
            httpClientFactory,
            systemVariableResolver,
            mock(TemplateRenderService.class),
            coordinatesService,
            new ObjectMapper());

    Task task =
        Task.builder()
            .id(32315)
            .properties(
                Map.of(
                    DomainConstants.Tasks.PROPERTY_SCOPE,
                    DomainConstants.Tasks.SCOPE_WEB_API_QUERY,
                    DomainConstants.Tasks.PROPERTY_MIME_TYPE,
                    "application/pdf"))
            .build();
    TemplateTaskExecutionRequestDto requestDto = new TemplateTaskExecutionRequestDto();
    requestDto.setLinkedTaskId(32315);

    WmsPayloadDto payload =
        WmsPayloadDto.builder().uri("https://api.example.org/report.pdf").method("GET").build();
    ConfigProxyDto config = ConfigProxyDto.builder().type("API").payload(payload).build();

    when(taskRepository.findById(32315)).thenReturn(Optional.of(task));
    when(proxyConfigurationService.getConfiguration(any(), eq(0L), any())).thenReturn(config);
    when(systemVariableResolver.resolve(eq("https://api.example.org/report.pdf"), any()))
        .thenReturn("https://api.example.org/report.pdf");
    Response response =
        new Response.Builder()
            .request(new Request.Builder().url("https://api.example.org/report.pdf").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(
                ResponseBody.create(
                    "%PDF-1.7 raw binary contents", okhttp3.MediaType.parse("application/pdf")))
            .build();
    when(httpClientFactory.executeRequest(any())).thenReturn(response);

    TemplateTaskExecutionResponseDto result = service.executeLinkedTask(requestDto);

    assertThat(result.getResultType()).isEqualTo("resource");
    assertThat(result.getResourceUrl()).isEqualTo("https://api.example.org/report.pdf");
    assertThat(result.getContext())
        .containsEntry("contentUrl", "https://api.example.org/report.pdf")
        .containsEntry("url", "https://api.example.org/report.pdf")
        .containsEntry("mimeType", "application/pdf")
        .containsEntry("binary", true)
        .containsEntry("value", "[contenido binario]");
    assertThat(result.getRows()).contains(Map.of("field", "value", "value", "[contenido binario]"));
    assertThat(result.getRows()).noneMatch(row -> String.valueOf(row.get("value")).contains("%PDF"));
    assertThat(result.getContext()).doesNotContainValue("%PDF-1.7 raw binary contents");
  }

  @Test
  void executeLinkedTaskReturnsBinaryMetadataForImageApiResponse() throws IOException {
    TaskRepository taskRepository = mock(TaskRepository.class);
    ProxyConfigurationService proxyConfigurationService = mock(ProxyConfigurationService.class);
    HttpClientFactory httpClientFactory = mock(HttpClientFactory.class);
    SystemVariableResolver systemVariableResolver = mock(SystemVariableResolver.class);
    TemplateRequestCoordinatesService coordinatesService = mock(TemplateRequestCoordinatesService.class);
    when(coordinatesService.build(any())).thenReturn(new RequestCoordinates());

    TemplateExecutionService service =
        new TemplateExecutionService(
            taskRepository,
            mock(TaskRelationRepository.class),
            proxyConfigurationService,
            mock(DatabaseConnectionService.class),
            httpClientFactory,
            systemVariableResolver,
            mock(TemplateRenderService.class),
            coordinatesService,
            new ObjectMapper());

    Task task =
        Task.builder()
            .id(32317)
            .properties(Map.of(DomainConstants.Tasks.PROPERTY_SCOPE, DomainConstants.Tasks.SCOPE_WEB_API_QUERY))
            .build();
    TemplateTaskExecutionRequestDto requestDto = new TemplateTaskExecutionRequestDto();
    requestDto.setLinkedTaskId(32317);

    WmsPayloadDto payload =
        WmsPayloadDto.builder().uri("https://api.example.org/image.jpg").method("GET").build();
    ConfigProxyDto config = ConfigProxyDto.builder().type("API").payload(payload).build();

    when(taskRepository.findById(32317)).thenReturn(Optional.of(task));
    when(proxyConfigurationService.getConfiguration(any(), eq(0L), any())).thenReturn(config);
    when(systemVariableResolver.resolve(eq("https://api.example.org/image.jpg"), any()))
        .thenReturn("https://api.example.org/image.jpg");
    Response response =
        new Response.Builder()
            .request(new Request.Builder().url("https://api.example.org/image.jpg").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(
                ResponseBody.create(
                    new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff, 0x00, 0x10},
                    okhttp3.MediaType.parse("image/jpeg")))
            .build();
    when(httpClientFactory.executeRequest(any())).thenReturn(response);

    TemplateTaskExecutionResponseDto result = service.executeLinkedTask(requestDto);

    assertThat(result.getResultType()).isEqualTo("resource");
    assertThat(result.getResourceUrl()).isEqualTo("https://api.example.org/image.jpg");
    assertThat(result.getContext())
        .containsEntry("contentUrl", "https://api.example.org/image.jpg")
        .containsEntry("url", "https://api.example.org/image.jpg")
        .containsEntry("mimeType", "image/jpeg")
        .containsEntry("binary", true)
        .containsEntry("value", "[contenido binario]");
    assertThat(result.getRows()).contains(Map.of("field", "value", "value", "[contenido binario]"));
  }

  @Test
  void executeLinkedTaskPreservesAlreadyEncodedApiUrlForBinaryImageResponse() throws IOException {
    TaskRepository taskRepository = mock(TaskRepository.class);
    ProxyConfigurationService proxyConfigurationService = mock(ProxyConfigurationService.class);
    HttpClientFactory httpClientFactory = mock(HttpClientFactory.class);
    SystemVariableResolver systemVariableResolver = mock(SystemVariableResolver.class);
    TemplateRequestCoordinatesService coordinatesService = mock(TemplateRequestCoordinatesService.class);
    when(coordinatesService.build(any())).thenReturn(new RequestCoordinates());

    TemplateExecutionService service =
        new TemplateExecutionService(
            taskRepository,
            mock(TaskRelationRepository.class),
            proxyConfigurationService,
            mock(DatabaseConnectionService.class),
            httpClientFactory,
            systemVariableResolver,
            mock(TemplateRenderService.class),
            coordinatesService,
            new ObjectMapper());

    String imageUrl =
        "https://raw.githubusercontent.com/sitmun/community/master/logotip%20SITMUN%20JPG/horitzontal/01.principal-horit-normal.jpg";
    Task task =
        Task.builder()
            .id(32317)
            .properties(Map.of(DomainConstants.Tasks.PROPERTY_SCOPE, DomainConstants.Tasks.SCOPE_WEB_API_QUERY))
            .build();
    TemplateTaskExecutionRequestDto requestDto = new TemplateTaskExecutionRequestDto();
    requestDto.setLinkedTaskId(32317);

    WmsPayloadDto payload = WmsPayloadDto.builder().uri(imageUrl).method("GET").build();
    ConfigProxyDto config = ConfigProxyDto.builder().type("API").payload(payload).build();

    when(taskRepository.findById(32317)).thenReturn(Optional.of(task));
    when(proxyConfigurationService.getConfiguration(any(), eq(0L), any())).thenReturn(config);
    when(systemVariableResolver.resolve(eq(imageUrl), any())).thenReturn(imageUrl);
    when(httpClientFactory.executeRequest(any()))
        .thenAnswer(
            invocation -> {
              Request request = invocation.getArgument(0);
              boolean exactUrl = imageUrl.equals(request.url().toString());
              return new Response.Builder()
                  .request(request)
                  .protocol(Protocol.HTTP_1_1)
                  .code(exactUrl ? 200 : 404)
                  .message(exactUrl ? "OK" : "Not Found")
                  .body(
                      exactUrl
                          ? ResponseBody.create(
                              new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff},
                              okhttp3.MediaType.parse("image/jpeg"))
                          : ResponseBody.create("404: Not Found", okhttp3.MediaType.parse("text/plain")))
                  .build();
            });

    TemplateTaskExecutionResponseDto result = service.executeLinkedTask(requestDto);

    assertThat(result.getResultType()).isEqualTo("resource");
    assertThat(result.getResourceUrl()).isEqualTo(imageUrl);
    assertThat(result.getContext())
        .containsEntry("contentUrl", imageUrl)
        .containsEntry("mimeType", "image/jpeg")
        .containsEntry("binary", true);
  }

  @Test
  void executeLinkedTaskTreatsUnknownNonTextMimeFamiliesAsBinaryWithoutReadingBody()
      throws IOException {
    TaskRepository taskRepository = mock(TaskRepository.class);
    ProxyConfigurationService proxyConfigurationService = mock(ProxyConfigurationService.class);
    HttpClientFactory httpClientFactory = mock(HttpClientFactory.class);
    SystemVariableResolver systemVariableResolver = mock(SystemVariableResolver.class);
    TemplateRequestCoordinatesService coordinatesService = mock(TemplateRequestCoordinatesService.class);
    when(coordinatesService.build(any())).thenReturn(new RequestCoordinates());

    TemplateExecutionService service =
        new TemplateExecutionService(
            taskRepository,
            mock(TaskRelationRepository.class),
            proxyConfigurationService,
            mock(DatabaseConnectionService.class),
            httpClientFactory,
            systemVariableResolver,
            mock(TemplateRenderService.class),
            coordinatesService,
            new ObjectMapper());

    Task task =
        Task.builder()
            .id(32318)
            .properties(Map.of(DomainConstants.Tasks.PROPERTY_SCOPE, DomainConstants.Tasks.SCOPE_WEB_API_QUERY))
            .build();
    TemplateTaskExecutionRequestDto requestDto = new TemplateTaskExecutionRequestDto();
    requestDto.setLinkedTaskId(32318);

    WmsPayloadDto payload =
        WmsPayloadDto.builder().uri("https://api.example.org/font.woff2").method("GET").build();
    ConfigProxyDto config = ConfigProxyDto.builder().type("API").payload(payload).build();

    when(taskRepository.findById(32318)).thenReturn(Optional.of(task));
    when(proxyConfigurationService.getConfiguration(any(), eq(0L), any())).thenReturn(config);
    when(systemVariableResolver.resolve(eq("https://api.example.org/font.woff2"), any()))
        .thenReturn("https://api.example.org/font.woff2");
    Response response =
        new Response.Builder()
            .request(new Request.Builder().url("https://api.example.org/font.woff2").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(new ThrowingStringResponseBody("font/woff2"))
            .build();
    when(httpClientFactory.executeRequest(any())).thenReturn(response);

    TemplateTaskExecutionResponseDto result = service.executeLinkedTask(requestDto);

    assertThat(result.getResultType()).isEqualTo("resource");
    assertThat(result.getContext())
        .containsEntry("mimeType", "font/woff2")
        .containsEntry("binary", true)
        .containsEntry("value", "[contenido binario]");
    assertThat(result.getRows()).contains(Map.of("field", "value", "value", "[contenido binario]"));
  }

  @Test
  void executeLinkedTaskDoesNotExposeDirectUrlForSecuredBinaryApiResponse() throws IOException {
    TaskRepository taskRepository = mock(TaskRepository.class);
    ProxyConfigurationService proxyConfigurationService = mock(ProxyConfigurationService.class);
    HttpClientFactory httpClientFactory = mock(HttpClientFactory.class);
    SystemVariableResolver systemVariableResolver = mock(SystemVariableResolver.class);
    TemplateRequestCoordinatesService coordinatesService = mock(TemplateRequestCoordinatesService.class);
    when(coordinatesService.build(any())).thenReturn(new RequestCoordinates());

    TemplateExecutionService service =
        new TemplateExecutionService(
            taskRepository,
            mock(TaskRelationRepository.class),
            proxyConfigurationService,
            mock(DatabaseConnectionService.class),
            httpClientFactory,
            systemVariableResolver,
            mock(TemplateRenderService.class),
            coordinatesService,
            new ObjectMapper());

    Task task =
        Task.builder()
            .id(32319)
            .properties(Map.of(DomainConstants.Tasks.PROPERTY_SCOPE, DomainConstants.Tasks.SCOPE_WEB_API_QUERY))
            .build();
    TemplateTaskExecutionRequestDto requestDto = new TemplateTaskExecutionRequestDto();
    requestDto.setLinkedTaskId(32319);

    WmsPayloadDto payload =
        WmsPayloadDto.builder()
            .uri("https://api.example.org/secure/report.pdf")
            .method("GET")
            .security(HttpSecurityDto.builder().username("user").password("secret").build())
            .build();
    ConfigProxyDto config = ConfigProxyDto.builder().type("API").payload(payload).build();

    when(taskRepository.findById(32319)).thenReturn(Optional.of(task));
    when(proxyConfigurationService.getConfiguration(any(), eq(0L), any())).thenReturn(config);
    when(systemVariableResolver.resolve(eq("https://api.example.org/secure/report.pdf"), any()))
        .thenReturn("https://api.example.org/secure/report.pdf");
    Response response =
        new Response.Builder()
            .request(new Request.Builder().url("https://api.example.org/secure/report.pdf").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(
                ResponseBody.create(
                    new byte[] {0x25, 0x50, 0x44, 0x46}, okhttp3.MediaType.parse("application/pdf")))
            .build();
    when(httpClientFactory.executeRequest(any())).thenReturn(response);

    TemplateTaskExecutionResponseDto result = service.executeLinkedTask(requestDto);

    assertThat(result.getResultType()).isEqualTo("resource");
    assertThat(result.getResourceUrl()).isNull();
    assertThat(result.getContext())
        .containsEntry("mimeType", "application/pdf")
        .containsEntry("binary", true)
        .containsEntry("embeddable", false)
        .containsEntry("value", "[contenido binario]");
    assertThat(result.getContext()).containsEntry("contentUrl", null).containsEntry("url", null);
    assertThat(result.getContext()).doesNotContainValue("https://api.example.org/secure/report.pdf");
  }

  private static final class ThrowingStringResponseBody extends ResponseBody {
    private final okhttp3.MediaType contentType;

    private ThrowingStringResponseBody(String contentType) {
      this.contentType = okhttp3.MediaType.parse(contentType);
    }

    @Override
    public okhttp3.MediaType contentType() {
      return contentType;
    }

    @Override
    public long contentLength() {
      return 4;
    }

    @Override
    public BufferedSource source() {
      return Okio.buffer(
          new Source() {
            @Override
            public long read(okio.Buffer sink, long byteCount) {
              throw new AssertionError("Binary response body must not be read as text");
            }

            @Override
            public Timeout timeout() {
              return Timeout.NONE;
            }

            @Override
            public void close() {}
          });
    }
  }

  @Test
  void executeLinkedTaskRendersNestedTemplatesCompletelyInBackend() {
    TaskRepository taskRepository = mock(TaskRepository.class);
    TaskRelationRepository taskRelationRepository = mock(TaskRelationRepository.class);
    TemplateRenderService templateRenderService = mock(TemplateRenderService.class);
    TemplateRequestCoordinatesService coordinatesService = mock(TemplateRequestCoordinatesService.class);
    SystemVariableResolver systemVariableResolver = mock(SystemVariableResolver.class);
    when(coordinatesService.build(any())).thenReturn(new RequestCoordinates());
    when(systemVariableResolver.resolve(eq("https://example.com/{slug}"), any()))
        .thenReturn("https://example.com/{slug}");

    TemplateExecutionService service =
        new TemplateExecutionService(
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
            .properties(Map.of(DomainConstants.Tasks.PROPERTY_TEMPLATE_HTML, "<div>{{plantilla_hija.html}}</div>"))
            .type(org.sitmun.domain.task.type.TaskType.builder().id(DomainConstants.Tasks.TASK_TYPE_ID_TEMPLATE).build())
            .build();
    Task childTemplate =
        Task.builder()
            .id(201)
            .properties(Map.of(DomainConstants.Tasks.PROPERTY_TEMPLATE_HTML, "<p>{{consulta_url.url}}</p>"))
            .type(org.sitmun.domain.task.type.TaskType.builder().id(DomainConstants.Tasks.TASK_TYPE_ID_TEMPLATE).build())
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
    when(templateRenderService.renderPreview(eq("<p>{{consulta_url.url}}</p>"), any(), eq(200)))
        .thenReturn(TemplatePreviewResponseDto.builder().html("<p>https://example.com/abc</p>").placeholders(List.of()).build());
    when(templateRenderService.renderPreview(eq("<div>{{plantilla_hija.html}}</div>"), any(), eq(200)))
        .thenReturn(TemplatePreviewResponseDto.builder().html("<div><p>https://example.com/abc</p></div>").placeholders(List.of()).build());

    TemplateTaskExecutionResponseDto result = service.executeLinkedTask(requestDto);

    assertThat(result.getResultType()).isEqualTo("template");
    assertThat(result.getContext()).containsEntry("html", "<div><p>https://example.com/abc</p></div>");
  }

  @Test
  void executeLinkedTaskUsesConfiguredTemplateParameterDefaultWhenExecutionValueMissing() {
    TaskRepository taskRepository = mock(TaskRepository.class);
    TemplateRenderService templateRenderService = mock(TemplateRenderService.class);
    TemplateRequestCoordinatesService coordinatesService = mock(TemplateRequestCoordinatesService.class);
    when(coordinatesService.build(any())).thenReturn(new RequestCoordinates());

    TemplateExecutionService service =
        new TemplateExecutionService(
            taskRepository,
            mock(TaskRelationRepository.class),
            mock(ProxyConfigurationService.class),
            mock(DatabaseConnectionService.class),
            mock(HttpClientFactory.class),
            mock(SystemVariableResolver.class),
            templateRenderService,
            coordinatesService,
            new ObjectMapper());

    Task templateTask =
        Task.builder()
            .id(801)
            .properties(
                Map.of(
                    DomainConstants.Tasks.PROPERTY_TEMPLATE_HTML,
                    "<h1>{{$title}}</h1>",
                    DomainConstants.Tasks.PROPERTY_PARAMETERS,
                    List.of(Map.of("name", "title", "type", DomainConstants.Tasks.TYPE_STRING, "value", "Default title"))))
            .type(org.sitmun.domain.task.type.TaskType.builder().id(DomainConstants.Tasks.TASK_TYPE_ID_TEMPLATE).build())
            .build();
    TemplateTaskExecutionRequestDto requestDto = new TemplateTaskExecutionRequestDto();
    requestDto.setLinkedTaskId(801);

    when(taskRepository.findById(801)).thenReturn(Optional.of(templateTask));
    when(templateRenderService.renderPreview(eq("<h1>{{$title}}</h1>"), any(), eq(801)))
        .thenAnswer(
            invocation -> {
              Map<String, Object> context = invocation.getArgument(1);
              return TemplatePreviewResponseDto.builder()
                  .html("<h1>" + context.get("$title") + "</h1>")
                  .placeholders(List.of())
                  .build();
            });

    TemplateTaskExecutionResponseDto result = service.executeLinkedTask(requestDto);

    assertThat(result.getContext()).containsEntry("html", "<h1>Default title</h1>");
  }

  @Test
  void executeLinkedTaskPrefersExecutionTemplateParameterOverConfiguredDefault() {
    TaskRepository taskRepository = mock(TaskRepository.class);
    TemplateRenderService templateRenderService = mock(TemplateRenderService.class);
    TemplateRequestCoordinatesService coordinatesService = mock(TemplateRequestCoordinatesService.class);
    when(coordinatesService.build(any())).thenReturn(new RequestCoordinates());

    TemplateExecutionService service =
        new TemplateExecutionService(
            taskRepository,
            mock(TaskRelationRepository.class),
            mock(ProxyConfigurationService.class),
            mock(DatabaseConnectionService.class),
            mock(HttpClientFactory.class),
            mock(SystemVariableResolver.class),
            templateRenderService,
            coordinatesService,
            new ObjectMapper());

    Task templateTask =
        Task.builder()
            .id(802)
            .properties(
                Map.of(
                    DomainConstants.Tasks.PROPERTY_TEMPLATE_HTML,
                    "<h1>{{$title}}</h1>",
                    DomainConstants.Tasks.PROPERTY_PARAMETERS,
                    List.of(Map.of("name", "title", "type", DomainConstants.Tasks.TYPE_STRING, "value", "Default title"))))
            .type(org.sitmun.domain.task.type.TaskType.builder().id(DomainConstants.Tasks.TASK_TYPE_ID_TEMPLATE).build())
            .build();
    TemplateTaskExecutionRequestDto requestDto = new TemplateTaskExecutionRequestDto();
    requestDto.setLinkedTaskId(802);
    requestDto.setParameters(Map.of("title", "Mapped title"));

    when(taskRepository.findById(802)).thenReturn(Optional.of(templateTask));
    when(templateRenderService.renderPreview(eq("<h1>{{$title}}</h1>"), any(), eq(802)))
        .thenAnswer(
            invocation -> {
              Map<String, Object> context = invocation.getArgument(1);
              return TemplatePreviewResponseDto.builder()
                  .html("<h1>" + context.get("$title") + "</h1>")
                  .placeholders(List.of())
                  .build();
            });

    TemplateTaskExecutionResponseDto result = service.executeLinkedTask(requestDto);

    assertThat(result.getContext()).containsEntry("html", "<h1>Mapped title</h1>");
  }

  @Test
  void executeLinkedTaskRejectsTemplateNestingDeeperThanThreeLevels() {
    TaskRepository taskRepository = mock(TaskRepository.class);
    TaskRelationRepository taskRelationRepository = mock(TaskRelationRepository.class);
    TemplateRenderService templateRenderService = mock(TemplateRenderService.class);
    TemplateRequestCoordinatesService coordinatesService = mock(TemplateRequestCoordinatesService.class);
    when(coordinatesService.build(any())).thenReturn(new RequestCoordinates());

    TemplateExecutionService service =
        new TemplateExecutionService(
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
            .type(org.sitmun.domain.task.type.TaskType.builder().id(DomainConstants.Tasks.TASK_TYPE_ID_TEMPLATE).build())
            .build();
    Task template2 =
        Task.builder()
            .id(302)
            .properties(Map.of(DomainConstants.Tasks.PROPERTY_TEMPLATE_HTML, "{{task_303.html}}"))
            .type(org.sitmun.domain.task.type.TaskType.builder().id(DomainConstants.Tasks.TASK_TYPE_ID_TEMPLATE).build())
            .build();
    Task template3 =
        Task.builder()
            .id(303)
            .properties(Map.of(DomainConstants.Tasks.PROPERTY_TEMPLATE_HTML, "{{task_304.html}}"))
            .type(org.sitmun.domain.task.type.TaskType.builder().id(DomainConstants.Tasks.TASK_TYPE_ID_TEMPLATE).build())
            .build();
    Task template4 =
        Task.builder()
            .id(304)
            .properties(Map.of(DomainConstants.Tasks.PROPERTY_TEMPLATE_HTML, "<p>too deep</p>"))
            .type(org.sitmun.domain.task.type.TaskType.builder().id(DomainConstants.Tasks.TASK_TYPE_ID_TEMPLATE).build())
            .build();

    TemplateTaskExecutionRequestDto requestDto = new TemplateTaskExecutionRequestDto();
    requestDto.setLinkedTaskId(301);

    when(taskRepository.findById(301)).thenReturn(Optional.of(template1));
    when(taskRelationRepository.findByTaskId(301))
        .thenReturn(List.of(TaskRelation.builder().id(1).task(template1).relationType("template-nested").relatedTask(template2).build()));
    when(taskRelationRepository.findByTaskId(302))
        .thenReturn(List.of(TaskRelation.builder().id(2).task(template2).relationType("template-nested").relatedTask(template3).build()));
    when(taskRelationRepository.findByTaskId(303))
        .thenReturn(List.of(TaskRelation.builder().id(3).task(template3).relationType("template-nested").relatedTask(template4).build()));

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
}
