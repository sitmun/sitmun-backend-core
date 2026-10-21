package org.sitmun.authorization.proxy.validator;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sitmun.authorization.proxy.dto.ConfigProxyRequestDto;
import org.sitmun.domain.application.ApplicationRepository;
import org.sitmun.domain.cartography.Cartography;
import org.sitmun.domain.cartography.CartographyRepository;
import org.sitmun.domain.cartography.parameter.CartographyServiceParameterRepository;
import org.sitmun.domain.role.Role;
import org.sitmun.domain.role.RoleRepository;
import org.sitmun.domain.service.Service;
import org.sitmun.domain.service.ServiceRepository;
import org.sitmun.domain.task.Task;
import org.sitmun.domain.task.TaskRepository;
import org.sitmun.domain.territory.Territory;
import org.sitmun.domain.territory.TerritoryRepository;
import org.sitmun.domain.user.User;
import org.sitmun.domain.user.UserRepository;
import org.sitmun.domain.user.configuration.UserConfiguration;

@ExtendWith(MockitoExtension.class)
class ProxyApplicationRoleValidatorTest {

  private static final String USER = "territory-user";
  private static final int APPLICATION_ID = 20;
  private static final int TERRITORY_ID = 7;
  private static final int TASK_ID = 55;
  private static final int SERVICE_ID = 153;

  @Mock private TaskRepository taskRepository;
  @Mock private ServiceRepository serviceRepository;
  @Mock private UserRepository userRepository;
  @Mock private ApplicationRepository applicationRepository;
  @Mock private TerritoryRepository territoryRepository;
  @Mock private CartographyRepository cartographyRepository;
  @Mock private CartographyServiceParameterRepository cartographyServiceParameterRepository;
  @Mock private RoleRepository roleRepository;

  private TaskResourceAccessValidator taskValidator;
  private ServiceResourceAccessValidator serviceValidator;

  @BeforeEach
  void setUp() {
    taskValidator =
        new TaskResourceAccessValidator(
            taskRepository,
            userRepository,
            applicationRepository,
            territoryRepository,
            roleRepository);
    serviceValidator =
        new ServiceResourceAccessValidator(
            serviceRepository,
            userRepository,
            applicationRepository,
            territoryRepository,
            cartographyRepository,
            cartographyServiceParameterRepository,
            roleRepository);

    User user = userWithTerritoryRole();
    lenient().when(userRepository.findByUsername(USER)).thenReturn(Optional.of(user));
    lenient().when(applicationRepository.existsById(APPLICATION_ID)).thenReturn(true);
    lenient().when(territoryRepository.existsById(TERRITORY_ID)).thenReturn(true);
    lenient().when(taskRepository.existsById(TASK_ID)).thenReturn(true);
    lenient()
        .when(serviceRepository.findById(SERVICE_ID))
        .thenReturn(Optional.of(Service.builder().id(SERVICE_ID).blocked(false).build()));
  }

  @Test
  @DisplayName("denies task proxy when the territory role is not a role of the application")
  void deniesTaskProxyWhenRoleIsNotOnTheApplication() {
    lenient()
        .when(taskRepository.findByRolesAndTerritory(anyList(), eq(TERRITORY_ID)))
        .thenReturn(List.of(Task.builder().id(TASK_ID).build()));

    assertFalse(taskValidator.validate(taskRequest(), USER));
    verify(roleRepository)
        .findRolesByApplicationAndUserAndTerritory(USER, APPLICATION_ID, TERRITORY_ID);
    verify(taskRepository, never()).findByRolesAndTerritory(anyList(), eq(TERRITORY_ID));
  }

  @Test
  @DisplayName("allows task proxy when the role belongs to the application and territory")
  void allowsTaskProxyWhenRoleIsOnTheApplication() {
    Role role = mock(Role.class);
    lenient()
        .when(
            roleRepository.findRolesByApplicationAndUserAndTerritory(
                USER, APPLICATION_ID, TERRITORY_ID))
        .thenReturn(List.of(role));
    lenient()
        .when(taskRepository.findByRolesAndTerritory(anyList(), eq(TERRITORY_ID)))
        .thenReturn(List.of(Task.builder().id(TASK_ID).build()));

    assertTrue(taskValidator.validate(taskRequest(), USER));
    verify(roleRepository)
        .findRolesByApplicationAndUserAndTerritory(USER, APPLICATION_ID, TERRITORY_ID);
    verify(taskRepository).findByRolesAndTerritory(List.of(role), TERRITORY_ID);
  }

  @Test
  @DisplayName("denies service proxy when the territory role is not a role of the application")
  void deniesServiceProxyWhenRoleIsNotOnTheApplication() {
    Cartography cartography = cartographyOnService();
    lenient()
        .when(cartographyRepository.findByRolesAndTerritory(anyList(), eq(TERRITORY_ID)))
        .thenReturn(List.of(cartography));

    assertFalse(serviceValidator.validate(serviceRequest(), USER));
    verify(roleRepository)
        .findRolesByApplicationAndUserAndTerritory(USER, APPLICATION_ID, TERRITORY_ID);
    verify(cartographyRepository, never()).findByRolesAndTerritory(anyList(), eq(TERRITORY_ID));
  }

  @Test
  @DisplayName("allows service proxy when the role belongs to the application and territory")
  void allowsServiceProxyWhenRoleIsOnTheApplication() {
    Role role = mock(Role.class);
    Cartography cartography = cartographyOnService();
    lenient()
        .when(
            roleRepository.findRolesByApplicationAndUserAndTerritory(
                USER, APPLICATION_ID, TERRITORY_ID))
        .thenReturn(List.of(role));
    lenient()
        .when(cartographyRepository.findByRolesAndTerritory(anyList(), eq(TERRITORY_ID)))
        .thenReturn(List.of(cartography));

    assertTrue(serviceValidator.validate(serviceRequest(), USER));
    verify(roleRepository)
        .findRolesByApplicationAndUserAndTerritory(USER, APPLICATION_ID, TERRITORY_ID);
    verify(cartographyRepository).findByRolesAndTerritory(List.of(role), TERRITORY_ID);
  }

  private static User userWithTerritoryRole() {
    Territory territory = Territory.builder().id(TERRITORY_ID).build();
    Role role = Role.builder().id(4).build();
    UserConfiguration config = UserConfiguration.builder().territory(territory).role(role).build();
    User user = User.builder().id(1).username(USER).blocked(false).administrator(false).build();
    user.setPermissions(Set.of(config));
    return user;
  }

  private static Cartography cartographyOnService() {
    Cartography cartography = mock(Cartography.class);
    lenient()
        .when(cartography.getService())
        .thenReturn(Service.builder().id(SERVICE_ID).blocked(false).build());
    return cartography;
  }

  private static ConfigProxyRequestDto taskRequest() {
    return ConfigProxyRequestDto.builder()
        .appId(APPLICATION_ID)
        .terId(TERRITORY_ID)
        .type("SQL")
        .typeId(TASK_ID)
        .build();
  }

  private static ConfigProxyRequestDto serviceRequest() {
    return ConfigProxyRequestDto.builder()
        .appId(APPLICATION_ID)
        .terId(TERRITORY_ID)
        .type("WMS")
        .typeId(SERVICE_ID)
        .build();
  }
}
