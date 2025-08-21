package org.sitmun.domain.user;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.domain.territory.Territory;
import org.sitmun.domain.user.configuration.UserConfiguration;
import org.sitmun.domain.user.configuration.UserConfigurationRepository;
import org.sitmun.domain.user.position.UserPosition;
import org.sitmun.domain.user.position.UserPositionRepository;
import org.sitmun.infrastructure.security.core.SecurityConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@DisplayName("UserChecksService Test")
class UserChecksServiceTest {

  @MockitoBean private UserConfigurationRepository userConfigurationRepository;

  @MockitoBean private UserPositionRepository userPositionRepository;

  @Autowired private UserChecksService userChecksService;

  @Test
  @WithMockUser(roles = {"USER", "PUBLIC", "PROXY"})
  @DisplayName("When user is not admin, getWarnings returns null")
  void getWarningsWhenUserIsNotAdminReturnsNull() {
    // Arrange
    User user = mock(User.class);

    // Act
    List<String> warnings = userChecksService.getWarnings(user);

    // Assert
    assertNull(warnings);
    verifyNoInteractions(userConfigurationRepository, userPositionRepository);
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName(
      "When user is admin, getWarnings returns a warning if any position has missing details")
  void getWarningsWhenPositionDetailsAreMissingAddsWarning() {
    // Arrange

    User user = mock(User.class);
    when(user.getUsername()).thenReturn("testUser");

    Territory territory = mock(Territory.class);
    UserConfiguration userConfig = mock(UserConfiguration.class);
    when(userConfig.getTerritory()).thenReturn(territory);

    UserPosition position = mock(UserPosition.class);
    when(position.getUser()).thenReturn(user);
    when(position.getTerritory()).thenReturn(territory);
    when(position.getName()).thenReturn("");
    when(position.getType()).thenReturn("Type");
    when(position.getOrganization()).thenReturn("Org");
    when(position.getEmail()).thenReturn("email@example.com");

    when(userConfigurationRepository.findByUser(user)).thenReturn(List.of(userConfig));
    when(userPositionRepository.findByUser(user)).thenReturn(List.of(position));

    // Act
    List<String> warnings = userChecksService.getWarnings(user);

    // Assert
    assertNotNull(warnings);
    System.out.println("Warnings: " + warnings);
    assertEquals(1, warnings.size());
    assertTrue(warnings.contains("entity.user.warning.position-without-details"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName(
      "When user is admin, getWarnings returns a warning if the user has no positions for a territory in an user configuration")
  void getWarningsWhenRoleWithoutPositionAddsWarning() {
    // Arrange
    User user = mock(User.class);
    when(user.getUsername()).thenReturn("testUser");

    Territory territory = mock(Territory.class);
    UserConfiguration userConfig = mock(UserConfiguration.class);
    when(userConfig.getTerritory()).thenReturn(territory);

    when(userConfigurationRepository.findByUser(user)).thenReturn(List.of(userConfig));
    when(userPositionRepository.findByUser(user)).thenReturn(new ArrayList<>());

    // Act
    List<String> warnings = userChecksService.getWarnings(user);

    // Assert
    assertNotNull(warnings);
    assertEquals(1, warnings.size());
    assertTrue(warnings.contains("entity.user.warning.role-without-position"));
    verify(userPositionRepository, times(1)).save(any(UserPosition.class));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName(
      "When user is admin, getWarnings returns a warning if the user has no user configuration")
  void getWarningsWhenNoUserConfigurationPresentAddsWarning() {
    // Arrange
    User user = mock(User.class);
    when(userConfigurationRepository.findByUser(user)).thenReturn(new ArrayList<>());

    // Act
    List<String> warnings = userChecksService.getWarnings(user);

    // Assert
    assertNotNull(warnings);
    assertEquals(1, warnings.size());
    assertTrue(warnings.contains("entity.user.warning.no-roles"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("When user is public user, position checks are skipped")
  void getWarningsWhenPublicUserSkipsPositionChecks() {
    // Arrange
    User user = mock(User.class);
    when(user.getUsername()).thenReturn(SecurityConstants.PUBLIC_PRINCIPAL);

    when(userConfigurationRepository.findByUser(user)).thenReturn(new ArrayList<>());

    // Act
    List<String> warnings = userChecksService.getWarnings(user);

    // Assert
    assertNotNull(warnings);
    assertEquals(1, warnings.size());
    assertTrue(warnings.contains("entity.user.warning.no-roles"));
    verify(userPositionRepository, never()).findByUser(user);
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("When user is public user, enforce position checks are skipped")
  void getWarningsWhenPublicUserSkipsEnforcePositionChecks() {
    // Arrange
    User user = mock(User.class);
    when(user.getUsername()).thenReturn(SecurityConstants.PUBLIC_PRINCIPAL);

    Territory territory = mock(Territory.class);
    UserConfiguration userConfig = mock(UserConfiguration.class);
    when(userConfig.getTerritory()).thenReturn(territory);

    when(userConfigurationRepository.findByUser(user)).thenReturn(List.of(userConfig));
    when(userPositionRepository.findByUser(user)).thenReturn(new ArrayList<>());

    // Act
    List<String> warnings = userChecksService.getWarnings(user);

    // Assert
    assertNotNull(warnings);
    assertEquals(0, warnings.size()); // Public user has configuration, so no warnings
    verify(userPositionRepository, never()).save(any(UserPosition.class));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("When position has blank organization, warning is added")
  void getWarningsWhenPositionHasBlankOrganizationAddsWarning() {
    getWarningsWhenPositionHasIssues("Name", "Type", "    ", "email@example.com");
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("When position has blank email, warning is added")
  void getWarningsWhenPositionHasBlankEmailAddsWarning() {
    getWarningsWhenPositionHasIssues("Name", "Type", "Org", null);
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("When position has blank type, warning is added")
  void getWarningsWhenPositionHasBlankTypeAddsWarning() {
    getWarningsWhenPositionHasIssues("Name", "", "Org", "email@example.com");
  }

  private void getWarningsWhenPositionHasIssues(
      String name, String type, String organization, String email) {
    // Arrange
    User user = mock(User.class);
    when(user.getUsername()).thenReturn("testUser");

    Territory territory = mock(Territory.class);
    UserConfiguration userConfig = mock(UserConfiguration.class);
    when(userConfig.getTerritory()).thenReturn(territory);

    UserPosition position = mock(UserPosition.class);
    when(position.getUser()).thenReturn(user);
    when(position.getTerritory()).thenReturn(territory);
    when(position.getName()).thenReturn(name);
    when(position.getType()).thenReturn(type);
    when(position.getOrganization()).thenReturn(organization);
    when(position.getEmail()).thenReturn(email);

    when(userConfigurationRepository.findByUser(user)).thenReturn(List.of(userConfig));
    when(userPositionRepository.findByUser(user)).thenReturn(List.of(position));
    when(position.getOrganization()).thenReturn("   ");
    when(position.getEmail()).thenReturn("email@example.com");

    when(userConfigurationRepository.findByUser(user)).thenReturn(List.of(userConfig));
    when(userPositionRepository.findByUser(user)).thenReturn(List.of(position));

    // Act
    List<String> warnings = userChecksService.getWarnings(user);

    // Assert
    assertNotNull(warnings);
    assertEquals(1, warnings.size());
    assertTrue(warnings.contains("entity.user.warning.position-without-details"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("When user has multiple issues, multiple warnings are returned")
  void getWarningsWhenMultipleIssuesReturnsMultipleWarnings() {
    // Arrange
    User user = mock(User.class);
    when(user.getUsername()).thenReturn("testUser");

    Territory territory1 = mock(Territory.class);
    Territory territory2 = mock(Territory.class);
    UserConfiguration userConfig1 = mock(UserConfiguration.class);
    UserConfiguration userConfig2 = mock(UserConfiguration.class);
    when(userConfig1.getTerritory()).thenReturn(territory1);
    when(userConfig2.getTerritory()).thenReturn(territory2);

    UserPosition position = mock(UserPosition.class);
    when(position.getUser()).thenReturn(user);
    when(position.getTerritory()).thenReturn(territory1);
    when(position.getName()).thenReturn("");
    when(position.getType()).thenReturn("Type");
    when(position.getOrganization()).thenReturn("Org");
    when(position.getEmail()).thenReturn("email@example.com");

    when(userConfigurationRepository.findByUser(user))
        .thenReturn(List.of(userConfig1, userConfig2));
    when(userPositionRepository.findByUser(user)).thenReturn(List.of(position));

    // Act
    List<String> warnings = userChecksService.getWarnings(user);

    // Assert
    assertNotNull(warnings);
    assertEquals(2, warnings.size());
    assertTrue(warnings.contains("entity.user.warning.position-without-details"));
    assertTrue(warnings.contains("entity.user.warning.role-without-position"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("When user has valid positions, no position warnings are returned")
  void getWarningsWhenValidPositionsNoPositionWarnings() {
    // Arrange
    User user = mock(User.class);
    when(user.getUsername()).thenReturn("testUser");

    Territory territory = mock(Territory.class);
    UserConfiguration userConfig = mock(UserConfiguration.class);
    when(userConfig.getTerritory()).thenReturn(territory);

    UserPosition position = mock(UserPosition.class);
    when(position.getUser()).thenReturn(user);
    when(position.getTerritory()).thenReturn(territory);
    when(position.getName()).thenReturn("Valid Name");
    when(position.getType()).thenReturn("Valid Type");
    when(position.getOrganization()).thenReturn("Valid Org");
    when(position.getEmail()).thenReturn("valid@example.com");

    when(userConfigurationRepository.findByUser(user)).thenReturn(List.of(userConfig));
    when(userPositionRepository.findByUser(user)).thenReturn(List.of(position));

    // Act
    List<String> warnings = userChecksService.getWarnings(user);

    // Assert
    assertNotNull(warnings);
    assertEquals(0, warnings.size());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("When user has positions for all territories, no role-without-position warning")
  void getWarningsWhenPositionsForAllTerritoriesNoRoleWarning() {
    // Arrange
    User user = mock(User.class);
    when(user.getUsername()).thenReturn("testUser");

    Territory territory = mock(Territory.class);
    UserConfiguration userConfig = mock(UserConfiguration.class);
    when(userConfig.getTerritory()).thenReturn(territory);

    UserPosition position = mock(UserPosition.class);
    when(position.getUser()).thenReturn(user);
    when(position.getTerritory()).thenReturn(territory);
    when(position.getName()).thenReturn("Valid Name");
    when(position.getType()).thenReturn("Valid Type");
    when(position.getOrganization()).thenReturn("Valid Org");
    when(position.getEmail()).thenReturn("valid@example.com");

    when(userConfigurationRepository.findByUser(user)).thenReturn(List.of(userConfig));
    when(userPositionRepository.findByUser(user)).thenReturn(List.of(position));

    // Act
    List<String> warnings = userChecksService.getWarnings(user);

    // Assert
    assertNotNull(warnings);
    assertEquals(0, warnings.size());
    verify(userPositionRepository, never()).save(any(UserPosition.class));
  }
}
