package org.sitmun.administration.service.template.childdata;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
import org.sitmun.administration.service.database.DatabaseConnectionService;
import org.sitmun.administration.service.database.tester.DatabaseSQLException;
import org.sitmun.administration.service.extractor.HttpClientFactory;
import org.sitmun.administration.service.i18n.CurrentRequestLanguageResolver;
import org.sitmun.administration.service.i18n.LiteralTranslationResolver;
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
import org.sitmun.infrastructure.variables.SystemVariableResolver;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class TemplateChildDataService {
  private static final String BINARY_VALUE_PLACEHOLDER_LITERAL = "[binary content]";
  private static final String BINARY_ACCESS_MESSAGE_LITERAL =
      "Binary content cannot be embedded: server authentication required";
  private static final Pattern URI_TEMPLATE_PARAMETER_PATTERN = Pattern.compile("\\{([^/{}]+)}");
  private static final String TABLE = "table";
  private final ProxyConfigurationService proxyConfigurationService;
  private final DatabaseConnectionService databaseConnectionService;
  private final HttpClientFactory httpClientFactory;
  private final SystemVariableResolver systemVariableResolver;
  private final LiteralTranslationResolver literalTranslationResolver;
  private final CurrentRequestLanguageResolver currentRequestLanguageResolver;
  private final ObjectMapper objectMapper;

  private boolean mayExecuteDataPlane(
      ChildDataRequest request, ConfigProxyRequestDto configRequest) {
    if (request.getPrincipalKind() == PrincipalKind.ADMIN) {
      return true;
    }
    return proxyConfigurationService.validateUserAccess(
        configRequest, resolveUsername(request.getCoordinates()));
  }

  private String resolveUsername(RequestCoordinates coordinates) {
    if (coordinates != null
        && coordinates.getUser() != null
        && StringUtils.hasText(coordinates.getUser().getUsername())) {
      return coordinates.getUser().getUsername();
    }
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !StringUtils.hasText(authentication.getName())) {
      return null;
    }
    return authentication.getName();
  }

  public ChildDataResult executeSql(ChildDataRequest request) {
    Task task = request.getTask();
    Map<String, String> parameters =
        request.getParameters() == null ? new LinkedHashMap<>() : request.getParameters();
    RequestCoordinates coordinates = request.getCoordinates();
    ConfigProxyRequestDto configRequest =
        ConfigProxyRequestDto.builder()
            .appId(coordinates.getApplication() != null ? coordinates.getApplication().getId() : 0)
            .terId(coordinates.getTerritory() != null ? coordinates.getTerritory().getId() : 0)
            .type(DomainConstants.Proxy.TYPE_SQL)
            .typeId(task.getId())
            .parameters(parameters)
            .build();
    if (!mayExecuteDataPlane(request, configRequest)) {
      return ChildDataResult.noData();
    }
    ConfigProxyDto config;
    try {
      config = proxyConfigurationService.getConfiguration(configRequest, 0, coordinates);
      proxyConfigurationService.applyDecorators(config, configRequest, coordinates);
    } catch (BadRequestException exception) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
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
      rows =
          databaseConnectionService.executeQuery(
              connection, payload.getSql(), payload.getParameters());
    } catch (DatabaseSQLException exception) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          exception.getCause() != null ? exception.getCause().getMessage() : exception.getMessage(),
          exception);
    }
    Map<String, Object> context = buildRowAndParameterContext(rows, parameters);
    return ChildDataResult.builder()
        .outcome(ChildDataOutcome.OK)
        .resultType(TABLE)
        .context(context)
        .rows(rows)
        .resourceUrl(null)
        .build();
  }

  public ChildDataResult executeApi(ChildDataRequest request) {
    Task task = request.getTask();
    Map<String, String> parameters =
        request.getParameters() == null ? new LinkedHashMap<>() : request.getParameters();
    RequestCoordinates coordinates = request.getCoordinates();
    ConfigProxyRequestDto configRequest =
        ConfigProxyRequestDto.builder()
            .appId(coordinates.getApplication() != null ? coordinates.getApplication().getId() : 0)
            .terId(coordinates.getTerritory() != null ? coordinates.getTerritory().getId() : 0)
            .type(DomainConstants.Proxy.TYPE_API)
            .typeId(task.getId())
            .parameters(parameters)
            .build();
    if (!mayExecuteDataPlane(request, configRequest)) {
      return ChildDataResult.noData();
    }
    ConfigProxyDto config;
    try {
      config = proxyConfigurationService.getConfiguration(configRequest, 0, coordinates);
      proxyConfigurationService.applyDecorators(config, configRequest, coordinates);
    } catch (BadRequestException exception) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
    }
    WmsPayloadDto payload = (WmsPayloadDto) config.getPayload();
    String requestUrl =
        buildHttpRequestUrl(payload, parameters, coordinates, readTaskCommand(task));
    String sanitizedRequestUrl = sanitizeRequestUrlForLogging(requestUrl, parameters);
    try {
      Request.Builder requestBuilder = new Request.Builder().url(requestUrl);
      applySecurity(payload.getSecurity(), requestBuilder);
      String method =
          StringUtils.hasText(payload.getMethod()) ? payload.getMethod().toUpperCase() : "GET";
      if ("POST".equals(method)) {
        RequestBody requestBody =
            RequestBody.create(
                payload.getBody() == null ? "" : payload.getBody(),
                okhttp3.MediaType.parse(MediaType.APPLICATION_JSON_VALUE));
        requestBuilder.post(requestBody);
      } else {
        requestBuilder.get();
      }
      log.debug(
          "Executing template API task {} with method {} at {}",
          task.getId(),
          method,
          sanitizedRequestUrl);
      try (Response response = httpClientFactory.executeRequest(requestBuilder.build())) {
        okhttp3.ResponseBody responseBody = response.body();
        okhttp3.MediaType contentType = responseBody != null ? responseBody.contentType() : null;
        String resolvedMimeType = resolveApiResponseMimeType(task, contentType);
        if (response.isSuccessful() && isBinaryMimeType(resolvedMimeType)) {
          log.debug(
              "Template API task {} returned HTTP {} binary content-type {} length {}",
              task.getId(),
              response.code(),
              resolvedMimeType,
              responseBody != null ? responseBody.contentLength() : 0);
          return buildBinaryApiResponse(
              task,
              parameters,
              payload.getParameters(),
              hasServerSideHttpSecurity(payload.getSecurity()) ? null : requestUrl,
              resolvedMimeType);
        }
        String body = responseBody != null ? responseBody.string() : "";
        log.debug(
            "Template API task {} returned HTTP {} content-type {} body length {}",
            task.getId(),
            response.code(),
            contentType,
            body.length());
        if (!response.isSuccessful()) {
          log.warn(
              "Template API task {} failed with HTTP {} body length {}",
              task.getId(),
              response.code(),
              body.length());
          throw new ResponseStatusException(
              HttpStatus.BAD_GATEWAY, "API task returned HTTP " + response.code());
        }
        Map<String, Object> bodyContext = normalizeBodyToContext(body);
        List<Map<String, Object>> rows = flattenContextToRows(bodyContext);
        Map<String, Object> context =
            buildApiContext(bodyContext, rows, payload.getParameters(), parameters);
        return ChildDataResult.builder()
            .outcome(ChildDataOutcome.OK)
            .resultType(TABLE)
            .context(context)
            .rows(rows)
            .resourceUrl(null)
            .build();
      }
    } catch (IOException e) {
      log.warn(
          "Template API task {} failed while calling {}", task.getId(), sanitizedRequestUrl, e);
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to execute API task", e);
    }
  }

  private String sanitizeRequestUrlForLogging(
      String requestUrl, Map<String, String> executionParameters) {
    if (!StringUtils.hasText(requestUrl)) {
      return "";
    }
    return maskQueryParameterValues(maskExecutionParameterValues(requestUrl, executionParameters));
  }

  private String maskExecutionParameterValues(
      String requestUrl, Map<String, String> executionParameters) {
    if (executionParameters == null || executionParameters.isEmpty()) {
      return requestUrl;
    }
    String sanitized = requestUrl;
    List<Map.Entry<String, String>> entries = new ArrayList<>(executionParameters.entrySet());
    entries.sort(Comparator.comparingInt(entry -> -String.valueOf(entry.getValue()).length()));
    for (Map.Entry<String, String> entry : entries) {
      if (!StringUtils.hasText(entry.getKey()) || !StringUtils.hasText(entry.getValue())) {
        continue;
      }
      String placeholder = "{" + entry.getKey() + "}";
      String encodedValue = URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8);
      sanitized = sanitized.replace(encodedValue.replace("+", "%20"), placeholder);
      sanitized = sanitized.replace(encodedValue, placeholder);
      sanitized = sanitized.replace(entry.getValue(), placeholder);
    }
    return sanitized;
  }

  private String maskQueryParameterValues(String requestUrl) {
    int fragmentStart = requestUrl.indexOf('#');
    String urlWithoutFragment =
        fragmentStart >= 0 ? requestUrl.substring(0, fragmentStart) : requestUrl;
    String fragment = fragmentStart >= 0 ? requestUrl.substring(fragmentStart) : "";
    int queryStart = urlWithoutFragment.indexOf('?');
    if (queryStart < 0) {
      return requestUrl;
    }
    String prefix = urlWithoutFragment.substring(0, queryStart + 1);
    String query = urlWithoutFragment.substring(queryStart + 1);
    if (query.isEmpty()) {
      return requestUrl;
    }
    List<String> maskedParameters = new ArrayList<>();
    for (String queryParameter : query.split("&", -1)) {
      int valueStart = queryParameter.indexOf('=');
      if (valueStart < 0) {
        maskedParameters.add(queryParameter);
      } else {
        maskedParameters.add(queryParameter.substring(0, valueStart + 1) + "***");
      }
    }
    return prefix + String.join("&", maskedParameters) + fragment;
  }

  public ChildDataResult resolveDirect(ChildDataRequest request) {
    Task task = request.getTask();
    Map<String, String> parameters =
        request.getParameters() == null ? new LinkedHashMap<>() : request.getParameters();
    String scope = request.getScope();
    RequestCoordinates coordinates = request.getCoordinates();
    String command =
        String.valueOf(
            task.getProperties().getOrDefault(DomainConstants.Tasks.PROPERTY_COMMAND, ""));
    String resolved = resolveTemplateUrl(command, parameters, coordinates);
    Map<String, Object> context = new LinkedHashMap<>();
    context.put("url", resolved);
    parameters.forEach((key, value) -> context.put("$" + key, value));
    String resultType =
        DomainConstants.Tasks.SCOPE_RESOURCE_QUERY.equalsIgnoreCase(scope)
                || DomainConstants.Tasks.SCOPE_RESOURCE.equalsIgnoreCase(scope)
            ? "resource"
            : "url";
    return ChildDataResult.builder()
        .outcome(ChildDataOutcome.OK)
        .resultType(resultType)
        .context(context)
        .rows(Collections.emptyList())
        .resourceUrl(resolved)
        .build();
  }

  private Map<String, Object> buildRowAndParameterContext(
      List<Map<String, Object>> rows, Map<String, String> parameters) {
    Map<String, Object> context = new LinkedHashMap<>();
    parameters.forEach((key, value) -> context.put("$" + key, value));
    context.put("rows", rows);
    if (!rows.isEmpty()) {
      rows.get(0).forEach(context::put);
    }
    return context;
  }

  private Map<String, Object> buildApiContext(
      Map<String, Object> bodyContext,
      List<Map<String, Object>> rows,
      Map<String, String> configuredParameters,
      Map<String, String> executionParameters) {
    Map<String, Object> context = new LinkedHashMap<>();
    mergeTemplateParameterContext(context, configuredParameters, executionParameters);
    context.put("rows", rows);
    if (bodyContext != null) {
      context.putAll(bodyContext);
    }
    return context;
  }

  private ChildDataResult buildBinaryApiResponse(
      Task task,
      Map<String, String> executionParameters,
      Map<String, String> configuredParameters,
      String contentUrl,
      String mimeType) {
    Map<String, Object> context = new LinkedHashMap<>();
    mergeTemplateParameterContext(context, configuredParameters, executionParameters);
    context.put("contentUrl", contentUrl);
    context.put("url", contentUrl);
    context.put("mimeType", mimeType);
    context.put("binary", true);
    context.put("embeddable", StringUtils.hasText(contentUrl));
    String language = currentRequestLanguageResolver.resolve(this);
    if (!StringUtils.hasText(contentUrl)) {
      context.put("accessMessage", resolveLiteral(BINARY_ACCESS_MESSAGE_LITERAL, language));
    }
    context.put(
        DomainConstants.Tasks.PARAMETERS_VALUE,
        resolveLiteral(BINARY_VALUE_PLACEHOLDER_LITERAL, language));
    List<Map<String, Object>> rows = flattenContextToRows(context);
    return ChildDataResult.builder()
        .outcome(ChildDataOutcome.OK)
        .resultType("resource")
        .context(context)
        .rows(rows)
        .resourceUrl(contentUrl)
        .build();
  }

  private String resolveApiResponseMimeType(Task task, okhttp3.MediaType contentType) {
    String configuredMimeType = readConfiguredMimeType(task);
    if (StringUtils.hasText(configuredMimeType)) {
      return configuredMimeType.trim();
    }
    return contentType != null ? contentType.toString() : null;
  }

  private String readConfiguredMimeType(Task task) {
    Map<String, Object> properties = task.getProperties();
    Object mimeType =
        properties != null ? properties.get(DomainConstants.Tasks.PROPERTY_MIME_TYPE) : null;
    return mimeType != null ? String.valueOf(mimeType) : null;
  }

  private boolean isBinaryMimeType(String mimeType) {
    if (!StringUtils.hasText(mimeType)) {
      return true;
    }
    String normalized = mimeType.toLowerCase().split(";", 2)[0].trim();
    if (normalized.startsWith("text/")
        || normalized.equals(MediaType.APPLICATION_JSON_VALUE)
        || normalized.equals(MediaType.APPLICATION_XML_VALUE)
        || normalized.equals(MediaType.TEXT_XML_VALUE)
        || normalized.equals("application/xhtml+xml")
        || normalized.equals("application/javascript")
        || normalized.equals("application/x-javascript")
        || normalized.equals("application/ecmascript")
        || normalized.equals("application/x-www-form-urlencoded")
        || normalized.equals("application/csv")
        || normalized.endsWith("+json")
        || normalized.endsWith("+xml")) {
      return false;
    }
    return true;
  }

  private boolean hasServerSideHttpSecurity(HttpSecurityDto security) {
    if (security == null) {
      return false;
    }
    return (security.getHeaders() != null && !security.getHeaders().isEmpty())
        || StringUtils.hasText(security.getUsername())
        || StringUtils.hasText(security.getPassword());
  }

  private void mergeTemplateParameterContext(
      Map<String, Object> context,
      Map<String, String> configuredParameters,
      Map<String, String> executionParameters) {
    if (configuredParameters != null) {
      configuredParameters.forEach((key, value) -> context.put("$" + key, value));
    }
    if (executionParameters != null) {
      executionParameters.forEach((key, value) -> context.put("$" + key, value));
    }
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
    return Collections.singletonMap(DomainConstants.Tasks.PARAMETERS_VALUE, body);
  }

  private List<Map<String, Object>> flattenContextToRows(Map<String, Object> context) {
    List<Map<String, Object>> rows = new ArrayList<>();
    flattenValue(null, context, rows);
    return rows;
  }

  private void flattenValue(String path, Object value, List<Map<String, Object>> rows) {
    if (value instanceof Map<?, ?> mapValue) {
      mapValue.forEach(
          (key, nestedValue) ->
              flattenValue(appendPath(path, String.valueOf(key)), nestedValue, rows));
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
      row.put(DomainConstants.Tasks.PARAMETERS_VALUE, value);
      rows.add(row);
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
    if (StringUtils.hasText(security.getUsername())
        && StringUtils.hasText(security.getPassword())) {
      requestBuilder.addHeader(
          "Authorization",
          Credentials.basic(
              security.getUsername(), security.getPassword(), StandardCharsets.UTF_8));
    }
  }

  private String buildHttpRequestUrl(
      WmsPayloadDto payload,
      Map<String, String> executionParameters,
      RequestCoordinates coordinates,
      String taskCommand) {
    Map<String, String> templateParameters = new LinkedHashMap<>();
    if (payload.getParameters() != null) {
      templateParameters.putAll(payload.getParameters());
    }
    if (executionParameters != null) {
      templateParameters.putAll(executionParameters);
    }
    String uriTemplateSource = selectUriTemplateSource(payload.getUri(), taskCommand);
    String resolvedUri = resolveTemplateUrl(uriTemplateSource, templateParameters, coordinates);
    Set<String> uriTemplateParameters = extractUriTemplateParameters(uriTemplateSource);
    Map<String, String> queryParameters = new LinkedHashMap<>();
    if (payload.getParameters() != null) {
      queryParameters.putAll(payload.getParameters());
    }
    if (executionParameters != null) {
      executionParameters.forEach(queryParameters::putIfAbsent);
    }
    uriTemplateParameters.forEach(queryParameters::remove);
    return appendEncodedQueryParameters(resolvedUri, queryParameters);
  }

  private String appendEncodedQueryParameters(
      String resolvedUri, Map<String, String> queryParameters) {
    if (queryParameters.isEmpty()) {
      return resolvedUri;
    }
    StringBuilder url = new StringBuilder(resolvedUri);
    url.append(resolvedUri.contains("?") ? "&" : "?");
    List<String> encodedParameters = new ArrayList<>();
    queryParameters.forEach(
        (key, value) ->
            encodedParameters.add(encodeQueryComponent(key) + "=" + encodeQueryComponent(value)));
    url.append(String.join("&", encodedParameters));
    return url.toString();
  }

  private String encodeQueryComponent(String value) {
    return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8)
        .replace("+", "%20");
  }

  private String selectUriTemplateSource(String payloadUri, String taskCommand) {
    if (extractUriTemplateParameters(payloadUri).isEmpty()
        && !extractUriTemplateParameters(taskCommand).isEmpty()) {
      return taskCommand;
    }
    return payloadUri;
  }

  private String readTaskCommand(Task task) {
    Map<String, Object> properties = task.getProperties();
    Object command =
        properties != null ? properties.get(DomainConstants.Tasks.PROPERTY_COMMAND) : null;
    return command != null ? String.valueOf(command) : null;
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

  private String resolveTemplateUrl(
      String command, Map<String, String> parameters, RequestCoordinates coordinates) {
    String resolved = systemVariableResolver.resolve(command, coordinates);
    if (resolved == null) {
      resolved = command;
    }
    for (Map.Entry<String, String> entry : parameters.entrySet()) {
      String encodedValue = URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8);
      resolved = resolved.replace("{" + entry.getKey() + "}", encodedValue);
      resolved = resolved.replace("${" + entry.getKey() + "}", encodedValue);
    }
    return resolved;
  }

  private String resolveLiteral(String key, String language) {
    String resolved = literalTranslationResolver.resolve(key, language);
    return StringUtils.hasText(resolved) ? resolved : key;
  }
}
