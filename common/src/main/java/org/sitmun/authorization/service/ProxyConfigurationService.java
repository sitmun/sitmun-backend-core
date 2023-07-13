package org.sitmun.authorization.service;

import org.sitmun.authorization.dto.*;
import org.sitmun.authorization.exception.BadRequestException;
import org.sitmun.domain.cartography.CartographyRepository;
import org.sitmun.domain.database.DatabaseConnection;
import org.sitmun.domain.service.Service;
import org.sitmun.domain.service.parameter.ServiceParameter;
import org.sitmun.domain.task.Task;
import org.sitmun.domain.task.TaskRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
public class ProxyConfigurationService {

  private static final String SQL_COMMAND_KEY = "command";

  private static final String CONNECTION_TYPE_KEY = "SQL";

  private static final String VARY_KEY = "VARY";

  private final CartographyRepository cartographyRepository;

  private final TaskRepository taskRepository;

  @Value("${sitmun.proxy.config-response-validity-in-seconds}")
  private int responseValidityTime;

  public ProxyConfigurationService(CartographyRepository cartographyRepository,
      TaskRepository taskRepository) {
    this.cartographyRepository = cartographyRepository;
    this.taskRepository = taskRepository;
  }

  public boolean validateUserAccess(ConfigProxyRequest configProxyRequest, String userName) {
    boolean result = true;

    return true;
  }

  public void addExtraParams(ConfigProxyRequest configProxyRequest, String username) {
    Map<String, String> parameters = configProxyRequest.getParameters();
    if (parameters == null) {
      parameters = new HashMap<>();
    }
    parameters.put("USER_ID", username);
    parameters.put("TERR_ID", String.valueOf(configProxyRequest.getTerId()));
    parameters.put("APP_ID", String.valueOf(configProxyRequest.getAppId()));
    configProxyRequest.setParameters(parameters);
  }

  public ConfigProxyDto getConfiguration(ConfigProxyRequest configProxyRequest, long expirationTimeToken) {
    AtomicReference<PayloadDto> payload = new AtomicReference<>(null);
    AtomicReference<String> configType = new AtomicReference<>("");
    if (CONNECTION_TYPE_KEY.equalsIgnoreCase(configProxyRequest.getType())) {
      taskRepository.findById(configProxyRequest.getTypeId()).ifPresent(task -> {
        payload.set(getDatasourceConfiguration(task, configProxyRequest.getParameters()));
        configType.set(CONNECTION_TYPE_KEY);
      });
    } else {
      cartographyRepository.findById(configProxyRequest.getTypeId()).ifPresent(cartography -> {
        Service service = cartography.getService();
        Map<String, String> parameters = configProxyRequest.getParameters();
        parameters.put("LAYERS", cartography.getLayers().stream()
            .collect(Collectors.joining(",")));
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
    } else {
      throw new BadRequestException("Bad request");
    }
  }

  private OgcWmsPayloadDto getOgcWmsConfiguration(Service service, ConfigProxyRequest configProxyRequest) {

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

    List<String> varyParameters = new ArrayList<>();
    Map<String, String> parameters = configProxyRequest.getParameters();
    if (parameters == null) {
      parameters = new HashMap<>();
    }

    for (ServiceParameter parameter : service.getParameters()) {
      if (VARY_KEY.equalsIgnoreCase(parameter.getType())) {
        varyParameters.add(parameter.getName());
      } else {
        parameters.put(parameter.getName(), parameter.getValue());
      }
    }

    return OgcWmsPayloadDto.builder()
        .uri(service.getServiceURL())
        .method(configProxyRequest.getMethod())
        .vary(varyParameters)
        .parameters(parameters)
        .security(security)
        .build();
  }

  private DatasourcePayloadDto getDatasourceConfiguration(Task task, Map<String, String> parameters) {
    DatabaseConnection databaseConnection = task.getConnection();
    String sql = formatSql(getSqlByTask(task), parameters);
    return databaseConnection != null ? DatasourcePayloadDto.builder()
        .uri(databaseConnection.getUrl())
        .user(databaseConnection.getUser())
        .password(databaseConnection.getPassword())
        .driver(databaseConnection.getDriver())
        .sql(sql)
        .build() : null;
  }

  private String getSqlByTask(Task task) {
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

  private String formatSql(String query, Map<String, String> parameters) {
    String result = query;
    if (result != null && !result.isEmpty()) {
      if (!query.contains("WHERE")) {
        result = result.concat(" where ");
      }
      for (String par : parameters.keySet()) {
        if (!par.equalsIgnoreCase("limit") && !par.equalsIgnoreCase("offset")) {
          if (par.equals("USER_ID") || par.equals("TERR_ID") || par.equals("APP_ID")) {
            result.replace("${" + par + "}", parameters.get(par));
          } else {
            result = result.concat(par + "=" + parameters.get(par) + " ");
          }
        }
      }
      if (parameters.containsKey("limit")) {
        result = result.concat("limit " + parameters.get("limit") + " ");
      }
      if (parameters.containsKey("offset")) {
        result = result.concat("offset " + parameters.get("offset") + " ");
      }
    }
    return result;
  }
}
