package org.sitmun.authorization.proxy.service;

import static org.sitmun.authorization.proxy.decorators.QueryPaginationDecorator.SQL_LIMIT;
import static org.sitmun.authorization.proxy.decorators.QueryPaginationDecorator.SQL_OFFSET;
import static org.sitmun.domain.DomainConstants.Proxy.*;
import static org.sitmun.domain.DomainConstants.Tasks.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import org.sitmun.authorization.proxy.decorators.HttpUserParametrizationDecorator;
import org.sitmun.authorization.proxy.decorators.QueryPaginationDecorator;
import org.sitmun.authorization.proxy.decorators.SqlUserParametrizationDecorator;
import org.sitmun.authorization.proxy.dto.ConfigProxyDto;
import org.sitmun.authorization.proxy.dto.ConfigProxyRequestDto;
import org.sitmun.authorization.proxy.dto.HttpSecurityDto;
import org.sitmun.authorization.proxy.dto.PayloadDto;
import org.sitmun.authorization.proxy.exception.BadRequestException;
import org.sitmun.authorization.proxy.protocols.jdbc.JdbcPayloadDto;
import org.sitmun.authorization.proxy.protocols.wms.WmsPayloadDto;
import org.sitmun.authorization.proxy.validator.ResourceAccessValidator;
import org.sitmun.domain.application.ApplicationRepository;
import org.sitmun.domain.database.DatabaseConnection;
import org.sitmun.domain.service.Service;
import org.sitmun.domain.service.ServiceRepository;
import org.sitmun.domain.service.parameter.ServiceParameter;
import org.sitmun.domain.task.MoreInfoTaskResolver;
import org.sitmun.domain.task.Task;
import org.sitmun.domain.task.TaskRepository;
import org.sitmun.domain.task.parameter.TaskParameter;
import org.sitmun.domain.task.parameter.TaskParameterProcessor;
import org.sitmun.domain.territory.TerritoryRepository;
import org.sitmun.domain.user.UserRepository;
import org.sitmun.infrastructure.variables.SystemVariableResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;

@Slf4j
@org.springframework.stereotype.Service
public class ProxyConfigurationService {

  /** OpenAPI 3.x {@code SecurityScheme.type} for username/password HTTP authentication. */
  private static final String SECURITY_SCHEME_TYPE_HTTP = "http";

  /** OpenAPI 3.x {@code SecurityScheme.type} for API key authentication. */
  private static final String SECURITY_SCHEME_TYPE_API_KEY = "apiKey";

  /**
   * OpenAPI 3.x {@code SecurityScheme.scheme} for HTTP Basic (IANA, lowercase per spec examples).
   */
  private static final String OPENAPI_SECURITY_SCHEME_HTTP_BASIC = "basic";

  private final ServiceRepository serviceRepository;

  private final TaskRepository taskRepository;

  private final UserRepository userRepository;

  private final TerritoryRepository territoryRepository;

  private final ApplicationRepository applicationRepository;

  private final SqlUserParametrizationDecorator sqlUserParametrizationDecorator;

  private final HttpUserParametrizationDecorator httpUserParametrizationDecorator;

  private final QueryPaginationDecorator queryPaginationDecorator;

  private final List<ResourceAccessValidator> accessValidators;

  private final SystemVariableResolver systemVariableResolver;

  private final MoreInfoTaskResolver moreInfoTaskResolver;

  private final TaskParameterProcessor taskParameterProcessor;

  @Value("${sitmun.proxy-middleware.config-response-validity-in-seconds:3600}")
  private int responseValidityTime;

  @Value("${sitmun.proxy-middleware.validate-user-access:true}")
  private boolean validateUserAccessEnabled;

  public ProxyConfigurationService(
      ServiceRepository serviceRepository,
      TaskRepository taskRepository,
      UserRepository userRepository,
      TerritoryRepository territoryRepository,
      ApplicationRepository applicationRepository,
      SqlUserParametrizationDecorator sqlUserParametrizationDecorator,
      HttpUserParametrizationDecorator httpUserParametrizationDecorator,
      QueryPaginationDecorator queryPaginationDecorator,
      List<ResourceAccessValidator> accessValidators,
      SystemVariableResolver systemVariableResolver,
      MoreInfoTaskResolver moreInfoTaskResolver,
      TaskParameterProcessor taskParameterProcessor) {
    this.serviceRepository = serviceRepository;
    this.taskRepository = taskRepository;
    this.userRepository = userRepository;
    this.territoryRepository = territoryRepository;
    this.applicationRepository = applicationRepository;
    this.sqlUserParametrizationDecorator = sqlUserParametrizationDecorator;
    this.httpUserParametrizationDecorator = httpUserParametrizationDecorator;
    this.queryPaginationDecorator = queryPaginationDecorator;
    this.accessValidators = accessValidators;
    this.systemVariableResolver = systemVariableResolver;
    this.moreInfoTaskResolver = moreInfoTaskResolver;
    this.taskParameterProcessor = taskParameterProcessor;
  }

  private WmsPayloadDto getOgcWmsConfiguration(
      Service service,
      ConfigProxyRequestDto configProxyRequestDto,
      RequestCoordinates coordinates) {

    if (service == null) {
      return null;
    }

    HttpSecurityDto security = null;
    if (Boolean.TRUE.equals(service.getPasswordSet())) {
      security =
          HttpSecurityDto.builder()
              .type(SECURITY_SCHEME_TYPE_HTTP)
              .scheme(OPENAPI_SECURITY_SCHEME_HTTP_BASIC)
              .username(service.getUser())
              .password(service.getPassword())
              .build();
    }

    Map<String, String> parameters = configProxyRequestDto.getParameters();
    if (parameters == null) {
      parameters = new HashMap<>();
    }
    Set<ServiceParameter> servParams = service.getParameters();
    List<String> varyParameters = new ArrayList<>();
    for (ServiceParameter parameter : servParams) {
      if (PARAM_TYPE_VARY.equalsIgnoreCase(parameter.getType())) {
        varyParameters.add(parameter.getName());
      } else {
        final String resolvedValue =
            systemVariableResolver.resolve(parameter.getValue(), coordinates);
        parameters.put(parameter.getName(), resolvedValue);
      }
    }

    final String resolvedUrl = systemVariableResolver.resolve(service.getServiceURL(), coordinates);
    WmsPayloadDto ogcPayload =
        WmsPayloadDto.builder()
            .uri(resolvedUrl)
            .method(configProxyRequestDto.getMethod())
            .vary(varyParameters)
            .parameters(parameters)
            .body(configProxyRequestDto.getRequestBody())
            .security(security)
            .build();
    log.debug(
        "OGC/WMS proxy payload: uri={} method={} security={}",
        resolvedUrl,
        configProxyRequestDto.getMethod(),
        security == null ? "none" : security.describeForLog());
    return ogcPayload;
  }

  private static String getSqlByTask(Task task) {
    String sql = "";
    Map<String, Object> taskParams = task.getProperties();
    if (taskParams != null && taskParams.containsKey(PROPERTY_COMMAND)) {
      sql = (String) taskParams.get(PROPERTY_COMMAND);
    }

    if (!StringUtils.hasText(sql)) {
      throw new BadRequestException("Bad request");
    }
    return sql;
  }

  private JdbcPayloadDto getDatasourceConfiguration(Task task, RequestCoordinates coordinates) {
    DatabaseConnection databaseConnection = task.getConnection();
    String sql = getSqlByTask(task);

    // Resolve system variables (#{}) before sending to proxy
    sql = systemVariableResolver.resolve(sql, coordinates);

    if (databaseConnection == null) {
      return null;
    }
    log.debug("JDBC proxy payload built (connection URL, credentials, and SQL omitted from logs)");
    return JdbcPayloadDto.builder()
        .uri(databaseConnection.getUrl())
        .user(databaseConnection.getUser())
        .password(databaseConnection.getPassword())
        .driver(databaseConnection.getDriver())
        .sql(sql)
        .build();
  }

  private WmsPayloadDto getHttpApiConfiguration(Task task, RequestCoordinates coordinates) {
    final Map<String, Object> taskProps = task.getProperties();

    //  Check for null properties
    if (taskProps == null) {
      return null;
    }

    String url = (String) taskProps.get(PROPERTY_COMMAND);

    // Check for null or blank URL
    if (!StringUtils.hasText(url)) {
      return null;
    }

    // Resolve system variables (#{}) in URL before sending to proxy
    url = systemVariableResolver.resolve(url, coordinates);

    // HTTP payload parameters contain only strict backend defaults (locked + provided)
    // so logging at construction time shows only what the backend controls.
    // Full effectiveParameters (including client values) are overlaid later in applyDecorators.
    List<TaskParameter> parameters = taskParameterProcessor.parse(task);
    Map<String, String> backendOnlyParameters =
        taskParameterProcessor.buildEffectiveParameters(parameters, null, coordinates);
    // Filter to only backend-only (LOCKED + PROVIDED) parameters
    Map<String, String> filteredBackendOnly = new LinkedHashMap<>();
    for (TaskParameter param : parameters) {
      if (taskParameterProcessor.classify(param).isBackendOnly()) {
        String value = backendOnlyParameters.get(param.name());
        if (value != null) {
          filteredBackendOnly.put(param.name(), value);
        }
      }
    }

    final String body = (String) taskProps.getOrDefault(PROPERTY_BODY, null);
    final HttpSecurityDto security = buildHttpSecurity(taskProps);

    // API method is hardcoded to GET as per current proxy middleware implementation.
    // The actual HTTP method (GET/POST) is determined by the client's original request in the
    // middleware.
    // This value is used for HTTP-based payloads but may need to be parameterized in future
    // versions
    // if backend-level method switching is required.
    WmsPayloadDto httpApiPayload =
        WmsPayloadDto.builder()
            .uri(url)
            .method("GET")
            .parameters(filteredBackendOnly)
            .body(body)
            .security(security)
            .build();
    log.debug(
        "HTTP API proxy payload: uri={} security={}",
        url,
        security == null ? "none" : security.describeForLog());
    return httpApiPayload;
  }

  public boolean validateUserAccess(ConfigProxyRequestDto configProxyRequestDto, String userName) {
    // Check if validation is enabled via configuration
    if (!validateUserAccessEnabled) {
      log.debug("User access validation is disabled via configuration");
      return true;
    }

    if (userName == null || userName.isBlank()) {
      log.warn("Username is null or blank, denying access");
      return false;
    }

    String resourceType = configProxyRequestDto.getType();
    log.debug(
        "Validating user access: user={}, type={}, typeId={}, appId={}, terId={}",
        userName,
        resourceType,
        configProxyRequestDto.getTypeId(),
        configProxyRequestDto.getAppId(),
        configProxyRequestDto.getTerId());

    // Find appropriate validator using strategy pattern
    return accessValidators.stream()
        .filter(validator -> validator.supports(resourceType))
        .findFirst()
        .map(validator -> validator.validate(configProxyRequestDto, userName))
        .orElseGet(
            () -> {
              // Deny by default for unknown resource types
              log.warn("No validator found for resource type: {}, denying access", resourceType);
              return false;
            });
  }

  public RequestCoordinates getRequestCoordinates(
      ConfigProxyRequestDto configProxyRequestDto, String username) {
    var response = new RequestCoordinates();
    // Fetch context entities for system variable resolution
    response.user = userRepository.findByUsername(username).orElse(null);
    response.territory =
        territoryRepository.findById(configProxyRequestDto.getTerId()).orElse(null);
    response.application =
        applicationRepository.findById(configProxyRequestDto.getAppId()).orElse(null);
    return response;
  }

  public ConfigProxyDto getConfiguration(
      ConfigProxyRequestDto configProxyRequestDto,
      long expirationTimeToken,
      RequestCoordinates coordinates) {
    log.info(
        "Fetching configuration for service type {} with id {}",
        configProxyRequestDto.getType(),
        configProxyRequestDto.getTypeId());

    Objects.requireNonNull(coordinates, "coordinates");

    AtomicReference<PayloadDto> payload = new AtomicReference<>(null);
    AtomicReference<String> configType = new AtomicReference<>("");
    if (TYPE_SQL.equalsIgnoreCase(configProxyRequestDto.getType())) {
      taskRepository
          .findById(configProxyRequestDto.getTypeId())
          .ifPresent(
              task -> {
                payload.set(
                    getDatasourceConfiguration(
                        moreInfoTaskResolver.resolveOrSelf(task), coordinates));
                configType.set(TYPE_SQL);
              });
    } else if (TYPE_API.equalsIgnoreCase(configProxyRequestDto.getType())) {
      taskRepository
          .findById(configProxyRequestDto.getTypeId())
          .ifPresent(
              task -> {
                payload.set(
                    getHttpApiConfiguration(moreInfoTaskResolver.resolveOrSelf(task), coordinates));
                configType.set(TYPE_API);
              });
    } else {
      log.info(
          "Searching service type {} with id {}",
          configProxyRequestDto.getType(),
          configProxyRequestDto.getTypeId());
      serviceRepository
          .findById(configProxyRequestDto.getTypeId())
          .ifPresent(
              service -> {
                payload.set(getOgcWmsConfiguration(service, configProxyRequestDto, coordinates));
                configType.set(service.getType());
              });
    }
    if (payload.get() != null) {
      long expirationTime =
          expirationTimeToken > 0
              ? expirationTimeToken / 1000
              : (new Date().getTime() / 1000) + responseValidityTime;
      log.debug(
          "Proxy configuration built: requestedType={} resultConfigType={} expSec={} summary={}",
          configProxyRequestDto.getType(),
          configType.get(),
          expirationTime,
          summarizePayloadForLog(payload.get()));
      return ConfigProxyDto.builder()
          .type(configType.get())
          .exp(expirationTime)
          .payload(payload.get())
          .build();
    }
    throw new BadRequestException(
        "Bad request for service type "
            + configProxyRequestDto.getType()
            + " with id "
            + configProxyRequestDto.getTypeId());
  }

  public void applyDecorators(
      ConfigProxyDto configProxyDto,
      ConfigProxyRequestDto configProxyRequestDto,
      RequestCoordinates coordinates) {
    Objects.requireNonNull(coordinates, "coordinates");

    PayloadDto payload = configProxyDto.getPayload();

    // Wrap client parameters in value object
    ClientRequestParameters clientParams =
        ClientRequestParameters.of(configProxyRequestDto.getParameters());

    log.debug(
        "applyDecorators: incomingRequestParameterCount={} payloadClass={}",
        clientParams.asMap().size(),
        payload.getClass().getSimpleName());

    // Security: reject any client-supplied parameter value containing #{...} patterns before
    // processing, to prevent injection of system variable expressions.
    clientParams.rejectSystemVariables(taskParameterProcessor);

    // Strip pagination parameters before filtering
    ClientRequestParameters.PaginationExtractionResult paginationResult =
        clientParams.takePagination();
    Pagination pagination = paginationResult.pagination();
    ClientRequestParameters paramsWithoutPagination = paginationResult.remainingParameters();

    // Update the original request to reflect stripped pagination parameters
    configProxyRequestDto.setParameters(new LinkedHashMap<>(paramsWithoutPagination.asMap()));

    // Parse task parameters and filter/build effective parameters
    List<TaskParameter> taskParameters = getTaskParametersForRequest(configProxyRequestDto);

    ClientRequestParameters filteredClientParameters;
    if (taskParameters.isEmpty()) {
      // For OGC/WMS/WMTS services (non-task requests), use client parameters directly
      // without filtering through task parameter declarations to support URI template expansion
      filteredClientParameters = paramsWithoutPagination;
    } else {
      // For task requests (SQL/API), filter client parameters against client-allowed names
      filteredClientParameters =
          paramsWithoutPagination.filterToAllowed(taskParameters, taskParameterProcessor);
    }

    // Build effectiveParameters with priority: locked > provided > client > literal > empty
    EffectiveParameters effectiveParameters =
        buildEffectiveParametersTyped(
            taskParameters, filteredClientParameters.asMap(), coordinates);

    // For OGC/WMS/WMTS services, use filtered client parameters directly for expansion
    // For task requests, use effective parameters
    Map<String, String> parametersForExpansion =
        taskParameters.isEmpty() ? filteredClientParameters.asMap() : effectiveParameters.asMap();

    if (!parametersForExpansion.isEmpty()) {
      expandUserParameters(parametersForExpansion, payload);
    }

    addPagination(pagination.limit(), pagination.offset(), payload);
  }

  /**
   * Builds effective parameters and wraps result in {@link EffectiveParameters}.
   *
   * @param taskParameters declared task parameters
   * @param filteredClientParameters client-supplied parameters after filtering
   * @param coordinates request coordinates
   * @return typed effective parameters
   */
  private EffectiveParameters buildEffectiveParametersTyped(
      List<TaskParameter> taskParameters,
      Map<String, String> filteredClientParameters,
      RequestCoordinates coordinates) {
    Map<String, String> effectiveMap =
        taskParameterProcessor.buildEffectiveParameters(
            taskParameters, filteredClientParameters, coordinates);
    // Wrap in LinkedHashMap to ensure stable iteration order
    return new EffectiveParameters(new LinkedHashMap<>(effectiveMap));
  }

  /**
   * Gets the parsed task parameters for the current request. Returns empty list for non-task
   * requests (OGC/WMS services).
   */
  private List<TaskParameter> getTaskParametersForRequest(
      ConfigProxyRequestDto configProxyRequestDto) {
    if (!TYPE_SQL.equalsIgnoreCase(configProxyRequestDto.getType())
        && !TYPE_API.equalsIgnoreCase(configProxyRequestDto.getType())) {
      return Collections.emptyList();
    }

    return taskRepository
        .findById(configProxyRequestDto.getTypeId())
        .map(moreInfoTaskResolver::resolveOrSelf)
        .map(taskParameterProcessor::parse)
        .orElse(Collections.emptyList());
  }

  /**
   * Reads LIMIT and OFFSET from the request map using case-insensitive key names, removes every
   * matching key, and returns {@code [limit, offset]}. When several keys match the same semantic
   * (e.g. {@code limit} and {@code LIMIT}), the last entry encountered in map iteration order wins.
   */
  private void expandUserParameters(Map<String, String> parameters, PayloadDto payload) {
    sqlUserParametrizationDecorator.apply(parameters, payload);
    httpUserParametrizationDecorator.apply(parameters, payload);
  }

  private void addPagination(String limit, String offset, PayloadDto payload) {
    Map<String, String> pagination = new HashMap<>();
    if (StringUtils.hasText(limit)) {
      pagination.put(SQL_LIMIT, limit);
    }
    if (StringUtils.hasText(offset)) {
      pagination.put(SQL_OFFSET, offset);
    }
    queryPaginationDecorator.apply(pagination, payload);
  }

  /**
   * Extracts string key-value pairs from a map object, filtering out non-string keys or values and
   * empty keys.
   *
   * @param mapObject the object to extract from (expected to be a Map)
   * @return a new map containing only valid string key-value pairs
   */
  private Map<String, String> extractStringMap(Object mapObject) {
    if (!(mapObject instanceof Map<?, ?> sourceMap)) {
      return new HashMap<>();
    }
    Map<String, String> result = new HashMap<>();
    for (var entry : sourceMap.entrySet()) {
      if (entry.getKey() instanceof String key
          && StringUtils.hasText(key)
          && entry.getValue() instanceof String value) {
        result.put(key, value);
      }
    }
    return result;
  }

  /**
   * Builds HTTP security DTO from task properties. Supports three authentication types with
   * precedence: HTTP Basic Auth > API Key Headers > API Key Query Params.
   *
   * @param taskProps the task properties map
   * @return HttpSecurityDto if authentication is configured, null otherwise
   */
  private HttpSecurityDto buildHttpSecurity(Map<String, Object> taskProps) {
    String authenticationMode = (String) taskProps.getOrDefault(PROPERTY_AUTHENTICATION_MODE, null);
    String apiUser = (String) taskProps.getOrDefault(PROPERTY_USER, null);
    String apiPassword = (String) taskProps.getOrDefault(PROPERTY_PASSWORD, null);
    Object headersObject = taskProps.get(PROPERTY_HEADERS);
    Object queryParamsObject = taskProps.get(PROPERTY_QUERY_PARAMS);

    boolean isApiKeyType =
        headersObject instanceof Map<?, ?> && !((Map<?, ?>) headersObject).isEmpty();
    boolean isQueryParamType =
        queryParamsObject instanceof Map<?, ?> && !((Map<?, ?>) queryParamsObject).isEmpty();
    boolean isHttpType =
        StringUtils.hasText(authenticationMode)
            && StringUtils.hasText(apiUser)
            && StringUtils.hasText(apiPassword);

    // HTTP basic authentication takes precedence if multiple security configurations are present
    if (isHttpType) {
      return HttpSecurityDto.builder()
          .type(SECURITY_SCHEME_TYPE_HTTP)
          .scheme(OPENAPI_SECURITY_SCHEME_HTTP_BASIC)
          .username(apiUser)
          .password(apiPassword)
          .build();
    } else if (isApiKeyType) {
      Map<String, String> securityHeaders = extractStringMap(headersObject);
      return HttpSecurityDto.builder()
          .type(SECURITY_SCHEME_TYPE_API_KEY)
          .headers(securityHeaders)
          .build();
    } else if (isQueryParamType) {
      Map<String, String> securityQueryParams = extractStringMap(queryParamsObject);
      return HttpSecurityDto.builder()
          .type(SECURITY_SCHEME_TYPE_API_KEY)
          .queryParams(securityQueryParams)
          .build();
    }

    return null;
  }

  private static String summarizePayloadForLog(PayloadDto payload) {
    if (payload instanceof WmsPayloadDto wms) {
      HttpSecurityDto sec = wms.getSecurity();
      return "uri="
          + wms.getUri()
          + ", method="
          + wms.getMethod()
          + ", "
          + (sec == null ? "security=null" : sec.describeForLog());
    }
    if (payload instanceof JdbcPayloadDto) {
      return "jdbc(payload credentials and SQL omitted)";
    }
    return payload.getClass().getSimpleName();
  }
}
