package org.sitmun.authorization.proxy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sitmun.infrastructure.security.core.SecurityConstants.PUBLIC_PRINCIPAL;

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
import org.sitmun.domain.application.Application;
import org.sitmun.domain.application.ApplicationRepository;
import org.sitmun.domain.cartography.Cartography;
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

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Proxy validateUserAccess — account and app privacy gates")
class ProxyValidateUserAccessGateTest {

  private static final String ACTIVE_USERNAME = "activeuser";
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
  private Service accessibleService;

  @BeforeEach
  void setUp() {
    ServiceResourceAccessValidator serviceResourceAccessValidator =
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

    accessibleService = Service.builder().id(SERVICE_ID).blocked(false).build();
    when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.of(accessibleService));
    when(applicationRepository.existsById(APP_ID)).thenReturn(true);
    when(territoryRepository.existsById(TER_ID)).thenReturn(true);
  }

  @Test
  @DisplayName("denies blocked authenticated user despite roles")
  void deniesBlockedAuthenticatedUserDespiteRoles() {
    User blockedUser = userWithRoles(ACTIVE_USERNAME, true);
    stubWmsAccessFor(ACTIVE_USERNAME, blockedUser);

    assertThat(proxyConfigurationService.validateUserAccess(wmsRequest(), ACTIVE_USERNAME))
        .isFalse();
  }

  @Test
  @DisplayName("denies public principal when public user account is blocked")
  void deniesPublicPrincipalWhenPublicUserAccountIsBlocked() {
    User blockedPublic = userWithRoles(PUBLIC_PRINCIPAL, true);
    stubWmsAccessFor(PUBLIC_PRINCIPAL, blockedPublic);
    when(applicationRepository.findById(APP_ID))
        .thenReturn(Optional.of(Application.builder().id(APP_ID).appPrivate(false).build()));

    assertThat(proxyConfigurationService.validateUserAccess(wmsRequest(), PUBLIC_PRINCIPAL))
        .isFalse();
  }

  @Test
  @DisplayName("denies public principal on private application")
  void deniesPublicPrincipalOnPrivateApplication() {
    User publicUser = userWithRoles(PUBLIC_PRINCIPAL, false);
    stubWmsAccessFor(PUBLIC_PRINCIPAL, publicUser);
    when(applicationRepository.findById(APP_ID))
        .thenReturn(Optional.of(Application.builder().id(APP_ID).appPrivate(true).build()));

    assertThat(proxyConfigurationService.validateUserAccess(wmsRequest(), PUBLIC_PRINCIPAL))
        .isFalse();
  }

  @Test
  @DisplayName("allows public principal on public application when RBAC passes")
  void allowsPublicPrincipalOnPublicApplication() {
    User publicUser = userWithRoles(PUBLIC_PRINCIPAL, false);
    stubWmsAccessFor(PUBLIC_PRINCIPAL, publicUser);
    when(applicationRepository.findById(APP_ID))
        .thenReturn(Optional.of(Application.builder().id(APP_ID).appPrivate(false).build()));

    assertThat(proxyConfigurationService.validateUserAccess(wmsRequest(), PUBLIC_PRINCIPAL))
        .isTrue();
  }

  @Test
  @DisplayName("allows authenticated user on private application when RBAC passes")
  void allowsAuthenticatedUserOnPrivateApplication() {
    User activeUser = userWithRoles(ACTIVE_USERNAME, false);
    stubWmsAccessFor(ACTIVE_USERNAME, activeUser);

    assertThat(proxyConfigurationService.validateUserAccess(wmsRequest(), ACTIVE_USERNAME))
        .isTrue();
  }

  @Test
  @DisplayName("still allows blocked user when validate-user-access config is disabled")
  void stillAllowsBlockedUserWhenValidateUserAccessConfigDisabled() {
    ReflectionTestUtils.setField(proxyConfigurationService, "validateUserAccessEnabled", false);

    assertThat(proxyConfigurationService.validateUserAccess(wmsRequest(), ACTIVE_USERNAME))
        .isTrue();
  }

  private void stubWmsAccessFor(String username, User user) {
    when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
    when(cartographyRepository.findByRolesAndTerritory(anyList(), eq(TER_ID)))
        .thenReturn(List.of(Cartography.builder().service(accessibleService).build()));
  }

  private static User userWithRoles(String username, boolean blocked) {
    User user =
        User.builder().id(1).username(username).blocked(blocked).administrator(false).build();
    Role role = mock(Role.class);
    UserConfiguration config = mock(UserConfiguration.class);
    Territory territory = mock(Territory.class);
    when(territory.getId()).thenReturn(TER_ID);
    when(config.getTerritory()).thenReturn(territory);
    when(config.getRole()).thenReturn(role);
    user.setPermissions(Set.of(config));
    return user;
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
