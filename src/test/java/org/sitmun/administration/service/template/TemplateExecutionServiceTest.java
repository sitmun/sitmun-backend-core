package org.sitmun.administration.service.template;

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
import org.junit.jupiter.api.Test;
import org.sitmun.administration.controller.dto.MoreInfoAdvancedRenderRequestDto;
import org.sitmun.administration.controller.dto.MoreInfoAdvancedRenderResponseDto;
import org.sitmun.administration.controller.dto.TemplatePreviewResponseDto;
import org.sitmun.administration.controller.dto.TemplateTaskExecutionRequestDto;
import org.sitmun.administration.controller.dto.TemplateTaskExecutionResponseDto;
import org.sitmun.administration.service.database.DatabaseConnectionService;
import org.sitmun.administration.service.extractor.HttpClientFactory;
import org.sitmun.authorization.proxy.dto.ConfigProxyDto;
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
            coordinatesService);

    TaskUI miaUi = mock(TaskUI.class);
    when(miaUi.getName()).thenReturn("sitna.moreInfoAdvanced");

    Task miaTask = mock(Task.class);
    when(miaTask.getId()).thenReturn(16);
    when(miaTask.getName()).thenReturn("MIA parent");
    when(miaTask.getUi()).thenReturn(miaUi);
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
            coordinatesService);

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
            coordinatesService);

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
            coordinatesService);

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
            coordinatesService);

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
            coordinatesService);

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
