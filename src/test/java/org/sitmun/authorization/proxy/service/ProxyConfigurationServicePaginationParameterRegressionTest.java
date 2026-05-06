package org.sitmun.authorization.proxy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.sitmun.domain.DomainConstants.Proxy.TYPE_SQL;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sitmun.authorization.proxy.decorators.HttpUserParametrizationDecorator;
import org.sitmun.authorization.proxy.decorators.QueryPaginationDecorator;
import org.sitmun.authorization.proxy.decorators.SqlUserParametrizationDecorator;
import org.sitmun.authorization.proxy.dto.ConfigProxyDto;
import org.sitmun.authorization.proxy.dto.ConfigProxyRequestDto;
import org.sitmun.authorization.proxy.protocols.jdbc.JdbcPayloadDto;
import org.sitmun.authorization.proxy.protocols.wms.WmsPayloadDto;
import org.sitmun.domain.application.ApplicationRepository;
import org.sitmun.domain.service.ServiceRepository;
import org.sitmun.domain.task.TaskRepository;
import org.sitmun.domain.territory.TerritoryRepository;
import org.sitmun.domain.user.UserRepository;
import org.sitmun.infrastructure.variables.SystemVariableResolver;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Regression tests for case-insensitive recognition of {@code LIMIT} / {@code OFFSET} request
 * parameters in {@link ProxyConfigurationService#applyDecorators}.
 *
 * <p>These fail while {@code applyDecorators} only reads exact uppercase keys {@code LIMIT} and
 * {@code OFFSET}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProxyConfigurationService LIMIT/OFFSET parameter casing (regression)")
class ProxyConfigurationServicePaginationParameterRegressionTest {

  @Mock private ServiceRepository serviceRepository;
  @Mock private TaskRepository taskRepository;
  @Mock private UserRepository userRepository;
  @Mock private TerritoryRepository territoryRepository;
  @Mock private ApplicationRepository applicationRepository;
  @Mock private SqlUserParametrizationDecorator sqlUserParametrizationDecorator;
  @Mock private HttpUserParametrizationDecorator httpUserParametrizationDecorator;
  @Mock private QueryPaginationDecorator queryPaginationDecorator;
  @Mock private SystemVariableResolver systemVariableResolver;
  @Mock private org.sitmun.domain.task.MoreInfoTaskResolver moreInfoTaskResolver;

  private ProxyConfigurationService service;

  @BeforeEach
  void setUp() {
    lenient()
        .when(systemVariableResolver.resolve(any(String.class), any(RequestCoordinates.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    service =
        new ProxyConfigurationService(
            serviceRepository,
            taskRepository,
            userRepository,
            territoryRepository,
            applicationRepository,
            sqlUserParametrizationDecorator,
            httpUserParametrizationDecorator,
            queryPaginationDecorator,
            Collections.emptyList(),
            systemVariableResolver,
            moreInfoTaskResolver);
    ReflectionTestUtils.setField(service, "responseValidityTime", 3600);
    ReflectionTestUtils.setField(service, "validateUserAccessEnabled", false);
  }

  private RequestCoordinates coordinates(ConfigProxyRequestDto request) {
    return service.getRequestCoordinates(request, "testuser");
  }

  @Test
  @DisplayName("Lowercase limit and offset are passed to JDBC pagination and stripped from request")
  void lowercaseLimitOffsetFeedJdbcPaginationAndAreStripped() {
    Map<String, String> requestParams = new HashMap<>();
    requestParams.put("limit", "10");
    requestParams.put("offset", "5");
    requestParams.put("userId", "7");

    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder()
            .appId(1)
            .terId(1)
            .type(TYPE_SQL)
            .typeId(1)
            .method("GET")
            .parameters(requestParams)
            .build();

    List<String> jdbcPositional = new ArrayList<>();
    JdbcPayloadDto jdbc =
        JdbcPayloadDto.builder()
            .vary(null)
            .uri("jdbc:h2:mem:test")
            .user("u")
            .password("p")
            .driver("org.h2.Driver")
            .sql("SELECT * FROM t WHERE id = ?")
            .parameters(jdbcPositional)
            .build();

    ConfigProxyDto config = ConfigProxyDto.builder().type(TYPE_SQL).exp(100).payload(jdbc).build();

    service.applyDecorators(config, request, coordinates(request));

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<String, String>> paginationCaptor = ArgumentCaptor.forClass(Map.class);
    verify(queryPaginationDecorator).apply(paginationCaptor.capture(), eq(jdbc));
    assertThat(paginationCaptor.getValue())
        .containsEntry(QueryPaginationDecorator.SQL_LIMIT, "10")
        .containsEntry(QueryPaginationDecorator.SQL_OFFSET, "5");

    assertThat(request.getParameters().keySet()).noneMatch("limit"::equalsIgnoreCase);
    assertThat(request.getParameters().keySet()).noneMatch("offset"::equalsIgnoreCase);
    assertThat(request.getParameters()).containsEntry("userId", "7");
  }

  @Test
  @DisplayName("Mixed-case Limit and OffSet are stripped from HTTP payload request parameters")
  void mixedCaseLimitOffsetStrippedForHttpPayload() {
    Map<String, String> requestParams = new HashMap<>();
    requestParams.put("Limit", "20");
    requestParams.put("OffSet", "3");
    requestParams.put("bbox", "0,0,1,1");

    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder()
            .appId(1)
            .terId(1)
            .type("WMS")
            .typeId(1)
            .method("GET")
            .parameters(requestParams)
            .build();

    WmsPayloadDto wms =
        WmsPayloadDto.builder()
            .vary(null)
            .uri("https://example.com/wms")
            .method("GET")
            .parameters(new HashMap<>())
            .security(null)
            .body(null)
            .build();

    ConfigProxyDto config = ConfigProxyDto.builder().type("WMS").exp(100).payload(wms).build();

    service.applyDecorators(config, request, coordinates(request));

    assertThat(request.getParameters().keySet()).noneMatch("limit"::equalsIgnoreCase);
    assertThat(request.getParameters().keySet()).noneMatch("offset"::equalsIgnoreCase);
    assertThat(request.getParameters()).containsEntry("bbox", "0,0,1,1");
  }
}
