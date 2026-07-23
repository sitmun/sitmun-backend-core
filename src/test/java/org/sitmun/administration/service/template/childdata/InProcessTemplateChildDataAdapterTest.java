package org.sitmun.administration.service.template.childdata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.sitmun.administration.service.database.DatabaseConnectionService;
import org.sitmun.administration.service.extractor.HttpClientFactory;
import org.sitmun.authorization.proxy.dto.ConfigProxyDto;
import org.sitmun.authorization.proxy.protocols.jdbc.JdbcPayloadDto;
import org.sitmun.authorization.proxy.service.ProxyConfigurationService;
import org.sitmun.authorization.proxy.service.RequestCoordinates;
import org.sitmun.domain.DomainConstants;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.task.Task;
import org.sitmun.domain.territory.Territory;
import org.sitmun.domain.user.User;
import org.sitmun.infrastructure.variables.SystemVariableResolver;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class InProcessTemplateChildDataAdapterTest {

  @AfterEach
  void clearSecurity() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void executeSqlPassesCoordinatesToGetConfiguration() throws Exception {
    ProxyConfigurationService proxyConfigurationService = mock(ProxyConfigurationService.class);
    DatabaseConnectionService databaseConnectionService = mock(DatabaseConnectionService.class);
    InProcessTemplateChildDataAdapter adapter =
        new InProcessTemplateChildDataAdapter(
            proxyConfigurationService,
            databaseConnectionService,
            mock(HttpClientFactory.class),
            mock(SystemVariableResolver.class),
            new ObjectMapper());

    authenticate("admin", "ROLE_ADMIN");
    Task task =
        Task.builder()
            .id(13)
            .properties(
                Map.of(DomainConstants.Tasks.PROPERTY_SCOPE, DomainConstants.Tasks.SCOPE_SQL_QUERY))
            .build();
    RequestCoordinates coordinates = coordinates();
    JdbcPayloadDto payload =
        JdbcPayloadDto.builder()
            .driver("org.h2.Driver")
            .uri("jdbc:h2:mem:test")
            .user("sa")
            .password("")
            .sql("select 1")
            .build();
    when(proxyConfigurationService.getConfiguration(any(), anyLong(), eq(coordinates)))
        .thenReturn(ConfigProxyDto.builder().type("JDBC").payload(payload).build());
    when(databaseConnectionService.executeQuery(any(), any(), any()))
        .thenReturn(List.of(Map.of("n", 1)));

    ChildDataResult result =
        adapter.executeSql(
            ChildDataRequest.builder()
                .appId(5)
                .terId(7)
                .taskId(task.getId())
                .task(task)
                .parameters(Map.of("featureId", "1"))
                .principalKind(PrincipalKind.ADMIN)
                .coordinates(coordinates)
                .scope(DomainConstants.Tasks.SCOPE_SQL_QUERY)
                .build());

    assertThat(result.getOutcome()).isEqualTo(ChildDataOutcome.OK);
    ArgumentCaptor<RequestCoordinates> coordinatesCaptor =
        ArgumentCaptor.forClass(RequestCoordinates.class);
    verify(proxyConfigurationService)
        .getConfiguration(any(), anyLong(), coordinatesCaptor.capture());
    assertThat(coordinatesCaptor.getValue()).isSameAs(coordinates);
  }

  @Test
  void adminSkipsValidateUserAccess() throws Exception {
    ProxyConfigurationService proxyConfigurationService = mock(ProxyConfigurationService.class);
    DatabaseConnectionService databaseConnectionService = mock(DatabaseConnectionService.class);
    HttpClientFactory httpClientFactory = mock(HttpClientFactory.class);
    InProcessTemplateChildDataAdapter adapter =
        new InProcessTemplateChildDataAdapter(
            proxyConfigurationService,
            databaseConnectionService,
            httpClientFactory,
            mock(SystemVariableResolver.class),
            new ObjectMapper());

    authenticate("admin", "ROLE_ADMIN");
    Task task =
        Task.builder()
            .id(13)
            .properties(
                Map.of(DomainConstants.Tasks.PROPERTY_SCOPE, DomainConstants.Tasks.SCOPE_SQL_QUERY))
            .build();
    JdbcPayloadDto payload =
        JdbcPayloadDto.builder()
            .driver("org.h2.Driver")
            .uri("jdbc:h2:mem:test")
            .user("sa")
            .password("")
            .sql("select 1")
            .build();
    when(proxyConfigurationService.getConfiguration(any(), eq(0L), any()))
        .thenReturn(ConfigProxyDto.builder().type("JDBC").payload(payload).build());
    when(databaseConnectionService.executeQuery(any(), any(), any()))
        .thenReturn(List.of(Map.of("n", 1)));

    ChildDataResult result = adapter.executeSql(sqlRequest(task, PrincipalKind.ADMIN));

    assertThat(result.getOutcome()).isEqualTo(ChildDataOutcome.OK);
    verify(proxyConfigurationService, never()).validateUserAccess(any(), any());
    verify(databaseConnectionService).executeQuery(any(), any(), any());
  }

  @Test
  void userValidateUserAccessFalseReturnsNoDataWithoutJdbc() throws Exception {
    ProxyConfigurationService proxyConfigurationService = mock(ProxyConfigurationService.class);
    DatabaseConnectionService databaseConnectionService = mock(DatabaseConnectionService.class);
    when(proxyConfigurationService.validateUserAccess(any(), any())).thenReturn(false);
    InProcessTemplateChildDataAdapter adapter =
        new InProcessTemplateChildDataAdapter(
            proxyConfigurationService,
            databaseConnectionService,
            mock(HttpClientFactory.class),
            mock(SystemVariableResolver.class),
            new ObjectMapper());

    authenticate("viewer", "ROLE_USER");
    Task task =
        Task.builder()
            .id(13)
            .properties(
                Map.of(DomainConstants.Tasks.PROPERTY_SCOPE, DomainConstants.Tasks.SCOPE_SQL_QUERY))
            .build();

    ChildDataResult result = adapter.executeSql(sqlRequest(task, PrincipalKind.USER));

    assertThat(result.getOutcome()).isEqualTo(ChildDataOutcome.NO_DATA);
    verify(proxyConfigurationService).validateUserAccess(any(), any());
    verify(proxyConfigurationService, never()).getConfiguration(any(), any(Long.class), any());
    verify(databaseConnectionService, never()).executeQuery(any(), any(), any());
  }

  @Test
  void noProxyResolveDirectDoesNotCallHttpClientFactory() throws Exception {
    HttpClientFactory httpClientFactory = mock(HttpClientFactory.class);
    SystemVariableResolver systemVariableResolver = mock(SystemVariableResolver.class);
    when(systemVariableResolver.resolve(any(), any())).thenReturn(null);
    InProcessTemplateChildDataAdapter adapter =
        new InProcessTemplateChildDataAdapter(
            mock(ProxyConfigurationService.class),
            mock(DatabaseConnectionService.class),
            httpClientFactory,
            systemVariableResolver,
            new ObjectMapper());

    Task task =
        Task.builder()
            .id(99)
            .properties(
                Map.of(
                    DomainConstants.Tasks.PROPERTY_SCOPE,
                    DomainConstants.Tasks.SCOPE_WEB_API_QUERY_NO_PROXY,
                    DomainConstants.Tasks.PROPERTY_COMMAND,
                    "https://example.org/resource/{id}"))
            .build();
    ChildDataRequest request =
        ChildDataRequest.builder()
            .appId(5)
            .terId(7)
            .taskId(99)
            .task(task)
            .parameters(Map.of("id", "1"))
            .principalKind(PrincipalKind.USER)
            .coordinates(coordinates())
            .scope(DomainConstants.Tasks.SCOPE_WEB_API_QUERY_NO_PROXY)
            .build();

    ChildDataResult result = adapter.resolveDirect(request);

    assertThat(result.getOutcome()).isEqualTo(ChildDataOutcome.OK);
    assertThat(result.getResourceUrl()).contains("https://example.org/resource/");
    verify(httpClientFactory, never()).executeRequest(any());
  }

  private ChildDataRequest sqlRequest(Task task, PrincipalKind principalKind) {
    return ChildDataRequest.builder()
        .appId(5)
        .terId(7)
        .taskId(task.getId())
        .task(task)
        .parameters(Map.of())
        .principalKind(principalKind)
        .coordinates(coordinates())
        .scope(DomainConstants.Tasks.SCOPE_SQL_QUERY)
        .build();
  }

  private RequestCoordinates coordinates() {
    RequestCoordinates coordinates = new RequestCoordinates();
    coordinates.setApplication(Application.builder().id(5).build());
    coordinates.setTerritory(Territory.builder().id(7).build());
    coordinates.setUser(User.builder().username("viewer").build());
    return coordinates;
  }

  private void authenticate(String username, String role) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                username, "n/a", List.of(new SimpleGrantedAuthority(role))));
  }
}
