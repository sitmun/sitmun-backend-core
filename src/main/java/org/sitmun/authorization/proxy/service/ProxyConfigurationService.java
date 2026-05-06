package org.sitmun.authorization.proxy.service;

import static org.sitmun.authorization.proxy.decorators.QueryPaginationDecorator.SQL_LIMIT;
import static org.sitmun.authorization.proxy.decorators.QueryPaginationDecorator.SQL_OFFSET;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
import org.sitmun.domain.DomainConstants;
import org.sitmun.domain.application.ApplicationRepository;
import org.sitmun.domain.database.DatabaseConnection;
import org.sitmun.domain.service.Service;
import org.sitmun.domain.service.ServiceRepository;
import org.sitmun.domain.service.parameter.ServiceParameter;
import org.sitmun.domain.task.Task;
import org.sitmun.domain.task.TaskRepository;
import org.sitmun.domain.task.relation.TaskRelation;
import org.sitmun.domain.territory.TerritoryRepository;
import org.sitmun.domain.user.UserRepository;
import org.sitmun.infrastructure.util.TaskParameterUtil;
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
      SystemVariableResolver systemVariableResolver) {
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
      if (DomainConstants.Proxy.PARAM_TYPE_VARY.equalsIgnoreCase(parameter.getType())) {
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
    if (taskParams != null && taskParams.containsKey(DomainConstants.Tasks.PROPERTY_COMMAND)) {
      sql = (String) taskParams.get(DomainConstants.Tasks.PROPERTY_COMMAND);
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

    String url = (String) taskProps.get(DomainConstants.Tasks.PROPERTY_COMMAND);

    // Check for null or blank URL
    if (!StringUtils.hasText(url)) {
      return null;
    }

    // Resolve system variables (#{}) in URL before sending to proxy
    url = systemVariableResolver.resolve(url, coordinates);

    // Handle non-String parameter values - key by variable name (with backward compatibility)
    @SuppressWarnings("unchecked")
    final Map<String, String> parameters =
        ((List<Map<String, Object>>)
                taskProps.getOrDefault(
                    DomainConstants.Tasks.PROPERTY_PARAMETERS, Collections.emptyList()))
            .stream()
                .flatMap(
                    p -> {
                      // Use backward-compatible variable reading (tries 'variable' then 'name')
                      String key = TaskParameterUtil.getParameterVariable(p);
                      // Fallback to label if neither variable nor name exists (legacy support)
                      if (key == null) {
                        Object label = p.get(DomainConstants.Tasks.PARAMETERS_LABEL);
                        key = label != null ? String.valueOf(label) : null;
                      }
                      if (!StringUtils.hasText(key)) {
                        return Stream.empty();
                      }
                      Object rawValue = p.get(DomainConstants.Tasks.PARAMETERS_VALUE);
                      if (rawValue == null) {
                        return Stream.empty();
                      }
                      String value = String.valueOf(rawValue);
                      return Stream.of(Map.entry(key, value));
                    })
                .collect(
                    Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> b)); // last-wins for duplicates

    final String body = (String) taskProps.getOrDefault(DomainConstants.Tasks.PROPERTY_BODY, null);

    // Only build security DTO if authentication is configured
    HttpSecurityDto security = null;
    String authenticationMode =
        (String) taskProps.getOrDefault(DomainConstants.Tasks.PROPERTY_AUTHENTICATION_MODE, null);
    String apiUser = (String) taskProps.getOrDefault(DomainConstants.Tasks.PROPERTY_USER, null);
    String apiPassword =
        (String) taskProps.getOrDefault(DomainConstants.Tasks.PROPERTY_PASSWORD, null);
    Object headersObject = taskProps.get(DomainConstants.Tasks.PROPERTY_HEADERS);
    Object queryParamsObject = taskProps.get(DomainConstants.Tasks.PROPERTY_QUERY_PARAMS);

    var isApiKeyType = headersObject instanceof Map<?, ?> && !((Map<?, ?>) headersObject).isEmpty();
    var isQueryParamType = queryParamsObject instanceof Map<?, ?> && !((Map<?, ?>) queryParamsObject).isEmpty();
    var isHttpType =
        StringUtils.hasText(authenticationMode)
            && StringUtils.hasText(apiUser)
            && StringUtils.hasText(apiPassword);
    if (isHttpType) {
      security =
          HttpSecurityDto.builder()
              .type(SECURITY_SCHEME_TYPE_HTTP)
              .scheme(OPENAPI_SECURITY_SCHEME_HTTP_BASIC)
              .username(apiUser)
              .password(apiPassword)
              .build();
    } else if (isApiKeyType) {
      Map<?, ?> headers = (Map<?, ?>) headersObject;
      var securityHeaders = new HashMap<String, String>();
      for (var e : headers.entrySet()) {
        if (e.getKey() instanceof String key
            && StringUtils.hasText(key)
            && e.getValue() instanceof String value) {
          securityHeaders.put(key, value);
        }
      }
      security =
          HttpSecurityDto.builder()
              .type(SECURITY_SCHEME_TYPE_API_KEY)
              .headers(securityHeaders)
              .build();
    } else if (isQueryParamType) {
      Map<?, ?> qParams = (Map<?, ?>) queryParamsObject;
      var securityQueryParams = new HashMap<String, String>();
      for (var e : qParams.entrySet()) {
        if (e.getKey() instanceof String key
            && StringUtils.hasText(key)
            && e.getValue() instanceof String value) {
          securityQueryParams.put(key, value);
        }
      }
      security =
          HttpSecurityDto.builder()
              .type(SECURITY_SCHEME_TYPE_API_KEY)
              .queryParams(securityQueryParams)
              .build();
    }

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
            .parameters(parameters)
            .body(body)
            .security(security)
            .build();
    log.debug(
        "HTTP API proxy payload: uri={} security={}",
        url,
        security == null ? "none" : security.describeForLog());
    return httpApiPayload;
  }

  private Task resolveExecutionTask(Task task) {
    if (task == null
        || !DomainConstants.Tasks.isMoreInfoTask(task)
        || task.getRelations() == null) {
      return task;
    }
    return task.getRelations().stream()
        .filter(Objects::nonNull)
        .filter(
            relation ->
                DomainConstants.Tasks.RELATION_TYPE_QUERY_TASK.equalsIgnoreCase(
                    relation.getRelationType()))
        .map(TaskRelation::getRelatedTask)
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(task);
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
    if (DomainConstants.Proxy.TYPE_SQL.equalsIgnoreCase(configProxyRequestDto.getType())) {
      taskRepository
          .findById(configProxyRequestDto.getTypeId())
          .ifPresent(
              task -> {
                payload.set(getDatasourceConfiguration(resolveExecutionTask(task), coordinates));
                configType.set(DomainConstants.Proxy.TYPE_SQL);
              });
    } else if (DomainConstants.Proxy.TYPE_API.equalsIgnoreCase(configProxyRequestDto.getType())) {
      taskRepository
          .findById(configProxyRequestDto.getTypeId())
          .ifPresent(
              task -> {
                payload.set(getHttpApiConfiguration(resolveExecutionTask(task), coordinates));
                configType.set(DomainConstants.Proxy.TYPE_API);
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
    log.debug(
        "applyDecorators: incomingRequestParameterCount={} payloadClass={}",
        configProxyRequestDto.getParameters() == null
            ? 0
            : configProxyRequestDto.getParameters().size(),
        payload.getClass().getSimpleName());

    // System variables (#{}) are resolved in getDatasourceConfiguration, getHttpApiConfiguration,
    // and getOgcWmsConfiguration (via systemVariableResolver.resolve).
    // Coordinates carry user/territory/application for future decorator steps that need context.
    // No need for addFixedFilters() anymore

    Map<String, String> parameters =
        configProxyRequestDto.getParameters() == null
            ? null
            : new LinkedHashMap<>(configProxyRequestDto.getParameters());

    String limit = null;
    String offset = null;
    if (parameters != null && !parameters.isEmpty()) {
      String[] pagination = takePaginationValuesAndStripKeys(parameters);
      limit = pagination[0];
      offset = pagination[1];
    }

    parameters = filterIncomingTaskParameters(configProxyRequestDto, parameters);
    parameters = addSqlDefaultParametersForExplicitPlaceholders(configProxyRequestDto, payload, parameters);

    if (parameters != null && !parameters.isEmpty()) {
      expandUserParameters(parameters, payload);
    }

    addPagination(limit, offset, payload);
  }

  private Map<String, String> filterIncomingTaskParameters(
      ConfigProxyRequestDto configProxyRequestDto, Map<String, String> parameters) {
    if (parameters == null || parameters.isEmpty()) {
      return parameters;
    }

    if (!DomainConstants.Proxy.TYPE_SQL.equalsIgnoreCase(configProxyRequestDto.getType())
        && !DomainConstants.Proxy.TYPE_API.equalsIgnoreCase(configProxyRequestDto.getType())) {
      return parameters;
    }

    return taskRepository
        .findById(configProxyRequestDto.getTypeId())
        .map(this::resolveExecutionTask)
        .map(this::getDeclaredTaskParameterNames)
        .map(
            declaredNames ->
                parameters.entrySet().stream()
                    .filter(entry -> declaredNames.contains(entry.getKey()))
                    .collect(
                        Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue,
                            (left, right) -> right,
                            LinkedHashMap::new)))
        .orElseGet(LinkedHashMap::new);
  }

  private Set<String> getDeclaredTaskParameterNames(Task task) {
    Map<String, Object> properties = task != null ? task.getProperties() : null;
    if (properties == null) {
      return Collections.emptySet();
    }

    Object rawParameters = properties.get(DomainConstants.Tasks.PROPERTY_PARAMETERS);
    if (!(rawParameters instanceof List<?> parameterList)) {
      return Collections.emptySet();
    }

    return parameterList.stream()
        .filter(Map.class::isInstance)
        .map(
            param -> {
              Map<String, Object> parameter = (Map<String, Object>) param;
              String name = TaskParameterUtil.getParameterVariable(parameter);
              if (StringUtils.hasText(name)) {
                return name;
              }
              Object label = parameter.get(DomainConstants.Tasks.PARAMETERS_LABEL);
              return label != null ? String.valueOf(label) : null;
            })
        .filter(StringUtils::hasText)
        .collect(Collectors.toSet());
  }

  private Map<String, String> addSqlDefaultParametersForExplicitPlaceholders(
      ConfigProxyRequestDto configProxyRequestDto, PayloadDto payload, Map<String, String> parameters) {
    if (!(payload instanceof JdbcPayloadDto jdbcPayload)
        || !DomainConstants.Proxy.TYPE_SQL.equalsIgnoreCase(configProxyRequestDto.getType())) {
      return parameters;
    }

    Map<String, String> merged = parameters == null ? new LinkedHashMap<>() : new LinkedHashMap<>(parameters);

    taskRepository
        .findById(configProxyRequestDto.getTypeId())
        .map(this::resolveExecutionTask)
        .map(Task::getProperties)
        .map(properties -> properties.get(DomainConstants.Tasks.PROPERTY_PARAMETERS))
        .filter(List.class::isInstance)
        .map(List.class::cast)
        .ifPresent(
            parameterList -> {
              for (Object rawParam : parameterList) {
                if (!(rawParam instanceof Map<?, ?> param)) {
                  continue;
                }
                String name = TaskParameterUtil.getParameterVariable((Map<String, Object>) param);
                if (!StringUtils.hasText(name)) {
                  Object label = param.get(DomainConstants.Tasks.PARAMETERS_LABEL);
                  name = label != null ? String.valueOf(label) : null;
                }
                Object rawValue = param.get(DomainConstants.Tasks.PARAMETERS_VALUE);
                if (!StringUtils.hasText(name) || rawValue == null) {
                  continue;
                }
                if (merged.containsKey(name)) {
                  continue;
                }
                if (jdbcPayload.getSql() != null
                    && jdbcPayload.getSql().contains("${" + name + "}")) {
                  merged.put(name, String.valueOf(rawValue));
                }
              }
            });

    return merged;
  }

  /**
   * Reads LIMIT and OFFSET from the request map using case-insensitive key names, removes every
   * matching key, and returns {@code [limit, offset]}. When several keys match the same semantic
   * (e.g. {@code limit} and {@code LIMIT}), the last entry encountered in map iteration order wins.
   */
  private static String[] takePaginationValuesAndStripKeys(Map<String, String> parameters) {
    String limit = null;
    String offset = null;
    List<String> keysToRemove = new ArrayList<>();
    for (Map.Entry<String, String> e : parameters.entrySet()) {
      String key = e.getKey();
      if (!StringUtils.hasText(key)) {
        continue;
      }
      if (SQL_LIMIT.equalsIgnoreCase(key)) {
        limit = e.getValue();
        keysToRemove.add(key);
      } else if (SQL_OFFSET.equalsIgnoreCase(key)) {
        offset = e.getValue();
        keysToRemove.add(key);
      }
    }
    keysToRemove.forEach(parameters::remove);
    return new String[] {limit, offset};
  }

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
