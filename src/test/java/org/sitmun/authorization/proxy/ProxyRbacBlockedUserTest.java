package org.sitmun.authorization.proxy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.sitmun.authorization.access.UserApplicationAccessPolicy;
import org.sitmun.authorization.proxy.decorators.HttpUserParametrizationDecorator;
import org.sitmun.authorization.proxy.decorators.QueryPaginationDecorator;
import org.sitmun.authorization.proxy.decorators.SqlUserParametrizationDecorator;
import org.sitmun.authorization.proxy.dto.ConfigProxyRequestDto;
import org.sitmun.authorization.proxy.service.ProxyConfigurationService;
import org.sitmun.authorization.proxy.validator.ServiceResourceAccessValidator;
import org.sitmun.domain.application.ApplicationRepository;
import org.sitmun.domain.cartography.CartographyRepository;
import org.sitmun.domain.role.Role;
import org.sitmun.domain.service.Service;
import org.sitmun.domain.service.ServiceRepository;
import org.sitmun.domain.task.TaskRepository;
import org.sitmun.domain.task.parameter.TaskParameterProcessor;
import org.sitmun.domain.territory.Territory;
import org.sitmun.domain.territory.TerritoryRepository;
import org.sitmun.domain.user.User;
import org.sitmun.domain.user.UserRepository;
import org.sitmun.domain.user.configuration.UserConfiguration;
import org.sitmun.infrastructure.variables.SystemVariableResolver;
import org.springframework.test.util.ReflectionTestUtils;

/** Proxy {@code validateUserAccess} must deny blocked accounts and honor service blocked flag. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Proxy RBAC — blocked user account")
class ProxyRbacBlockedUserTest {

  private static final String BLOCKED_USERNAME = "blockeduser";
  private static final int APP_ID = 1;
  private static final int TER_ID = 1;
  private static final int SERVICE_ID = 153;

  @Mock private ServiceRepository serviceRepository;
  @Mock private UserRepository userRepository;
  @Mock private ApplicationRepository applicationRepository;
  @Mock private TerritoryRepository territoryRepository;
  @Mock private CartographyRepository cartographyRepository;
  @Mock private TaskRepository taskRepository;
  @Mock private SqlUserParametrizationDecorator sqlUserParametrizationDecorator;
  @Mock private HttpUserParametrizationDecorator httpUserParametrizationDecorator;
  @Mock private QueryPaginationDecorator queryPaginationDecorator;
  @Mock private SystemVariableResolver systemVariableResolver;
  @Mock private org.sitmun.domain.task.MoreInfoTaskResolver moreInfoTaskResolver;

  private ProxyConfigurationService proxyConfigurationService;
  private ServiceResourceAccessValidator serviceResourceAccessValidator;
  private User blockedUser;

  @BeforeEach
  void setUp() {
    serviceResourceAccessValidator =
        new ServiceResourceAccessValidator(
            serviceRepository,
            userRepository,
            applicationRepository,
            territoryRepository,
            cartographyRepository);

    TaskParameterProcessor taskParameterProcessor =
        new TaskParameterProcessor(systemVariableResolver);
    proxyConfigurationService =
        new ProxyConfigurationService(
            serviceRepository,
            taskRepository,
            userRepository,
            territoryRepository,
            applicationRepository,
            sqlUserParametrizationDecorator,
            httpUserParametrizationDecorator,
            queryPaginationDecorator,
            List.of(serviceResourceAccessValidator),
            systemVariableResolver,
            moreInfoTaskResolver,
            taskParameterProcessor,
            new UserApplicationAccessPolicy(userRepository, applicationRepository));
    ReflectionTestUtils.setField(proxyConfigurationService, "validateUserAccessEnabled", true);

    blockedUser =
        User.builder().id(42).username(BLOCKED_USERNAME).blocked(true).administrator(false).build();
    Role role = mock(Role.class);
    UserConfiguration config = mock(UserConfiguration.class);
    Territory territory = mock(Territory.class);
    when(territory.getId()).thenReturn(TER_ID);
    when(config.getTerritory()).thenReturn(territory);
    when(config.getRole()).thenReturn(role);
    blockedUser.setPermissions(Set.of(config));
  }

  @Test
  @DisplayName("RBAC denies blocked service; validateUserAccess denies blocked user account")
  void rbacDeniesBlockedServiceAndDeniesBlockedUserAtGate() {
    ConfigProxyRequestDto request = wmsRequest();

    Service blockedService = Service.builder().id(SERVICE_ID).blocked(true).build();
    when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.of(blockedService));
    assertThat(serviceResourceAccessValidator.validate(request, BLOCKED_USERNAME)).isFalse();

    when(userRepository.findByUsername(BLOCKED_USERNAME)).thenReturn(Optional.of(blockedUser));
    assertThat(proxyConfigurationService.validateUserAccess(request, BLOCKED_USERNAME)).isFalse();
  }

  @Test
  @DisplayName("validateUserAccess denies blocked user account despite roles")
  void validateUserAccessDeniesBlockedUserAccount() {
    when(userRepository.findByUsername(BLOCKED_USERNAME)).thenReturn(Optional.of(blockedUser));

    assertThat(blockedUser.getBlocked()).isTrue();
    assertThat(proxyConfigurationService.validateUserAccess(wmsRequest(), BLOCKED_USERNAME))
        .isFalse();
  }

  private static ConfigProxyRequestDto wmsRequest() {
    return ConfigProxyRequestDto.builder()
        .appId(APP_ID)
        .terId(TER_ID)
        .type("WMS")
        .typeId(SERVICE_ID)
        .build();
  }
}
