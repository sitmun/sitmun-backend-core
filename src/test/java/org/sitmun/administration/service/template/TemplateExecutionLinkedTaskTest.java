package org.sitmun.administration.service.template;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
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
import org.sitmun.infrastructure.variables.SystemVariableResolver;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

class TemplateExecutionLinkedTaskTest extends TemplateExecutionServiceTestFixtures {
  @Test
  void executeLinkedTaskReturnsNoDataWhenTaskIsUnauthorized() {
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

    Task task =
        Task.builder()
            .id(13)
            .properties(
                Map.of(DomainConstants.Tasks.PROPERTY_SCOPE, DomainConstants.Tasks.SCOPE_SQL_QUERY))
            .build();
    when(taskRepository.findById(13)).thenReturn(Optional.of(task));

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setParameter("lang", "en");
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken("viewer", "n/a", List.of()));
    try {
      TemplateTaskExecutionRequestDto executionRequest = new TemplateTaskExecutionRequestDto();
      executionRequest.setLinkedTaskId(13);
      executionRequest.setParameters(Map.of());
      executionRequest.setAppId(5);
      executionRequest.setTerId(7);

      TemplateTaskExecutionResponseDto result = service.executeLinkedTask(executionRequest);

      assertThat(result.getStatus()).isEqualTo("COMPLETED");
      assertThat(result.getRows()).isEmpty();
      assertThat(result.getContext()).containsEntry("value", "No data");
    } finally {
      RequestContextHolder.resetRequestAttributes();
      SecurityContextHolder.clearContext();
    }
  }

  @Test
  void executeLinkedTaskPropagatesTemplateLinkedChildFailures() {
    TaskRepository taskRepository = mock(TaskRepository.class);
    TaskRelationRepository taskRelationRepository = mock(TaskRelationRepository.class);
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
            mock(TemplateRenderService.class),
            coordinatesService,
            new ObjectMapper());

    Task templateTask =
        Task.builder()
            .id(461)
            .properties(
                Map.of(
                    DomainConstants.Tasks.PROPERTY_TEMPLATE_HTML,
                    "<article>{{bad_child.html}}</article>"))
            .type(
                org.sitmun.domain.task.type.TaskType.builder()
                    .id(DomainConstants.Tasks.TASK_TYPE_ID_TEMPLATE)
                    .build())
            .build();
    Task unsupportedTask =
        Task.builder()
            .id(462)
            .properties(Map.of(DomainConstants.Tasks.PROPERTY_SCOPE, "unsupported-scope"))
            .build();
    TemplateTaskExecutionRequestDto requestDto = new TemplateTaskExecutionRequestDto();
    requestDto.setAppId(5);
    requestDto.setTerId(7);
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
  void executeLinkedTaskMapsInvalidSqlTaskConfigurationToBadRequest() {
    TaskRepository taskRepository = mock(TaskRepository.class);
    ProxyConfigurationService proxyConfigurationService = mock(ProxyConfigurationService.class);
    TemplateRequestCoordinatesService coordinatesService =
        mock(TemplateRequestCoordinatesService.class);
    when(coordinatesService.build(any(), any()))
        .thenReturn(requestCoordinatesWithUserPermission(7));
    TemplateExecutionService service =
        newService(
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
            .properties(
                Map.of(DomainConstants.Tasks.PROPERTY_SCOPE, DomainConstants.Tasks.SCOPE_SQL_QUERY))
            .build();
    TemplateTaskExecutionRequestDto requestDto = new TemplateTaskExecutionRequestDto();
    requestDto.setAppId(5);
    requestDto.setTerId(7);
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
    TemplateRequestCoordinatesService coordinatesService =
        mock(TemplateRequestCoordinatesService.class);
    when(coordinatesService.build(any(), any()))
        .thenReturn(requestCoordinatesWithUserPermission(7));

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

    Task task =
        Task.builder()
            .id(32281)
            .properties(
                Map.of(DomainConstants.Tasks.PROPERTY_SCOPE, DomainConstants.Tasks.SCOPE_SQL_QUERY))
            .build();
    TemplateTaskExecutionRequestDto requestDto = new TemplateTaskExecutionRequestDto();
    requestDto.setAppId(5);
    requestDto.setTerId(7);
    requestDto.setLinkedTaskId(32281);
    JdbcPayloadDto payload =
        JdbcPayloadDto.builder()
            .driver("org.postgresql.Driver")
            .uri("jdbc:postgresql://localhost/example")
            .user("user")
            .password("password")
            .sql("select * from layers")
            .build();
    List<Map<String, Object>> rows =
        List.of(
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
    TemplateRequestCoordinatesService coordinatesService =
        mock(TemplateRequestCoordinatesService.class);
    when(coordinatesService.build(any(), any()))
        .thenReturn(requestCoordinatesWithUserPermission(7));

    TemplateExecutionService service =
        newService(
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
                Map.of(
                    DomainConstants.Tasks.PROPERTY_SCOPE,
                    DomainConstants.Tasks.SCOPE_WEB_API_QUERY))
            .build();
    TemplateTaskExecutionRequestDto requestDto = new TemplateTaskExecutionRequestDto();
    requestDto.setAppId(5);
    requestDto.setTerId(7);
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
    TemplateRequestCoordinatesService coordinatesService =
        mock(TemplateRequestCoordinatesService.class);
    when(coordinatesService.build(any(), any()))
        .thenReturn(requestCoordinatesWithUserPermission(7));

    TemplateExecutionService service =
        newService(
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
    requestDto.setAppId(5);
    requestDto.setTerId(7);
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
    TemplateRequestCoordinatesService coordinatesService =
        mock(TemplateRequestCoordinatesService.class);
    when(coordinatesService.build(any(), any()))
        .thenReturn(requestCoordinatesWithUserPermission(7));

    TemplateExecutionService service =
        newService(
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
                Map.of(
                    DomainConstants.Tasks.PROPERTY_SCOPE,
                    DomainConstants.Tasks.SCOPE_WEB_API_QUERY))
            .build();
    TemplateTaskExecutionRequestDto requestDto = new TemplateTaskExecutionRequestDto();
    requestDto.setAppId(5);
    requestDto.setTerId(7);
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
              assertThat(responseStatusException.getReason())
                  .isEqualTo("API task returned HTTP 500");
            });
  }

  @Test
  void executeLinkedTaskFlattensJsonArrayApiResponse() throws IOException {
    TaskRepository taskRepository = mock(TaskRepository.class);
    ProxyConfigurationService proxyConfigurationService = mock(ProxyConfigurationService.class);
    HttpClientFactory httpClientFactory = mock(HttpClientFactory.class);
    SystemVariableResolver systemVariableResolver = mock(SystemVariableResolver.class);
    TemplateRequestCoordinatesService coordinatesService =
        mock(TemplateRequestCoordinatesService.class);
    when(coordinatesService.build(any(), any()))
        .thenReturn(requestCoordinatesWithUserPermission(7));

    TemplateExecutionService service =
        newService(
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
                Map.of(
                    DomainConstants.Tasks.PROPERTY_SCOPE,
                    DomainConstants.Tasks.SCOPE_WEB_API_QUERY))
            .build();
    TemplateTaskExecutionRequestDto requestDto = new TemplateTaskExecutionRequestDto();
    requestDto.setAppId(5);
    requestDto.setTerId(7);
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
    TemplateRequestCoordinatesService coordinatesService =
        mock(TemplateRequestCoordinatesService.class);
    when(coordinatesService.build(any(), any()))
        .thenReturn(requestCoordinatesWithUserPermission(7));

    TemplateExecutionService service =
        newService(
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
    requestDto.setAppId(5);
    requestDto.setTerId(7);
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

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setParameter("lang", "en");
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    try {
      TemplateTaskExecutionResponseDto result = service.executeLinkedTask(requestDto);

      assertThat(result.getResultType()).isEqualTo("resource");
      assertThat(result.getResourceUrl()).isEqualTo("https://api.example.org/report.pdf");
      assertThat(result.getContext())
          .containsEntry("contentUrl", "https://api.example.org/report.pdf")
          .containsEntry("url", "https://api.example.org/report.pdf")
          .containsEntry("mimeType", "application/pdf")
          .containsEntry("binary", true)
          .containsEntry("value", "[binary content]");
      assertThat(result.getRows()).contains(Map.of("field", "value", "value", "[binary content]"));
      assertThat(result.getRows())
          .noneMatch(row -> String.valueOf(row.get("value")).contains("%PDF"));
      assertThat(result.getContext()).doesNotContainValue("%PDF-1.7 raw binary contents");
    } finally {
      RequestContextHolder.resetRequestAttributes();
    }
  }

  @Test
  void executeLinkedTaskReturnsBinaryMetadataForImageApiResponse() throws IOException {
    TaskRepository taskRepository = mock(TaskRepository.class);
    ProxyConfigurationService proxyConfigurationService = mock(ProxyConfigurationService.class);
    HttpClientFactory httpClientFactory = mock(HttpClientFactory.class);
    SystemVariableResolver systemVariableResolver = mock(SystemVariableResolver.class);
    TemplateRequestCoordinatesService coordinatesService =
        mock(TemplateRequestCoordinatesService.class);
    when(coordinatesService.build(any(), any()))
        .thenReturn(requestCoordinatesWithUserPermission(7));

    TemplateExecutionService service =
        newService(
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
            .properties(
                Map.of(
                    DomainConstants.Tasks.PROPERTY_SCOPE,
                    DomainConstants.Tasks.SCOPE_WEB_API_QUERY))
            .build();
    TemplateTaskExecutionRequestDto requestDto = new TemplateTaskExecutionRequestDto();
    requestDto.setAppId(5);
    requestDto.setTerId(7);
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
        .containsEntry("value", "[binary content]");
    assertThat(result.getRows()).contains(Map.of("field", "value", "value", "[binary content]"));
  }

  @Test
  void executeLinkedTaskPreservesAlreadyEncodedApiUrlForBinaryImageResponse() throws IOException {
    TaskRepository taskRepository = mock(TaskRepository.class);
    ProxyConfigurationService proxyConfigurationService = mock(ProxyConfigurationService.class);
    HttpClientFactory httpClientFactory = mock(HttpClientFactory.class);
    SystemVariableResolver systemVariableResolver = mock(SystemVariableResolver.class);
    TemplateRequestCoordinatesService coordinatesService =
        mock(TemplateRequestCoordinatesService.class);
    when(coordinatesService.build(any(), any()))
        .thenReturn(requestCoordinatesWithUserPermission(7));

    TemplateExecutionService service =
        newService(
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
            .properties(
                Map.of(
                    DomainConstants.Tasks.PROPERTY_SCOPE,
                    DomainConstants.Tasks.SCOPE_WEB_API_QUERY))
            .build();
    TemplateTaskExecutionRequestDto requestDto = new TemplateTaskExecutionRequestDto();
    requestDto.setAppId(5);
    requestDto.setTerId(7);
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
                          : ResponseBody.create(
                              "404: Not Found", okhttp3.MediaType.parse("text/plain")))
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
    TemplateRequestCoordinatesService coordinatesService =
        mock(TemplateRequestCoordinatesService.class);
    when(coordinatesService.build(any(), any()))
        .thenReturn(requestCoordinatesWithUserPermission(7));

    TemplateExecutionService service =
        newService(
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
            .properties(
                Map.of(
                    DomainConstants.Tasks.PROPERTY_SCOPE,
                    DomainConstants.Tasks.SCOPE_WEB_API_QUERY))
            .build();
    TemplateTaskExecutionRequestDto requestDto = new TemplateTaskExecutionRequestDto();
    requestDto.setAppId(5);
    requestDto.setTerId(7);
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
        .containsEntry("value", "[binary content]");
    assertThat(result.getRows()).contains(Map.of("field", "value", "value", "[binary content]"));
  }

  @Test
  void executeLinkedTaskDoesNotExposeDirectUrlForSecuredBinaryApiResponse() throws IOException {
    TaskRepository taskRepository = mock(TaskRepository.class);
    ProxyConfigurationService proxyConfigurationService = mock(ProxyConfigurationService.class);
    HttpClientFactory httpClientFactory = mock(HttpClientFactory.class);
    SystemVariableResolver systemVariableResolver = mock(SystemVariableResolver.class);
    TemplateRequestCoordinatesService coordinatesService =
        mock(TemplateRequestCoordinatesService.class);
    when(coordinatesService.build(any(), any()))
        .thenReturn(requestCoordinatesWithUserPermission(7));

    TemplateExecutionService service =
        newService(
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
            .properties(
                Map.of(
                    DomainConstants.Tasks.PROPERTY_SCOPE,
                    DomainConstants.Tasks.SCOPE_WEB_API_QUERY))
            .build();
    TemplateTaskExecutionRequestDto requestDto = new TemplateTaskExecutionRequestDto();
    requestDto.setAppId(5);
    requestDto.setTerId(7);
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
                    new byte[] {0x25, 0x50, 0x44, 0x46},
                    okhttp3.MediaType.parse("application/pdf")))
            .build();
    when(httpClientFactory.executeRequest(any())).thenReturn(response);

    TemplateTaskExecutionResponseDto result = service.executeLinkedTask(requestDto);

    assertThat(result.getResultType()).isEqualTo("resource");
    assertThat(result.getResourceUrl()).isNull();
    assertThat(result.getContext())
        .containsEntry("mimeType", "application/pdf")
        .containsEntry("binary", true)
        .containsEntry("embeddable", false)
        .containsEntry("value", "[binary content]")
        .containsEntry(
            "accessMessage", "Binary content cannot be embedded: server authentication required");
    assertThat(result.getContext()).containsEntry("contentUrl", null).containsEntry("url", null);
    assertThat(result.getContext())
        .doesNotContainValue("https://api.example.org/secure/report.pdf");
  }

  @Test
  void executeLinkedTaskUsesConfiguredTemplateParameterDefaultWhenExecutionValueMissing() {
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

    Task templateTask =
        Task.builder()
            .id(801)
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
            .type(
                org.sitmun.domain.task.type.TaskType.builder()
                    .id(DomainConstants.Tasks.TASK_TYPE_ID_TEMPLATE)
                    .build())
            .build();
    TemplateTaskExecutionRequestDto requestDto = new TemplateTaskExecutionRequestDto();
    requestDto.setAppId(5);
    requestDto.setTerId(7);
    requestDto.setLinkedTaskId(801);

    when(taskRepository.findById(801)).thenReturn(Optional.of(templateTask));
    when(templateRenderService.renderPreview(
            eq("<h1>{{$title}}</h1>"), any(), any(), any(), any(), any()))
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

    Task templateTask =
        Task.builder()
            .id(802)
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
            .type(
                org.sitmun.domain.task.type.TaskType.builder()
                    .id(DomainConstants.Tasks.TASK_TYPE_ID_TEMPLATE)
                    .build())
            .build();
    TemplateTaskExecutionRequestDto requestDto = new TemplateTaskExecutionRequestDto();
    requestDto.setAppId(5);
    requestDto.setTerId(7);
    requestDto.setLinkedTaskId(802);
    requestDto.setParameters(Map.of("title", "Mapped title"));

    when(taskRepository.findById(802)).thenReturn(Optional.of(templateTask));
    when(templateRenderService.renderPreview(
            eq("<h1>{{$title}}</h1>"), any(), any(), any(), any(), any()))
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
  void executeLinkedTaskAdminIgnoresAvailability() {
    TaskRepository taskRepository = mock(TaskRepository.class);
    TemplateRequestCoordinatesService coordinatesService =
        mock(TemplateRequestCoordinatesService.class);
    when(coordinatesService.buildForCurrentUser()).thenReturn(new RequestCoordinates());
    when(taskRepository.findByRolesAndTerritory(any(), any())).thenReturn(List.of());
    SystemVariableResolver systemVariableResolver = mock(SystemVariableResolver.class);
    when(systemVariableResolver.resolve(any(), any())).thenReturn(null);
    TemplateExecutionService service =
        newService(
            taskRepository,
            mock(TaskRelationRepository.class),
            mock(ProxyConfigurationService.class),
            mock(DatabaseConnectionService.class),
            mock(HttpClientFactory.class),
            systemVariableResolver,
            mock(TemplateRenderService.class),
            coordinatesService,
            new ObjectMapper());

    Task task =
        Task.builder()
            .id(13)
            .properties(
                Map.of(
                    DomainConstants.Tasks.PROPERTY_SCOPE,
                    DomainConstants.Tasks.SCOPE_URL,
                    DomainConstants.Tasks.PROPERTY_COMMAND,
                    "https://example.org/{id}"))
            .build();
    when(taskRepository.findById(13)).thenReturn(Optional.of(task));

    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                "admin", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    try {
      TemplateTaskExecutionRequestDto requestDto = new TemplateTaskExecutionRequestDto();
      requestDto.setLinkedTaskId(13);
      requestDto.setParameters(Map.of("id", "1"));

      TemplateTaskExecutionResponseDto result = service.executeLinkedTask(requestDto);

      assertThat(result.getStatus()).isEqualTo("COMPLETED");
      assertThat(result.getResourceUrl()).contains("https://example.org/");
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  @Test
  void executeLinkedTaskWithoutCoordinatesUsesBuildForCurrentUser() {
    TaskRepository taskRepository = mock(TaskRepository.class);
    TemplateRequestCoordinatesService coordinatesService =
        mock(TemplateRequestCoordinatesService.class);
    RequestCoordinates previewCoordinates = new RequestCoordinates();
    when(coordinatesService.buildForCurrentUser()).thenReturn(previewCoordinates);
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

    Task task =
        Task.builder()
            .id(13)
            .properties(
                Map.of(
                    DomainConstants.Tasks.PROPERTY_SCOPE,
                    DomainConstants.Tasks.SCOPE_URL,
                    DomainConstants.Tasks.PROPERTY_COMMAND,
                    "https://example.org/{id}"))
            .build();
    when(taskRepository.findById(13)).thenReturn(Optional.of(task));

    TemplateTaskExecutionRequestDto requestDto = new TemplateTaskExecutionRequestDto();
    requestDto.setLinkedTaskId(13);
    requestDto.setParameters(Map.of("id", "1"));

    TemplateTaskExecutionResponseDto result = service.executeLinkedTask(requestDto);

    assertThat(result.getStatus()).isEqualTo("COMPLETED");
    verify(coordinatesService).buildForCurrentUser();
    verify(coordinatesService, never()).build(any(), any());
  }
}
