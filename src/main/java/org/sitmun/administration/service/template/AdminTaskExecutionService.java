package org.sitmun.administration.service.template;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Credentials;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.sitmun.administration.controller.dto.TemplateTaskExecutionRequestDto;
import org.sitmun.administration.controller.dto.TemplateTaskExecutionResponseDto;
import org.sitmun.administration.service.database.DatabaseConnectionService;
import org.sitmun.administration.service.database.tester.DatabaseSQLException;
import org.sitmun.authorization.proxy.dto.ConfigProxyDto;
import org.sitmun.authorization.proxy.dto.ConfigProxyRequestDto;
import org.sitmun.authorization.proxy.dto.HttpSecurityDto;
import org.sitmun.authorization.proxy.exception.BadRequestException;
import org.sitmun.authorization.proxy.protocols.jdbc.JdbcPayloadDto;
import org.sitmun.authorization.proxy.protocols.wms.WmsPayloadDto;
import org.sitmun.authorization.proxy.service.ProxyConfigurationService;
import org.sitmun.authorization.proxy.service.RequestCoordinates;
import org.sitmun.domain.DomainConstants;
import org.sitmun.domain.database.DatabaseConnection;
import org.sitmun.domain.task.Task;
import org.sitmun.domain.task.TaskRepository;
import org.sitmun.infrastructure.variables.SystemVariableResolver;
import org.sitmun.administration.service.extractor.HttpClientFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminTaskExecutionService {

  private static final Pattern URI_TEMPLATE_PARAMETER_PATTERN = Pattern.compile("\\{([^/{}]+)}");

  private final TaskRepository taskRepository;
  private final ProxyConfigurationService proxyConfigurationService;
  private final DatabaseConnectionService databaseConnectionService;
  private final HttpClientFactory httpClientFactory;
  private final SystemVariableResolver systemVariableResolver;

  private final ObjectMapper objectMapper = new ObjectMapper();

  public TemplateTaskExecutionResponseDto executeLinkedTask(TemplateTaskExecutionRequestDto requestDto) {
    Task task =
        taskRepository
            .findById(requestDto.getLinkedTaskId())
            .orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));

    if (task.getType() != null
        && Integer.valueOf(DomainConstants.Tasks.TASK_TYPE_ID_TEMPLATE).equals(task.getType().getId())) {
      return TemplateTaskExecutionResponseDto.builder()
          .taskId(task.getId())
          .status("PENDING")
          .resultType("template")
          .parameters(requestDto.getParameters())
          .context(Collections.singletonMap("html", readTemplateHtml(task)))
          .rows(Collections.emptyList())
          .resourceUrl(null)
          .flattenedContextKeys(List.of("html"))
          .build();
    }

    Map<String, String> parameters = stringifyParameters(requestDto.getParameters());
    String scope = String.valueOf(task.getProperties().get(DomainConstants.Tasks.PROPERTY_SCOPE));

    if (DomainConstants.Tasks.SCOPE_SQL_QUERY.equalsIgnoreCase(scope)) {
      return executeSqlTask(task, parameters);
    }
    if (DomainConstants.Tasks.SCOPE_WEB_API_QUERY.equalsIgnoreCase(scope)) {
      return executeApiTask(task, parameters);
    }
    if (DomainConstants.Tasks.SCOPE_URL_QUERY.equalsIgnoreCase(scope)
        || DomainConstants.Tasks.SCOPE_RESOURCE_QUERY.equalsIgnoreCase(scope)) {
      return resolveUrlTask(task, parameters, scope);
    }

    throw new ResponseStatusException(
        org.springframework.http.HttpStatus.BAD_REQUEST,
        "Unsupported linked task scope: " + scope);
  }

  private TemplateTaskExecutionResponseDto executeSqlTask(Task task, Map<String, String> parameters) {
    RequestCoordinates coordinates = new RequestCoordinates();
    ConfigProxyRequestDto configRequest =
        ConfigProxyRequestDto.builder()
            .appId(0)
            .terId(0)
            .type(DomainConstants.Proxy.TYPE_SQL)
            .typeId(task.getId())
            .parameters(parameters)
            .build();

    ConfigProxyDto config;
    try {
      config = proxyConfigurationService.getConfiguration(configRequest, 0, coordinates);
      proxyConfigurationService.applyDecorators(config, configRequest, coordinates);
    } catch (BadRequestException exception) {
      throw new ResponseStatusException(
          org.springframework.http.HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
    }

    JdbcPayloadDto payload = (JdbcPayloadDto) config.getPayload();
    DatabaseConnection connection =
        DatabaseConnection.builder()
            .driver(payload.getDriver())
            .url(payload.getUri())
            .user(payload.getUser())
            .password(payload.getPassword())
            .build();

    List<Map<String, Object>> rows;
    try {
      rows = databaseConnectionService.executeQuery(connection, payload.getSql(), payload.getParameters());
    } catch (DatabaseSQLException exception) {
      throw new ResponseStatusException(
          org.springframework.http.HttpStatus.BAD_REQUEST,
          exception.getCause() != null ? exception.getCause().getMessage() : exception.getMessage(),
          exception);
    }
    Map<String, Object> context = buildRowAndParameterContext(rows, parameters);

    return TemplateTaskExecutionResponseDto.builder()
        .taskId(task.getId())
        .status("COMPLETED")
        .resultType("table")
        .parameters(new LinkedHashMap<>(parameters))
        .context(context)
        .rows(rows)
        .resourceUrl(null)
        .flattenedContextKeys(new ArrayList<>(context.keySet()))
        .build();
  }

  private TemplateTaskExecutionResponseDto executeApiTask(Task task, Map<String, String> parameters) {
    RequestCoordinates coordinates = new RequestCoordinates();
    ConfigProxyRequestDto configRequest =
        ConfigProxyRequestDto.builder()
            .appId(0)
            .terId(0)
            .type(DomainConstants.Proxy.TYPE_API)
            .typeId(task.getId())
            .parameters(parameters)
            .build();

    ConfigProxyDto config;
    try {
      config = proxyConfigurationService.getConfiguration(configRequest, 0, coordinates);
      proxyConfigurationService.applyDecorators(config, configRequest, coordinates);
    } catch (BadRequestException exception) {
      throw new ResponseStatusException(
          org.springframework.http.HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
    }

    WmsPayloadDto payload = (WmsPayloadDto) config.getPayload();
    String requestUrl = buildHttpRequestUrl(payload, parameters);
    try {
      Request.Builder requestBuilder = new Request.Builder().url(requestUrl);
      applySecurity(payload.getSecurity(), requestBuilder);

      String method = StringUtils.hasText(payload.getMethod()) ? payload.getMethod().toUpperCase() : "GET";
      if ("POST".equals(method)) {
        RequestBody requestBody =
            RequestBody.create(payload.getBody() == null ? "" : payload.getBody(), okhttp3.MediaType.parse(MediaType.APPLICATION_JSON_VALUE));
        requestBuilder.post(requestBody);
      } else {
        requestBuilder.get();
      }

      try (Response response = httpClientFactory.executeRequest(requestBuilder.build())) {
        String body = response.body() != null ? response.body().string() : "";
        Map<String, Object> bodyContext = normalizeBodyToContext(body);
        List<Map<String, Object>> rows = flattenContextToRows(bodyContext);
        Map<String, Object> context = buildApiContext(bodyContext, parameters);

        return TemplateTaskExecutionResponseDto.builder()
            .taskId(task.getId())
            .status("COMPLETED")
            .resultType("table")
            .parameters(new LinkedHashMap<>(parameters))
            .context(context)
            .rows(rows)
            .resourceUrl(null)
            .flattenedContextKeys(extractFlattenedKeys(bodyContext, parameters))
            .build();
      }
    } catch (IOException e) {
      throw new ResponseStatusException(
          org.springframework.http.HttpStatus.BAD_GATEWAY, "Failed to execute API task", e);
    }
  }

  private TemplateTaskExecutionResponseDto resolveUrlTask(
      Task task, Map<String, String> parameters, String scope) {
    String command =
        String.valueOf(task.getProperties().getOrDefault(DomainConstants.Tasks.PROPERTY_COMMAND, ""));
    String resolved = resolveTemplateUrl(command, parameters);

    Map<String, Object> context = new LinkedHashMap<>();
    context.put("url", resolved);
    parameters.forEach((key, value) -> context.put("$" + key, value));

    return TemplateTaskExecutionResponseDto.builder()
        .taskId(task.getId())
        .status("COMPLETED")
        .resultType(DomainConstants.Tasks.SCOPE_RESOURCE_QUERY.equalsIgnoreCase(scope) ? "resource" : "url")
        .parameters(new LinkedHashMap<>(parameters))
        .context(context)
        .rows(Collections.emptyList())
        .resourceUrl(resolved)
        .flattenedContextKeys(new ArrayList<>(context.keySet()))
        .build();
  }

  private Map<String, String> stringifyParameters(Map<String, Object> parameters) {
    if (parameters == null || parameters.isEmpty()) {
      return new LinkedHashMap<>();
    }

    Map<String, String> normalized = new LinkedHashMap<>();
    parameters.forEach((key, value) -> normalized.put(key, value == null ? "" : String.valueOf(value)));
    return normalized;
  }

  private Map<String, Object> buildRowAndParameterContext(
      List<Map<String, Object>> rows, Map<String, String> parameters) {
    Map<String, Object> context = new LinkedHashMap<>();
    parameters.forEach((key, value) -> context.put("$" + key, value));
    if (!rows.isEmpty()) {
      rows.get(0).forEach(context::put);
    }
    return context;
  }

  private Map<String, Object> buildApiContext(
      Map<String, Object> bodyContext, Map<String, String> parameters) {
    Map<String, Object> context = new LinkedHashMap<>();
    parameters.forEach((key, value) -> context.put("$" + key, value));
    if (bodyContext != null) {
      context.putAll(bodyContext);
    }
    return context;
  }

  private Map<String, Object> normalizeBodyToContext(String body) throws IOException {
    if (!StringUtils.hasText(body)) {
      return Collections.emptyMap();
    }

    String trimmed = body.trim();
    if (trimmed.startsWith("[")) {
      List<?> values = objectMapper.readValue(trimmed, new TypeReference<List<?>>() {});
      return Collections.singletonMap("items", values);
    }

    if (trimmed.startsWith("{")) {
      return objectMapper.readValue(trimmed, new TypeReference<Map<String, Object>>() {});
    }

    return Collections.singletonMap("value", body);
  }

  private Map<String, Object> castMap(Map<?, ?> mapValue) {
    Map<String, Object> cast = new LinkedHashMap<>();
    mapValue.forEach((key, value) -> cast.put(String.valueOf(key), value));
    return cast;
  }

  private List<Map<String, Object>> flattenContextToRows(Map<String, Object> context) {
    List<Map<String, Object>> rows = new ArrayList<>();
    flattenValue(null, context, rows);
    return rows;
  }

  private List<String> extractFlattenedKeys(
      Map<String, Object> bodyContext, Map<String, String> parameters) {
    List<String> keys = new ArrayList<>();
    parameters.forEach((key, value) -> keys.add("$" + key));
    flattenKeys(null, bodyContext, keys);
    return keys;
  }

  @SuppressWarnings("unchecked")
  private void flattenValue(String path, Object value, List<Map<String, Object>> rows) {
    if (value instanceof Map<?, ?> mapValue) {
      mapValue.forEach((key, nestedValue) -> flattenValue(appendPath(path, String.valueOf(key)), nestedValue, rows));
      return;
    }

    if (value instanceof List<?> listValue) {
      for (int index = 0; index < listValue.size(); index++) {
        flattenValue(appendIndex(path, index), listValue.get(index), rows);
      }
      return;
    }

    if (path != null) {
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("field", path);
      row.put("value", value);
      rows.add(row);
    }
  }

  private void flattenKeys(String path, Object value, List<String> keys) {
    if (value instanceof Map<?, ?> mapValue) {
      mapValue.forEach((key, nestedValue) -> flattenKeys(appendPath(path, String.valueOf(key)), nestedValue, keys));
      return;
    }

    if (value instanceof List<?> listValue) {
      for (int index = 0; index < listValue.size(); index++) {
        flattenKeys(appendIndex(path, index), listValue.get(index), keys);
      }
      return;
    }

    if (path != null) {
      keys.add(path);
    }
  }

  private String appendPath(String parent, String segment) {
    return parent == null || parent.isEmpty() ? segment : parent + "." + segment;
  }

  private String appendIndex(String parent, int index) {
    return (parent == null ? "" : parent) + "[" + index + "]";
  }

  private void applySecurity(HttpSecurityDto security, Request.Builder requestBuilder) {
    if (security == null) {
      return;
    }

    if (security.getHeaders() != null) {
      security.getHeaders().forEach(requestBuilder::addHeader);
    }
    if (StringUtils.hasText(security.getUsername()) && StringUtils.hasText(security.getPassword())) {
      requestBuilder.addHeader("Authorization", Credentials.basic(security.getUsername(), security.getPassword(), StandardCharsets.UTF_8));
    }
  }

  private String buildHttpRequestUrl(WmsPayloadDto payload, Map<String, String> executionParameters) {
    Map<String, String> templateParameters = new LinkedHashMap<>();
    if (payload.getParameters() != null) {
      templateParameters.putAll(payload.getParameters());
    }
    if (executionParameters != null) {
      templateParameters.putAll(executionParameters);
    }

    String resolvedUri = resolveTemplateUrl(payload.getUri(), templateParameters);
    UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(resolvedUri);

    Set<String> uriTemplateParameters = extractUriTemplateParameters(payload.getUri());
    Map<String, String> queryParameters = new LinkedHashMap<>();
    if (payload.getParameters() != null) {
      queryParameters.putAll(payload.getParameters());
    }
    if (executionParameters != null) {
      executionParameters.forEach(queryParameters::putIfAbsent);
    }
    uriTemplateParameters.forEach(queryParameters::remove);

    if (!queryParameters.isEmpty()) {
      queryParameters.forEach(builder::queryParam);
    }

    return builder.build().encode().toUriString();
  }

  private Set<String> extractUriTemplateParameters(String uri) {
    if (!StringUtils.hasText(uri)) {
      return Collections.emptySet();
    }

    Matcher matcher = URI_TEMPLATE_PARAMETER_PATTERN.matcher(uri);
    Set<String> placeholders = new LinkedHashSet<>();
    while (matcher.find()) {
      placeholders.add(matcher.group(1));
    }
    return placeholders;
  }

  private String resolveTemplateUrl(String command, Map<String, String> parameters) {
    String resolved = systemVariableResolver.resolve(command, new RequestCoordinates());
    for (Map.Entry<String, String> entry : parameters.entrySet()) {
      resolved = resolved.replace("{" + entry.getKey() + "}", entry.getValue());
      resolved = resolved.replace("${" + entry.getKey() + "}", entry.getValue());
    }
    return resolved;
  }

  private String readTemplateHtml(Task task) {
    Object raw = task.getProperties().get(DomainConstants.Tasks.PROPERTY_TEMPLATE_HTML);
    return raw != null ? String.valueOf(raw) : "";
  }
}
