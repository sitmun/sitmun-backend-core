package org.sitmun.authorization.proxy.service;

import static org.sitmun.authorization.proxy.decorators.QueryPaginationDecorator.SQL_LIMIT;
import static org.sitmun.authorization.proxy.decorators.QueryPaginationDecorator.SQL_OFFSET;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.sitmun.authorization.proxy.decorators.QueryFixedFiltersDecorator;
import org.sitmun.authorization.proxy.decorators.QueryPaginationDecorator;
import org.sitmun.authorization.proxy.decorators.QueryVaryFiltersDecorator;
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
import org.sitmun.domain.territory.Territory;
import org.sitmun.domain.territory.TerritoryRepository;
import org.sitmun.domain.user.UserRepository;
import org.sitmun.infrastructure.util.TaskParameterUtil;
import org.sitmun.infrastructure.variables.SystemVariableResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;

@Slf4j
@org.springframework.stereotype.Service
public class ProxyConfigurationService {

  private final ServiceRepository serviceRepository;

  private final TaskRepository taskRepository;

  private final UserRepository userRepository;

  private final TerritoryRepository territoryRepository;

  private final ApplicationRepository applicationRepository;

  private final QueryFixedFiltersDecorator queryFixedFiltersDecorator;

  private final QueryVaryFiltersDecorator queryVaryFiltersDecorator;

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
      QueryFixedFiltersDecorator queryFixedFiltersDecorator,
      QueryVaryFiltersDecorator queryVaryFiltersDecorator,
      QueryPaginationDecorator queryPaginationDecorator,
      List<ResourceAccessValidator> accessValidators,
      SystemVariableResolver systemVariableResolver) {
    this.serviceRepository = serviceRepository;
    this.taskRepository = taskRepository;
    this.userRepository = userRepository;
    this.territoryRepository = territoryRepository;
    this.applicationRepository = applicationRepository;
    this.queryFixedFiltersDecorator = queryFixedFiltersDecorator;
    this.queryVaryFiltersDecorator = queryVaryFiltersDecorator;
    this.queryPaginationDecorator = queryPaginationDecorator;
    this.accessValidators = accessValidators;
    this.systemVariableResolver = systemVariableResolver;
  }

  private static WmsPayloadDto getOgcWmsConfiguration(
      Service service, ConfigProxyRequestDto configProxyRequestDto) {

    if (service == null) {
      return null;
    }

    HttpSecurityDto security = null;
    if (Boolean.TRUE.equals(service.getPasswordSet())) {
      security =
          HttpSecurityDto.builder()
              .type("http")
              .scheme("basic")
              .username(service.getUser())
              .password(service.getPassword())
              .build();
    }

    Map<String, String> parameters = configProxyRequestDto.getParameters();
    if (parameters == null) {
      parameters = new HashMap<>();
    }
    Set<ServiceParameter> servParams = service.getParameters();
    log.info("Parametros servicio {}", servParams.size());
    List<String> varyParameters = new ArrayList<>();
    for (ServiceParameter parameter : servParams) {
      if (DomainConstants.Proxy.PARAM_TYPE_VARY.equalsIgnoreCase(parameter.getType())) {
        varyParameters.add(parameter.getName());
      } else {
        parameters.put(parameter.getName(), parameter.getValue());
      }
    }
    return WmsPayloadDto.builder()
        .uri(service.getServiceURL())
        .method(configProxyRequestDto.getMethod())
        .vary(varyParameters)
        .parameters(parameters)
        .body(configProxyRequestDto.getRequestBody())
        .security(security)
        .build();
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

  private JdbcPayloadDto getDatasourceConfiguration(
      Task task,
      org.sitmun.domain.user.User user,
      Territory territory,
      org.sitmun.domain.application.Application application) {
    DatabaseConnection databaseConnection = task.getConnection();
    String sql = getSqlByTask(task);

    // Resolve system variables (#{}) before sending to proxy
    sql = systemVariableResolver.resolve(sql, user, territory, application);

    return databaseConnection != null
        ? JdbcPayloadDto.builder()
            .uri(databaseConnection.getUrl())
            .user(databaseConnection.getUser())
            .password(databaseConnection.getPassword())
            .driver(databaseConnection.getDriver())
            .sql(sql)
            .build()
        : null;
  }

  private WmsPayloadDto getHttpApiConfiguration(
      Task task,
      org.sitmun.domain.user.User user,
      Territory territory,
      org.sitmun.domain.application.Application application) {
    final Map<String, Object> taskProps = task.getProperties();

    //  Check for null properties
    if (taskProps == null) {
      return null;
    }

    String url = (String) taskProps.get(DomainConstants.Tasks.PROPERTY_COMMAND);

    // Check for null or blank URL
    if (url == null || !StringUtils.hasText(url)) {
      return null;
    }

    // Resolve system variables (#{}) in URL before sending to proxy
    url = systemVariableResolver.resolve(url, user, territory, application);

    // Handle non-String parameter values - key by variable name (with backward compatibility)
    @SuppressWarnings("unchecked")
    final Map<String, String> parameters =
        ((List<Map<String, Object>>)
                taskProps.getOrDefault(
                    DomainConstants.Tasks.PROPERTY_PARAMETERS, Collections.emptyList()))
            .stream()
                .map(
                    p -> {
                      // Use backward-compatible variable reading (tries 'variable' then 'name')
                      String key = TaskParameterUtil.getParameterVariable(p);
                      // Fallback to label if neither variable nor name exists (legacy support)
                      if (key == null) {
                        key = String.valueOf(p.get(DomainConstants.Tasks.PARAMETERS_LABEL));
                      }
                      String value = String.valueOf(p.get(DomainConstants.Tasks.PARAMETERS_VALUE));
                      return Map.entry(key, value);
                    })
                .filter(
                    e ->
                        e.getKey() != null
                            && !"null".equals(e.getKey())) // Skip entries without a valid key
                .collect(
                    Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> b)); // last-wins for duplicates

    final String body = (String) taskProps.getOrDefault(DomainConstants.Tasks.PROPERTY_BODY, null);

    String authenticationMode =
        (String) taskProps.getOrDefault(DomainConstants.Tasks.PROPERTY_AUTHENTICATION_MODE, null);
    String apiUser = (String) taskProps.getOrDefault(DomainConstants.Tasks.PROPERTY_USER, null);
    String apiPassword =
        (String) taskProps.getOrDefault(DomainConstants.Tasks.PROPERTY_PASSWORD, null);

    // Only build security DTO if authentication is configured
    HttpSecurityDto security = null;
    if (authenticationMode != null || apiUser != null || apiPassword != null) {
      HttpSecurityDto.HttpSecurityDtoBuilder securityBuilder =
          HttpSecurityDto.builder()
              .type(authenticationMode)
              .scheme("Basic")
              .username(apiUser)
              .password(apiPassword);

      Object headersObject = taskProps.get(DomainConstants.Tasks.PROPERTY_HEADERS);
      if (headersObject instanceof Map<?, ?> headers) {
        securityBuilder.headers(
            headers.entrySet().stream()
                .filter(e -> e.getKey() instanceof String && e.getValue() instanceof String)
                .collect(Collectors.toMap(e -> (String) e.getKey(), e -> (String) e.getValue())));
      }

      security = securityBuilder.build();
    }

    // API method is hardcoded to GET as per current proxy middleware implementation.
    // The actual HTTP method (GET/POST) is determined by the client's original request in the
    // middleware.
    // This value is used for HTTP-based payloads but may need to be parameterized in future
    // versions
    // if backend-level method switching is required.
    return WmsPayloadDto.builder()
        .uri(url)
        .method("GET")
        .parameters(parameters)
        .body(body)
        .security(security)
        .build();
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

  public ConfigProxyDto getConfiguration(
      ConfigProxyRequestDto configProxyRequestDto, long expirationTimeToken, String username) {
    log.info(
        "Fetching configuration for service type {} with id {}",
        configProxyRequestDto.getType(),
        configProxyRequestDto.getTypeId());

    // Fetch context entities for system variable resolution
    final org.sitmun.domain.user.User user = userRepository.findByUsername(username).orElse(null);
    final Territory territory =
        territoryRepository.findById(configProxyRequestDto.getTerId()).orElse(null);
    final org.sitmun.domain.application.Application application =
        applicationRepository.findById(configProxyRequestDto.getAppId()).orElse(null);

    AtomicReference<PayloadDto> payload = new AtomicReference<>(null);
    AtomicReference<String> configType = new AtomicReference<>("");
    if (DomainConstants.Proxy.TYPE_SQL.equalsIgnoreCase(configProxyRequestDto.getType())) {
      taskRepository
          .findById(configProxyRequestDto.getTypeId())
          .ifPresent(
              task -> {
                payload.set(getDatasourceConfiguration(task, user, territory, application));
                configType.set(DomainConstants.Proxy.TYPE_SQL);
              });
    } else if (DomainConstants.Proxy.TYPE_API.equalsIgnoreCase(configProxyRequestDto.getType())) {
      taskRepository
          .findById(configProxyRequestDto.getTypeId())
          .ifPresent(
              task -> {
                payload.set(getHttpApiConfiguration(task, user, territory, application));
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
                payload.set(getOgcWmsConfiguration(service, configProxyRequestDto));
                configType.set(service.getType());
              });
    }
    if (payload.get() != null) {
      long expirationTime =
          expirationTimeToken > 0
              ? expirationTimeToken / 1000
              : (new Date().getTime() / 1000) + responseValidityTime;
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
      ConfigProxyDto configProxyDto, ConfigProxyRequestDto configProxyRequestDto, String username) {
    PayloadDto payload = configProxyDto.getPayload();

    // System variables (#{}) are now resolved in getDatasourceConfiguration/getHttpApiConfiguration
    // No need for addFixedFilters() anymore

    Map<String, String> parameters = configProxyRequestDto.getParameters();

    if (parameters != null && !parameters.isEmpty()) {
      String limit = null;
      if (parameters.containsKey(SQL_LIMIT)) {
        limit = parameters.get(SQL_LIMIT);
        parameters.remove(SQL_LIMIT);
      }
      String offset = null;
      if (parameters.containsKey(SQL_OFFSET)) {
        offset = parameters.get(SQL_OFFSET);
        parameters.remove(SQL_OFFSET);
      }
      addVaryFilters(parameters, payload);
      addPagination(limit, offset, payload);
    }
  }

  private void addVaryFilters(Map<String, String> parameters, PayloadDto payload) {
    queryVaryFiltersDecorator.apply(parameters, payload);
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
}
