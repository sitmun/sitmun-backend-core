package org.sitmun.authorization.service;

import lombok.extern.slf4j.Slf4j;
import org.sitmun.authorization.dto.*;
import org.sitmun.authorization.dto.decorators.QueryFixedFiltersDecorator;
import org.sitmun.authorization.dto.decorators.QueryPaginationDecorator;
import org.sitmun.authorization.dto.decorators.QueryVaryFiltersDecorator;
import org.sitmun.authorization.exception.BadRequestException;
import org.sitmun.domain.database.DatabaseConnection;
import org.sitmun.domain.service.Service;
import org.sitmun.domain.service.ServiceRepository;
import org.sitmun.domain.service.parameter.ServiceParameter;
import org.sitmun.domain.task.Task;
import org.sitmun.domain.task.TaskRepository;
import org.sitmun.domain.territory.Territory;
import org.sitmun.domain.territory.TerritoryRepository;
import org.sitmun.domain.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.sitmun.authorization.dto.decorators.QueryPaginationDecorator.SQL_LIMIT;
import static org.sitmun.authorization.dto.decorators.QueryPaginationDecorator.SQL_OFFSET;

@Slf4j
@org.springframework.stereotype.Service
public class ProxyConfigurationService {

  private static final String SQL_COMMAND_KEY = "command";

  private static final String CONNECTION_TYPE_KEY = "SQL";

  private static final String VARY_KEY = "VARY";

  private final ServiceRepository serviceRepository;

  private final TaskRepository taskRepository;

  private final UserRepository userRepository;

  private final TerritoryRepository territoryRepository;

  private final QueryFixedFiltersDecorator queryFixedFiltersDecorator;

  private final QueryVaryFiltersDecorator queryVaryFiltersDecorator;

  private final QueryPaginationDecorator queryPaginationDecorator;

  @Value("${sitmun.proxy.config-response-validity-in-seconds}")
  private int responseValidityTime;

  public ProxyConfigurationService(ServiceRepository serviceRepository,
                                   TaskRepository taskRepository, UserRepository userRepository,
                                   TerritoryRepository territoryRepository, QueryFixedFiltersDecorator queryFixedFiltersDecorator,
                                   QueryVaryFiltersDecorator queryVaryFiltersDecorator, QueryPaginationDecorator queryPaginationDecorator) {
    this.serviceRepository = serviceRepository;
    this.taskRepository = taskRepository;
    this.userRepository = userRepository;
    this.territoryRepository = territoryRepository;
    this.queryFixedFiltersDecorator = queryFixedFiltersDecorator;
    this.queryVaryFiltersDecorator = queryVaryFiltersDecorator;
    this.queryPaginationDecorator = queryPaginationDecorator;
  }

  private static OgcWmsPayloadDto getOgcWmsConfiguration(Service service, ConfigProxyRequest configProxyRequest) {

    if (service == null) {
      return null;
    }

    HttpSecurityDto security = null;
    if (Boolean.TRUE.equals(service.getPasswordSet())) {
      security = HttpSecurityDto.builder()
        .type("http")
        .scheme("basic")
        .username(service.getUser())
        .password(service.getPassword())
        .build();
    }

    Map<String, String> parameters = configProxyRequest.getParameters();
    if (parameters == null) {
      parameters = new HashMap<>();
    }
    Set<ServiceParameter> servParams = service.getParameters();
    log.info("Parametros servicio {}", servParams.size());
    List<String> varyParameters = new ArrayList<>();
    for (ServiceParameter parameter : servParams) {
      if (VARY_KEY.equalsIgnoreCase(parameter.getType())) {
        varyParameters.add(parameter.getName());
      } else {
        parameters.put(parameter.getName(), parameter.getValue());
      }
    }
    log.info("Parametros finales {}", parameters.size());
    return OgcWmsPayloadDto.builder()
      .uri(service.getServiceURL())
      .method(configProxyRequest.getMethod())
      .vary(varyParameters)
      .parameters(parameters)
      .security(security)
      .build();
  }

  private static String getSqlByTask(Task task) {
    String sql = "";
    Map<String, Object> taskParams = task.getProperties();
    if (taskParams != null && taskParams.containsKey(SQL_COMMAND_KEY)) {
      sql = (String) taskParams.get(SQL_COMMAND_KEY);
    }

    if (!StringUtils.hasText(sql)) {
      throw new BadRequestException("Bad request");
    }
    return sql;
  }

  private static DatasourcePayloadDto getDatasourceConfiguration(Task task) {
    DatabaseConnection databaseConnection = task.getConnection();
    String sql = getSqlByTask(task);
    return databaseConnection != null ? DatasourcePayloadDto.builder()
      .uri(databaseConnection.getUrl())
      .user(databaseConnection.getUser())
      .password(databaseConnection.getPassword())
      .driver(databaseConnection.getDriver())
      .sql(sql)
      .build() : null;
  }

  public boolean validateUserAccess(ConfigProxyRequest configProxyRequest, String userName) {
    // TODO Implement user validation
    return true;
  }

  public ConfigProxyDto getConfiguration(ConfigProxyRequest configProxyRequest, long expirationTimeToken) {
    log.info("Fetching configuration for service type {} with id {}", configProxyRequest.getType(), configProxyRequest.getTypeId());
    AtomicReference<PayloadDto> payload = new AtomicReference<>(null);
    AtomicReference<String> configType = new AtomicReference<>("");
    if (CONNECTION_TYPE_KEY.equalsIgnoreCase(configProxyRequest.getType())) {
      taskRepository.findById(configProxyRequest.getTypeId()).ifPresent(task -> {
        payload.set(getDatasourceConfiguration(task));
        configType.set(CONNECTION_TYPE_KEY);
      });
    } else {
      log.info("Searching service type {} with id {}", configProxyRequest.getType(), configProxyRequest.getTypeId());
      serviceRepository.findById(configProxyRequest.getTypeId()).ifPresent(service -> {
        payload.set(getOgcWmsConfiguration(service, configProxyRequest));
        configType.set(service.getType());
      });
    }
    if (payload.get() != null) {
      long expirationTime = expirationTimeToken > 0 ? expirationTimeToken / 1000
        : (new Date().getTime() / 1000) + responseValidityTime;
      return ConfigProxyDto.builder()
        .type(configType.get())
        .exp(expirationTime)
        .payload(payload.get())
        .build();
    }
    throw new BadRequestException("Bad request for service type "+configProxyRequest.getType()+" with id "+configProxyRequest.getTypeId());
  }

  public void applyDecorators(ConfigProxyDto configProxyDto, ConfigProxyRequest configProxyRequest, String username) {
    PayloadDto payload = configProxyDto.getPayload();

    addFixedFilters(configProxyRequest, payload, username);
    Map<String, String> parameters = configProxyRequest.getParameters();

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

  private void addFixedFilters(ConfigProxyRequest configProxyRequest, PayloadDto payload, String username) {
    Map<String, String> fixedFilters = new HashMap<>();
    userRepository.findByUsername(username).ifPresent(user -> fixedFilters.put("USER_ID", user.getId().toString()));
    Territory territory = territoryRepository.findById(configProxyRequest.getTerId()).orElse(null);
    if (territory != null) {
      fixedFilters.put("TERR_ID", String.valueOf(territory.getId()));
      fixedFilters.put("TERR_COD", territory.getCode());
    }
    fixedFilters.put("APP_ID", String.valueOf(configProxyRequest.getAppId()));

    queryFixedFiltersDecorator.apply(fixedFilters, payload);
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
