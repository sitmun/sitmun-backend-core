package org.sitmun.authorization.proxy.service;

import static org.sitmun.authorization.proxy.decorators.QueryPaginationDecorator.SQL_LIMIT;
import static org.sitmun.authorization.proxy.decorators.QueryPaginationDecorator.SQL_OFFSET;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
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

  @Value("${sitmun.proxy-middleware.config-response-validity-in-seconds}")
  private int responseValidityTime;

  public ProxyConfigurationService(
      ServiceRepository serviceRepository,
      TaskRepository taskRepository,
      UserRepository userRepository,
      TerritoryRepository territoryRepository,
      QueryFixedFiltersDecorator queryFixedFiltersDecorator,
      QueryVaryFiltersDecorator queryVaryFiltersDecorator,
      QueryPaginationDecorator queryPaginationDecorator) {
    this.serviceRepository = serviceRepository;
    this.taskRepository = taskRepository;
    this.userRepository = userRepository;
    this.territoryRepository = territoryRepository;
    this.queryFixedFiltersDecorator = queryFixedFiltersDecorator;
    this.queryVaryFiltersDecorator = queryVaryFiltersDecorator;
    this.queryPaginationDecorator = queryPaginationDecorator;
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
      if (VARY_KEY.equalsIgnoreCase(parameter.getType())) {
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
    if (taskParams != null && taskParams.containsKey(SQL_COMMAND_KEY)) {
      sql = (String) taskParams.get(SQL_COMMAND_KEY);
    }

    if (!StringUtils.hasText(sql)) {
      throw new BadRequestException("Bad request");
    }
    return sql;
  }

  private static JdbcPayloadDto getDatasourceConfiguration(Task task) {
    DatabaseConnection databaseConnection = task.getConnection();
    String sql = getSqlByTask(task);
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

  public boolean validateUserAccess(ConfigProxyRequestDto configProxyRequestDto, String userName) {
    // TODO: Implement user validation
    return true;
  }

  public ConfigProxyDto getConfiguration(
      ConfigProxyRequestDto configProxyRequestDto, long expirationTimeToken) {
    log.info(
        "Fetching configuration for service type {} with id {}",
        configProxyRequestDto.getType(),
        configProxyRequestDto.getTypeId());
    AtomicReference<PayloadDto> payload = new AtomicReference<>(null);
    AtomicReference<String> configType = new AtomicReference<>("");
    if (CONNECTION_TYPE_KEY.equalsIgnoreCase(configProxyRequestDto.getType())) {
      taskRepository
          .findById(configProxyRequestDto.getTypeId())
          .ifPresent(
              task -> {
                payload.set(getDatasourceConfiguration(task));
                configType.set(CONNECTION_TYPE_KEY);
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

    addFixedFilters(configProxyRequestDto, payload, username);
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

  private void addFixedFilters(
      ConfigProxyRequestDto configProxyRequestDto, PayloadDto payload, String username) {
    Map<String, String> fixedFilters = new HashMap<>();
    userRepository
        .findByUsername(username)
        .ifPresent(user -> fixedFilters.put("USER_ID", user.getId().toString()));
    Territory territory =
        territoryRepository.findById(configProxyRequestDto.getTerId()).orElse(null);
    if (territory != null) {
      fixedFilters.put("TERR_ID", String.valueOf(territory.getId()));
      fixedFilters.put("TERR_COD", territory.getCode());
    }
    fixedFilters.put("APP_ID", String.valueOf(configProxyRequestDto.getAppId()));

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
