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
import org.sitmun.administration.controller.dto.TemplateTaskExecutionRequestDto;
import org.sitmun.administration.controller.dto.TemplateTaskExecutionResponseDto;
import org.sitmun.administration.service.extractor.HttpClientFactory;
import org.sitmun.authorization.proxy.dto.ConfigProxyDto;
import org.sitmun.authorization.proxy.exception.BadRequestException;
import org.sitmun.authorization.proxy.protocols.wms.WmsPayloadDto;
import org.sitmun.authorization.proxy.service.ProxyConfigurationService;
import org.sitmun.domain.DomainConstants;
import org.sitmun.domain.task.Task;
import org.sitmun.domain.task.TaskRepository;
import org.sitmun.infrastructure.variables.SystemVariableResolver;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class AdminTaskExecutionServiceTest {

  @Test
  void executeLinkedTaskMapsInvalidSqlTaskConfigurationToBadRequest() {
    TaskRepository taskRepository = mock(TaskRepository.class);
    ProxyConfigurationService proxyConfigurationService = mock(ProxyConfigurationService.class);

    AdminTaskExecutionService service =
        new AdminTaskExecutionService(
            taskRepository,
            proxyConfigurationService,
            null,
            null,
            mock(SystemVariableResolver.class));

    Task task =
        Task.builder()
            .id(32285)
            .properties(Map.of(DomainConstants.Tasks.PROPERTY_SCOPE, DomainConstants.Tasks.SCOPE_SQL_QUERY))
            .build();
    TemplateTaskExecutionRequestDto requestDto = new TemplateTaskExecutionRequestDto();
    requestDto.setLinkedTaskId(32285);

    when(taskRepository.findById(32285)).thenReturn(Optional.of(task));
    when(proxyConfigurationService.getConfiguration(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(0L), org.mockito.ArgumentMatchers.any()))
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
  void executeLinkedTaskBuildsApiUrlByResolvingPathTemplatesAndEncodingQueryValues()
      throws IOException {
    TaskRepository taskRepository = mock(TaskRepository.class);
    ProxyConfigurationService proxyConfigurationService = mock(ProxyConfigurationService.class);
    HttpClientFactory httpClientFactory = mock(HttpClientFactory.class);
    SystemVariableResolver systemVariableResolver = mock(SystemVariableResolver.class);

    AdminTaskExecutionService service =
        new AdminTaskExecutionService(
            taskRepository,
            proxyConfigurationService,
            null,
            httpClientFactory,
            systemVariableResolver);

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
    assertThat(result.getContext())
        .containsEntry("objectIdFieldName", "objectid")
        .containsKey("uniqueIdField")
        .containsKey("features")
        .containsKey("error");
    assertThat(result.getFlattenedContextKeys())
        .contains(
            "objectIdFieldName",
            "uniqueIdField.name",
            "uniqueIdField.isSystemMaintained",
            "features[0].attributes.name_prov",
            "error.details[0]");
  }
}
