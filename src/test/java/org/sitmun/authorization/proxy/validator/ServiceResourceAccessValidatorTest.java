package org.sitmun.authorization.proxy.validator;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
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
import org.sitmun.domain.service.Service;
import org.sitmun.domain.service.ServiceRepository;
import org.sitmun.domain.territory.Territory;
import org.sitmun.domain.territory.TerritoryRepository;
import org.sitmun.domain.user.User;
import org.sitmun.domain.user.UserRepository;
import org.sitmun.domain.user.configuration.UserConfiguration;

@ExtendWith(MockitoExtension.class)
class ServiceResourceAccessValidatorTest {

  @Mock private ServiceRepository serviceRepository;
  @Mock private UserRepository userRepository;
  @Mock private ApplicationRepository applicationRepository;
  @Mock private TerritoryRepository territoryRepository;
  @Mock private CartographyRepository cartographyRepository;
  @Mock private CartographyServiceParameterRepository cartographyServiceParameterRepository;

  private ServiceResourceAccessValidator validator;

  @BeforeEach
  void setUp() {
    validator =
        new ServiceResourceAccessValidator(
            serviceRepository,
            userRepository,
            applicationRepository,
            territoryRepository,
            cartographyRepository,
            cartographyServiceParameterRepository);
  }

  @Test
  @DisplayName("supports returns true for WMS service type")
  void supportsReturnsTrueForWms() {
    assertTrue(validator.supports("WMS"));
    assertTrue(validator.supports("wms"));
    assertTrue(validator.supports("Wms"));
  }

  @Test
  @DisplayName("supports returns true for WMTS service type")
  void supportsReturnsTrueForWmts() {
    assertTrue(validator.supports("WMTS"));
    assertTrue(validator.supports("wmts"));
  }

  @Test
  @DisplayName("supports returns true for WFS service type")
  void supportsReturnsTrueForWfs() {
    assertTrue(validator.supports("WFS"));
    assertTrue(validator.supports("wfs"));
  }

  @Test
  @DisplayName("supports returns false for unsupported service type")
  void supportsReturnsFalseForUnsupportedType() {
    assertFalse(validator.supports("SQL"));
    assertFalse(validator.supports("UNKNOWN"));
    // API is task-based, not service-based, so it's not supported here
    assertFalse(validator.supports("API"));
  }

  @Test
  @DisplayName("validate returns false when service not found")
  void validateReturnsFalseWhenServiceNotFound() {
    // Given
    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder().appId(1).terId(1).type("WMS").typeId(999).build();

    when(serviceRepository.findById(999)).thenReturn(Optional.empty());

    // When
    boolean result = validator.validate(request, "testuser");

    // Then
    assertFalse(result);
    verify(serviceRepository).findById(999);
  }

  @Test
  @DisplayName("validate returns false when service is blocked")
  void validateReturnsFalseWhenServiceIsBlocked() {
    // Given
    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder().appId(1).terId(1).type("WMS").typeId(153).build();

    Service blockedService = mock(Service.class);
    when(blockedService.getBlocked()).thenReturn(true);

    when(serviceRepository.findById(153)).thenReturn(Optional.of(blockedService));

    // When
    boolean result = validator.validate(request, "testuser");

    // Then
    assertFalse(result);
    verify(serviceRepository).findById(153);
    verify(blockedService).getBlocked();
  }

  @Test
  @DisplayName("validate returns false when service is blocked (using Boolean.TRUE)")
  void validateReturnsFalseWhenServiceIsBlockedBooleanTrue() {
    // Given
    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder().appId(1).terId(1).type("WMS").typeId(153).build();

    Service blockedService = Service.builder().id(153).blocked(Boolean.TRUE).build();

    when(serviceRepository.findById(153)).thenReturn(Optional.of(blockedService));

    // When
    boolean result = validator.validate(request, "testuser");

    // Then
    assertFalse(result);
  }

  @Test
  @DisplayName("validate continues validation when service is not blocked")
  void validateContinuesWhenServiceIsNotBlocked() {
    // Given
    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder().appId(1).terId(1).type("WMS").typeId(153).build();

    Service service = Service.builder().id(153).blocked(false).build();

    User user = mock(User.class);
    Role role = mock(Role.class);
    UserConfiguration config = mock(UserConfiguration.class);
    Territory territory = mock(Territory.class);

    when(territory.getId()).thenReturn(1);
    when(config.getTerritory()).thenReturn(territory);
    when(config.getRole()).thenReturn(role);
    when(user.getPermissions()).thenReturn(Set.of(config));

    Cartography cartography = mock(Cartography.class);
    when(cartography.getService()).thenReturn(service);

    when(serviceRepository.findById(153)).thenReturn(Optional.of(service));
    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
    when(applicationRepository.existsById(1)).thenReturn(true);
    when(territoryRepository.existsById(1)).thenReturn(true);
    when(cartographyRepository.findByRolesAndTerritory(anyList(), eq(1)))
        .thenReturn(List.of(cartography));

    // When
    boolean result = validator.validate(request, "testuser");

    // Then
    assertTrue(result);
    verify(serviceRepository).findById(153);
  }

  @Test
  @DisplayName("validate returns false when user not found")
  void validateReturnsFalseWhenUserNotFound() {
    // Given
    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder().appId(1).terId(1).type("WMS").typeId(153).build();

    Service service = Service.builder().id(153).blocked(false).build();

    when(serviceRepository.findById(153)).thenReturn(Optional.of(service));
    when(userRepository.findByUsername("unknownuser")).thenReturn(Optional.empty());

    // When
    boolean result = validator.validate(request, "unknownuser");

    // Then
    assertFalse(result);
  }

  @Test
  @DisplayName("validate returns false when application not found")
  void validateReturnsFalseWhenApplicationNotFound() {
    // Given
    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder().appId(999).terId(1).type("WMS").typeId(153).build();

    Service service = Service.builder().id(153).blocked(false).build();
    User user = mock(User.class);

    when(serviceRepository.findById(153)).thenReturn(Optional.of(service));
    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
    when(applicationRepository.existsById(999)).thenReturn(false);

    // When
    boolean result = validator.validate(request, "testuser");

    // Then
    assertFalse(result);
  }

  @Test
  @DisplayName("validate returns false when territory not found (terId > 0)")
  void validateReturnsFalseWhenTerritoryNotFound() {
    // Given
    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder().appId(1).terId(999).type("WMS").typeId(153).build();

    Service service = Service.builder().id(153).blocked(false).build();
    User user = mock(User.class);

    when(serviceRepository.findById(153)).thenReturn(Optional.of(service));
    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
    when(applicationRepository.existsById(1)).thenReturn(true);
    when(territoryRepository.existsById(999)).thenReturn(false);

    // When
    boolean result = validator.validate(request, "testuser");

    // Then
    assertFalse(result);
  }

  @Test
  @DisplayName("validate returns true when all validation passes (terId = 0 for public access)")
  void validateReturnsTrueForPublicAccessWithTerIdZero() {
    // Given - terId = 0 means public access, skip territory-based role checks
    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder().appId(1).terId(0).type("WMS").typeId(153).build();

    Service service = Service.builder().id(153).blocked(false).build();
    User user = mock(User.class);

    // For terId = 0, user should have empty permissions which will cause validation to fail
    // This is expected behavior - terId=0 doesn't bypass role checks
    when(user.getPermissions()).thenReturn(Set.of());

    when(serviceRepository.findById(153)).thenReturn(Optional.of(service));
    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
    when(applicationRepository.existsById(1)).thenReturn(true);

    // When
    boolean result = validator.validate(request, "testuser");

    // Then - Should fail because user has no roles for territory 0
    assertFalse(result);
    verify(territoryRepository, never()).existsById(anyInt());
  }

  @Test
  @DisplayName("validate returns true when all entities exist and service is not blocked")
  void validateReturnsTrueWhenAllValidationPasses() {
    // Given
    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder().appId(1).terId(1).type("WMS").typeId(153).build();

    Service service = Service.builder().id(153).blocked(false).build();

    User user = mock(User.class);
    Role role = mock(Role.class);
    UserConfiguration config = mock(UserConfiguration.class);
    Territory territory = mock(Territory.class);

    when(territory.getId()).thenReturn(1);
    when(config.getTerritory()).thenReturn(territory);
    when(config.getRole()).thenReturn(role);
    when(user.getPermissions()).thenReturn(Set.of(config));

    Cartography cartography = mock(Cartography.class);
    when(cartography.getService()).thenReturn(service);

    when(serviceRepository.findById(153)).thenReturn(Optional.of(service));
    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
    when(applicationRepository.existsById(1)).thenReturn(true);
    when(territoryRepository.existsById(1)).thenReturn(true);
    when(cartographyRepository.findByRolesAndTerritory(anyList(), eq(1)))
        .thenReturn(List.of(cartography));

    // When
    boolean result = validator.validate(request, "testuser");

    // Then
    assertTrue(result);
  }

  @Test
  @DisplayName("validate handles null blocked field as not blocked")
  void validateHandlesNullBlockedFieldAsNotBlocked() {
    // Given
    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder().appId(1).terId(1).type("WMS").typeId(153).build();

    Service service = mock(Service.class);
    when(service.getId()).thenReturn(153);
    when(service.getBlocked()).thenReturn(null);

    User user = mock(User.class);
    Role role = mock(Role.class);
    UserConfiguration config = mock(UserConfiguration.class);
    Territory territory = mock(Territory.class);

    when(territory.getId()).thenReturn(1);
    when(config.getTerritory()).thenReturn(territory);
    when(config.getRole()).thenReturn(role);
    when(user.getPermissions()).thenReturn(Set.of(config));

    Cartography cartography = mock(Cartography.class);
    when(cartography.getService()).thenReturn(service);

    when(serviceRepository.findById(153)).thenReturn(Optional.of(service));
    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
    when(applicationRepository.existsById(1)).thenReturn(true);
    when(territoryRepository.existsById(1)).thenReturn(true);
    when(cartographyRepository.findByRolesAndTerritory(anyList(), eq(1)))
        .thenReturn(List.of(cartography));

    // When
    boolean result = validator.validate(request, "testuser");

    // Then
    assertTrue(result);
  }

  // ========================================
  // Layer-Level Authorization Tests
  // ========================================

  @Test
  @DisplayName("Layer validation: exact match single cartography returns true")
  void testValidate_WithLayersParam_ExactMatchSingleCartography_ReturnsTrue() {
    // Given
    Map<String, String> parameters = Map.of("LAYERS", "id1,id2");
    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder()
            .appId(1)
            .terId(1)
            .type("WMS")
            .typeId(153)
            .parameters(parameters)
            .build();

    Service service = Service.builder().id(153).blocked(false).build();
    User user = mock(User.class);
    Role role = mock(Role.class);
    UserConfiguration config = mock(UserConfiguration.class);
    Territory territory = mock(Territory.class);

    when(territory.getId()).thenReturn(1);
    when(config.getTerritory()).thenReturn(territory);
    when(config.getRole()).thenReturn(role);
    when(user.getPermissions()).thenReturn(Set.of(config));

    Cartography cartography = mock(Cartography.class);
    when(cartography.getService()).thenReturn(service);
    when(cartography.getLayers()).thenReturn(List.of("id1", "id2"));

    when(serviceRepository.findById(153)).thenReturn(Optional.of(service));
    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
    when(applicationRepository.existsById(1)).thenReturn(true);
    when(territoryRepository.existsById(1)).thenReturn(true);
    when(cartographyRepository.findByRolesAndTerritory(anyList(), eq(1)))
        .thenReturn(List.of(cartography));

    // When
    boolean result = validator.validate(request, "testuser");

    // Then
    assertTrue(result);
  }

  @Test
  @DisplayName("Layer validation: exact match different order returns true")
  void testValidate_WithLayersParam_ExactMatchDifferentOrder_ReturnsTrue() {
    // Given - request has layers in different order
    Map<String, String> parameters = Map.of("LAYERS", "id2,id1");
    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder()
            .appId(1)
            .terId(1)
            .type("WMS")
            .typeId(153)
            .parameters(parameters)
            .build();

    Service service = Service.builder().id(153).blocked(false).build();
    User user = mock(User.class);
    Role role = mock(Role.class);
    UserConfiguration config = mock(UserConfiguration.class);
    Territory territory = mock(Territory.class);

    when(territory.getId()).thenReturn(1);
    when(config.getTerritory()).thenReturn(territory);
    when(config.getRole()).thenReturn(role);
    when(user.getPermissions()).thenReturn(Set.of(config));

    Cartography cartography = mock(Cartography.class);
    when(cartography.getService()).thenReturn(service);
    when(cartography.getLayers()).thenReturn(List.of("id1", "id2"));

    when(serviceRepository.findById(153)).thenReturn(Optional.of(service));
    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
    when(applicationRepository.existsById(1)).thenReturn(true);
    when(territoryRepository.existsById(1)).thenReturn(true);
    when(cartographyRepository.findByRolesAndTerritory(anyList(), eq(1)))
        .thenReturn(List.of(cartography));

    // When
    boolean result = validator.validate(request, "testuser");

    // Then
    assertTrue(result);
  }

  @Test
  @DisplayName("Layer validation: partial match subset returns false")
  void testValidate_WithLayersParam_PartialMatchSubset_ReturnsFalse() {
    // Given - request only has subset of cartography layers
    Map<String, String> parameters = Map.of("LAYERS", "id1");
    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder()
            .appId(1)
            .terId(1)
            .type("WMS")
            .typeId(153)
            .parameters(parameters)
            .build();

    Service service = Service.builder().id(153).blocked(false).build();
    User user = mock(User.class);
    Role role = mock(Role.class);
    UserConfiguration config = mock(UserConfiguration.class);
    Territory territory = mock(Territory.class);

    when(territory.getId()).thenReturn(1);
    when(config.getTerritory()).thenReturn(territory);
    when(config.getRole()).thenReturn(role);
    when(user.getPermissions()).thenReturn(Set.of(config));

    Cartography cartography = mock(Cartography.class);
    when(cartography.getService()).thenReturn(service);
    when(cartography.getLayers()).thenReturn(List.of("id1", "id2"));

    when(serviceRepository.findById(153)).thenReturn(Optional.of(service));
    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
    when(applicationRepository.existsById(1)).thenReturn(true);
    when(territoryRepository.existsById(1)).thenReturn(true);
    when(cartographyRepository.findByRolesAndTerritory(anyList(), eq(1)))
        .thenReturn(List.of(cartography));

    // When
    boolean result = validator.validate(request, "testuser");

    // Then
    assertFalse(result);
  }

  @Test
  @DisplayName("Layer validation: mixed layers from multiple cartographies returns false")
  void testValidate_WithLayersParam_MixedLayersFromMultipleCartographies_ReturnsFalse() {
    // Given - user has access to two cartographies, request mixes their layers
    Map<String, String> parameters = Map.of("LAYERS", "id1,id3");
    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder()
            .appId(1)
            .terId(1)
            .type("WMS")
            .typeId(153)
            .parameters(parameters)
            .build();

    Service service = Service.builder().id(153).blocked(false).build();
    User user = mock(User.class);
    Role role = mock(Role.class);
    UserConfiguration config = mock(UserConfiguration.class);
    Territory territory = mock(Territory.class);

    when(territory.getId()).thenReturn(1);
    when(config.getTerritory()).thenReturn(territory);
    when(config.getRole()).thenReturn(role);
    when(user.getPermissions()).thenReturn(Set.of(config));

    Cartography cartography1 = mock(Cartography.class);
    when(cartography1.getService()).thenReturn(service);
    when(cartography1.getLayers()).thenReturn(List.of("id1", "id2"));

    Cartography cartography2 = mock(Cartography.class);
    when(cartography2.getService()).thenReturn(service);
    when(cartography2.getLayers()).thenReturn(List.of("id3"));

    when(serviceRepository.findById(153)).thenReturn(Optional.of(service));
    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
    when(applicationRepository.existsById(1)).thenReturn(true);
    when(territoryRepository.existsById(1)).thenReturn(true);
    when(cartographyRepository.findByRolesAndTerritory(anyList(), eq(1)))
        .thenReturn(List.of(cartography1, cartography2));

    // When
    boolean result = validator.validate(request, "testuser");

    // Then
    assertFalse(result);
  }

  @Test
  @DisplayName("Layer validation: exact match second cartography returns true")
  void testValidate_WithLayersParam_ExactMatchSecondCartography_ReturnsTrue() {
    // Given - user has access to two cartographies, request matches second one
    Map<String, String> parameters = Map.of("LAYERS", "id3");
    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder()
            .appId(1)
            .terId(1)
            .type("WMS")
            .typeId(153)
            .parameters(parameters)
            .build();

    Service service = Service.builder().id(153).blocked(false).build();
    User user = mock(User.class);
    Role role = mock(Role.class);
    UserConfiguration config = mock(UserConfiguration.class);
    Territory territory = mock(Territory.class);

    when(territory.getId()).thenReturn(1);
    when(config.getTerritory()).thenReturn(territory);
    when(config.getRole()).thenReturn(role);
    when(user.getPermissions()).thenReturn(Set.of(config));

    Cartography cartography1 = mock(Cartography.class);
    when(cartography1.getService()).thenReturn(service);
    when(cartography1.getLayers()).thenReturn(List.of("id1", "id2"));

    Cartography cartography2 = mock(Cartography.class);
    when(cartography2.getService()).thenReturn(service);
    when(cartography2.getLayers()).thenReturn(List.of("id3"));

    when(serviceRepository.findById(153)).thenReturn(Optional.of(service));
    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
    when(applicationRepository.existsById(1)).thenReturn(true);
    when(territoryRepository.existsById(1)).thenReturn(true);
    when(cartographyRepository.findByRolesAndTerritory(anyList(), eq(1)))
        .thenReturn(List.of(cartography1, cartography2));

    // When
    boolean result = validator.validate(request, "testuser");

    // Then
    assertTrue(result);
  }

  @Test
  @DisplayName("Layer validation: superset of cartography returns false")
  void testValidate_WithLayersParam_SupersetOfCartography_ReturnsFalse() {
    // Given - request has more layers than cartography
    Map<String, String> parameters = Map.of("LAYERS", "id1,id2,id3");
    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder()
            .appId(1)
            .terId(1)
            .type("WMS")
            .typeId(153)
            .parameters(parameters)
            .build();

    Service service = Service.builder().id(153).blocked(false).build();
    User user = mock(User.class);
    Role role = mock(Role.class);
    UserConfiguration config = mock(UserConfiguration.class);
    Territory territory = mock(Territory.class);

    when(territory.getId()).thenReturn(1);
    when(config.getTerritory()).thenReturn(territory);
    when(config.getRole()).thenReturn(role);
    when(user.getPermissions()).thenReturn(Set.of(config));

    Cartography cartography = mock(Cartography.class);
    when(cartography.getService()).thenReturn(service);
    when(cartography.getLayers()).thenReturn(List.of("id1", "id2"));

    when(serviceRepository.findById(153)).thenReturn(Optional.of(service));
    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
    when(applicationRepository.existsById(1)).thenReturn(true);
    when(territoryRepository.existsById(1)).thenReturn(true);
    when(cartographyRepository.findByRolesAndTerritory(anyList(), eq(1)))
        .thenReturn(List.of(cartography));

    // When
    boolean result = validator.validate(request, "testuser");

    // Then
    assertFalse(result);
  }

  @Test
  @DisplayName("Layer validation: no matching cartography returns false")
  void testValidate_WithLayersParam_NoMatchingCartography_ReturnsFalse() {
    // Given - request has completely different layers
    Map<String, String> parameters = Map.of("LAYERS", "unknown,layer");
    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder()
            .appId(1)
            .terId(1)
            .type("WMS")
            .typeId(153)
            .parameters(parameters)
            .build();

    Service service = Service.builder().id(153).blocked(false).build();
    User user = mock(User.class);
    Role role = mock(Role.class);
    UserConfiguration config = mock(UserConfiguration.class);
    Territory territory = mock(Territory.class);

    when(territory.getId()).thenReturn(1);
    when(config.getTerritory()).thenReturn(territory);
    when(config.getRole()).thenReturn(role);
    when(user.getPermissions()).thenReturn(Set.of(config));

    Cartography cartography = mock(Cartography.class);
    when(cartography.getService()).thenReturn(service);
    when(cartography.getLayers()).thenReturn(List.of("id1", "id2"));

    when(serviceRepository.findById(153)).thenReturn(Optional.of(service));
    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
    when(applicationRepository.existsById(1)).thenReturn(true);
    when(territoryRepository.existsById(1)).thenReturn(true);
    when(cartographyRepository.findByRolesAndTerritory(anyList(), eq(1)))
        .thenReturn(List.of(cartography));

    // When
    boolean result = validator.validate(request, "testuser");

    // Then
    assertFalse(result);
  }

  @Test
  @DisplayName("Layer validation: empty LAYERS value returns true")
  void testValidate_WithLayersParam_EmptyValue_ReturnsTrue() {
    // Given - empty LAYERS parameter
    Map<String, String> parameters = Map.of("LAYERS", "");
    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder()
            .appId(1)
            .terId(1)
            .type("WMS")
            .typeId(153)
            .parameters(parameters)
            .build();

    Service service = Service.builder().id(153).blocked(false).build();
    User user = mock(User.class);
    Role role = mock(Role.class);
    UserConfiguration config = mock(UserConfiguration.class);
    Territory territory = mock(Territory.class);

    when(territory.getId()).thenReturn(1);
    when(config.getTerritory()).thenReturn(territory);
    when(config.getRole()).thenReturn(role);
    when(user.getPermissions()).thenReturn(Set.of(config));

    Cartography cartography = mock(Cartography.class);
    when(cartography.getService()).thenReturn(service);
    // No need to mock getLayers() - empty LAYERS param skips layer validation

    when(serviceRepository.findById(153)).thenReturn(Optional.of(service));
    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
    when(applicationRepository.existsById(1)).thenReturn(true);
    when(territoryRepository.existsById(1)).thenReturn(true);
    when(cartographyRepository.findByRolesAndTerritory(anyList(), eq(1)))
        .thenReturn(List.of(cartography));

    // When
    boolean result = validator.validate(request, "testuser");

    // Then
    assertTrue(result);
  }

  @Test
  @DisplayName("Layer validation: whitespace handling returns true")
  void testValidate_WithLayersParam_WhitespaceHandling_ReturnsTrue() {
    // Given - layers with extra whitespace
    Map<String, String> parameters = Map.of("LAYERS", " id1 , id2 ");
    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder()
            .appId(1)
            .terId(1)
            .type("WMS")
            .typeId(153)
            .parameters(parameters)
            .build();

    Service service = Service.builder().id(153).blocked(false).build();
    User user = mock(User.class);
    Role role = mock(Role.class);
    UserConfiguration config = mock(UserConfiguration.class);
    Territory territory = mock(Territory.class);

    when(territory.getId()).thenReturn(1);
    when(config.getTerritory()).thenReturn(territory);
    when(config.getRole()).thenReturn(role);
    when(user.getPermissions()).thenReturn(Set.of(config));

    Cartography cartography = mock(Cartography.class);
    when(cartography.getService()).thenReturn(service);
    when(cartography.getLayers()).thenReturn(List.of("id1", "id2"));

    when(serviceRepository.findById(153)).thenReturn(Optional.of(service));
    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
    when(applicationRepository.existsById(1)).thenReturn(true);
    when(territoryRepository.existsById(1)).thenReturn(true);
    when(cartographyRepository.findByRolesAndTerritory(anyList(), eq(1)))
        .thenReturn(List.of(cartography));

    // When
    boolean result = validator.validate(request, "testuser");

    // Then
    assertTrue(result);
  }

  @Test
  @DisplayName("Layer validation: without LAYERS parameter returns true")
  void testValidate_WithoutLayersParam_ServiceLevelOnly_ReturnsTrue() {
    // Given - no LAYERS parameter (service-level only)
    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder()
            .appId(1)
            .terId(1)
            .type("WMS")
            .typeId(153)
            .parameters(null)
            .build();

    Service service = Service.builder().id(153).blocked(false).build();
    User user = mock(User.class);
    Role role = mock(Role.class);
    UserConfiguration config = mock(UserConfiguration.class);
    Territory territory = mock(Territory.class);

    when(territory.getId()).thenReturn(1);
    when(config.getTerritory()).thenReturn(territory);
    when(config.getRole()).thenReturn(role);
    when(user.getPermissions()).thenReturn(Set.of(config));

    Cartography cartography = mock(Cartography.class);
    when(cartography.getService()).thenReturn(service);
    // No need to mock getLayers() - no LAYERS param means no layer validation

    when(serviceRepository.findById(153)).thenReturn(Optional.of(service));
    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
    when(applicationRepository.existsById(1)).thenReturn(true);
    when(territoryRepository.existsById(1)).thenReturn(true);
    when(cartographyRepository.findByRolesAndTerritory(anyList(), eq(1)))
        .thenReturn(List.of(cartography));

    // When
    boolean result = validator.validate(request, "testuser");

    // Then
    assertTrue(result);
  }

  @Test
  @DisplayName("Layer validation: case-insensitive key lookup returns true")
  void testValidate_WithLayersParam_CaseInsensitiveKey_ReturnsTrue() {
    // Given - lowercase 'layers' parameter key
    Map<String, String> parameters = Map.of("layers", "id1,id2");
    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder()
            .appId(1)
            .terId(1)
            .type("WMS")
            .typeId(153)
            .parameters(parameters)
            .build();

    Service service = Service.builder().id(153).blocked(false).build();
    User user = mock(User.class);
    Role role = mock(Role.class);
    UserConfiguration config = mock(UserConfiguration.class);
    Territory territory = mock(Territory.class);

    when(territory.getId()).thenReturn(1);
    when(config.getTerritory()).thenReturn(territory);
    when(config.getRole()).thenReturn(role);
    when(user.getPermissions()).thenReturn(Set.of(config));

    Cartography cartography = mock(Cartography.class);
    when(cartography.getService()).thenReturn(service);
    when(cartography.getLayers()).thenReturn(List.of("id1", "id2"));

    when(serviceRepository.findById(153)).thenReturn(Optional.of(service));
    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
    when(applicationRepository.existsById(1)).thenReturn(true);
    when(territoryRepository.existsById(1)).thenReturn(true);
    when(cartographyRepository.findByRolesAndTerritory(anyList(), eq(1)))
        .thenReturn(List.of(cartography));

    // When
    boolean result = validator.validate(request, "testuser");

    // Then
    assertTrue(result);
  }

  @Test
  @DisplayName("Layer validation: null layer in cartography is ignored")
  void testValidate_WithLayersParam_NullLayerInCartography_IgnoresNull() {
    // Given - cartography has null entry in layers list
    Map<String, String> parameters = Map.of("LAYERS", "id1,id2");
    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder()
            .appId(1)
            .terId(1)
            .type("WMS")
            .typeId(153)
            .parameters(parameters)
            .build();

    Service service = Service.builder().id(153).blocked(false).build();
    User user = mock(User.class);
    Role role = mock(Role.class);
    UserConfiguration config = mock(UserConfiguration.class);
    Territory territory = mock(Territory.class);

    when(territory.getId()).thenReturn(1);
    when(config.getTerritory()).thenReturn(territory);
    when(config.getRole()).thenReturn(role);
    when(user.getPermissions()).thenReturn(Set.of(config));

    Cartography cartography = mock(Cartography.class);
    when(cartography.getService()).thenReturn(service);
    when(cartography.getLayers()).thenReturn(Arrays.asList("id1", null, "id2"));

    when(serviceRepository.findById(153)).thenReturn(Optional.of(service));
    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
    when(applicationRepository.existsById(1)).thenReturn(true);
    when(territoryRepository.existsById(1)).thenReturn(true);
    when(cartographyRepository.findByRolesAndTerritory(anyList(), eq(1)))
        .thenReturn(List.of(cartography));

    // When
    boolean result = validator.validate(request, "testuser");

    // Then
    assertTrue(result);
  }

  @Test
  @DisplayName("GetFeatureInfo across two cartographies is allowed when neither has parameters")
  void getFeatureInfoAcrossCartographiesAllowedWithoutParameters() {
    Cartography first = cartography(1, List.of("id1"));
    Cartography second = cartography(2, List.of("id3"));
    stubAccess(List.of(first, second));

    boolean result =
        validator.validate(
            request(Map.of("REQUEST", "GetFeatureInfo", "LAYERS", "id1,id3"), null, null),
            "testuser");

    assertTrue(result);
  }

  @Test
  @DisplayName("GetMap is denied when the matched cartography has service parameters")
  void getMapDeniedWhenMatchedCartographyHasParameters() {
    Cartography cartography = cartography(7, List.of("id1", "id2"));
    when(cartographyServiceParameterRepository.existsByCartography_Id(7)).thenReturn(true);
    stubAccess(List.of(cartography));

    boolean result =
        validator.validate(
            request(Map.of("REQUEST", "GetMap", "LAYERS", "id1,id2"), null, null), "testuser");

    assertFalse(result);
  }

  @Test
  @DisplayName("Service path denies a WFS transaction")
  void servicePathDeniesTransaction() {
    stubAccess(List.of(cartography(7, List.of("roads"))));

    boolean result =
        validator.validate(
            request(
                Map.of("REQUEST", "Transaction"),
                null,
                "<wfs:Transaction xmlns:wfs=\"http://www.opengis.net/wfs\"><wfs:Delete typeName=\"roads\"/></wfs:Transaction>"),
            "testuser");

    assertFalse(result);
  }

  @Test
  @DisplayName("Service path denies a layer or feature type the caller cannot use")
  void servicePathDeniesUnownedLayerAndTypeName() {
    stubAccess(List.of(cartography(7, List.of("roads"))));

    assertFalse(
        validator.validate(
            request(Map.of("REQUEST", "GetLegendGraphic", "LAYER", "secret"), null, null),
            "testuser"));
    assertFalse(
        validator.validate(
            request(Map.of("REQUEST", "GetFeature", "typename", "secret"), null, null),
            "testuser"));
  }

  @Test
  @DisplayName("Cartography path allows GetMap for the exact layer set")
  void cartographyPathAllowsExactGetMap() {
    stubAccess(List.of(cartography(42, List.of("id1", "id2"))));

    boolean result =
        validator.validate(
            request(Map.of("REQUEST", "GetMap", "LAYERS", "id1,id2"), 42, null), "testuser");

    assertTrue(result);
  }

  @Test
  @DisplayName("Cartography path denies a GetMap subset and allows it for GetFeatureInfo")
  void cartographyPathSubsetDependsOnOperation() {
    stubAccess(List.of(cartography(42, List.of("id1", "id2"))));

    assertFalse(
        validator.validate(
            request(Map.of("REQUEST", "GetMap", "LAYERS", "id1"), 42, null), "testuser"));
    assertTrue(
        validator.validate(
            request(Map.of("REQUEST", "GetFeatureInfo", "LAYERS", "id1"), 42, null), "testuser"));
  }

  @Test
  @DisplayName("Cartography path denies a query layer outside the cartography")
  void cartographyPathDeniesForeignQueryLayer() {
    stubAccess(List.of(cartography(42, List.of("id1", "id2"))));

    boolean result =
        validator.validate(
            request(
                Map.of("REQUEST", "GetFeatureInfo", "LAYERS", "id1", "QUERY_LAYERS", "other"),
                42,
                null),
            "testuser");

    assertFalse(result);
  }

  @Test
  @DisplayName("Cartography path denies a legend layer or feature type outside the cartography")
  void cartographyPathDeniesForeignLegendLayerAndTypeName() {
    stubAccess(List.of(cartography(42, List.of("roads"))));

    assertFalse(
        validator.validate(
            request(Map.of("REQUEST", "GetLegendGraphic", "LAYER", "secret"), 42, null),
            "testuser"));
    assertFalse(
        validator.validate(
            request(Map.of("REQUEST", "GetFeature", "typename", "secret"), 42, null), "testuser"));
  }

  @Test
  @DisplayName("Cartography path allows a transaction whose type name it owns and denies the rest")
  void cartographyPathChecksTransactionTypeNames() {
    stubAccess(List.of(cartography(42, List.of("ns:roads"))));
    String inside =
        "<wfs:Transaction xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:ns=\"http://example.com\"><wfs:Update typeName=\"ns:roads\"/></wfs:Transaction>";
    String outside =
        "<wfs:Transaction xmlns:wfs=\"http://www.opengis.net/wfs\"><wfs:Delete typeName=\"ns:other\"/></wfs:Transaction>";

    assertTrue(validator.validate(request(Map.of(), 42, inside), "testuser"));
    assertFalse(validator.validate(request(Map.of(), 42, outside), "testuser"));
    assertFalse(validator.validate(request(Map.of(), 42, "<not-xml"), "testuser"));
  }

  @Test
  @DisplayName("Cartography path denies an unknown, foreign, or inaccessible cartography")
  void cartographyPathDeniesCartographyNotOnThisService() {
    Cartography otherService = cartography(9, List.of("id1"));
    Service foreign = Service.builder().id(999).blocked(false).build();
    when(otherService.getService()).thenReturn(foreign);
    stubAccess(List.of(cartography(42, List.of("id1")), otherService));

    assertFalse(
        validator.validate(
            request(Map.of("REQUEST", "GetMap", "LAYERS", "id1"), 9, null), "testuser"));
    assertFalse(
        validator.validate(
            request(Map.of("REQUEST", "GetMap", "LAYERS", "id1"), 404, null), "testuser"));
  }

  private ConfigProxyRequestDto request(
      Map<String, String> parameters, Integer cartographyId, String body) {
    return ConfigProxyRequestDto.builder()
        .appId(1)
        .terId(1)
        .type("WMS")
        .typeId(153)
        .parameters(parameters)
        .cartographyId(cartographyId)
        .requestBody(body)
        .build();
  }

  private Cartography cartography(int id, List<String> layers) {
    Cartography cartography = mock(Cartography.class);
    Service service = Service.builder().id(153).blocked(false).build();
    lenient().when(cartography.getId()).thenReturn(id);
    lenient().when(cartography.getService()).thenReturn(service);
    lenient().when(cartography.getLayers()).thenReturn(layers);
    return cartography;
  }

  private void stubAccess(List<Cartography> cartographies) {
    Service service = Service.builder().id(153).blocked(false).build();
    User user = mock(User.class);
    Role role = mock(Role.class);
    UserConfiguration config = mock(UserConfiguration.class);
    Territory territory = mock(Territory.class);
    when(territory.getId()).thenReturn(1);
    when(config.getTerritory()).thenReturn(territory);
    when(config.getRole()).thenReturn(role);
    when(user.getPermissions()).thenReturn(Set.of(config));
    when(serviceRepository.findById(153)).thenReturn(Optional.of(service));
    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
    when(applicationRepository.existsById(1)).thenReturn(true);
    when(territoryRepository.existsById(1)).thenReturn(true);
    when(cartographyRepository.findByRolesAndTerritory(anyList(), eq(1))).thenReturn(cartographies);
  }
}
