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

  private static User mockCheckableUser(String username, boolean passwordSet) {
    User user = mock(User.class);
    when(user.getUsername()).thenReturn(username);
    when(user.getPasswordSet()).thenReturn(passwordSet);
    return user;
  }

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
      "When user is admin, getWarnings returns a warning if any position is missing name or organization")
  void getWarningsWhenPositionDetailsAreMissingAddsWarning() {
    // Arrange

    User user = mockCheckableUser("testUser", true);

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
    assertEquals(1, warnings.size());
    assertTrue(warnings.contains("entity.user.warning.position-without-details"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName(
      "When user is admin, getWarnings returns a warning if the user has no positions for a territory in an user configuration")
  void getWarningsWhenRoleWithoutPositionAddsWarning() {
    // Arrange
    User user = mockCheckableUser("testUser", true);

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
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName(
      "When user is admin, getWarnings returns a warning if the user has no user configuration")
  void getWarningsWhenNoUserConfigurationPresentAddsWarning() {
    // Arrange
    User user = mockCheckableUser("testUser", true);
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
  @DisplayName("When user has no password, no-password warning is added")
  void getWarningsWhenNoPasswordAddsWarning() {
    User user = mockCheckableUser("testUser", false);

    when(userConfigurationRepository.findByUser(user)).thenReturn(List.of());
    when(userPositionRepository.findByUser(user)).thenReturn(List.of());

    List<String> warnings = userChecksService.getWarnings(user);

    assertNotNull(warnings);
    assertTrue(warnings.contains("entity.user.warning.no-password"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("When built-in admin user has no passwordSet, no-password warning is skipped")
  void getWarningsWhenBuiltInAdminSkipsNoPasswordWarning() {
    User user = mockCheckableUser("admin", false);

    when(userConfigurationRepository.findByUser(user)).thenReturn(List.of());
    when(userPositionRepository.findByUser(user)).thenReturn(List.of());

    List<String> warnings = userChecksService.getWarnings(user);

    assertNotNull(warnings);
    assertTrue(warnings.isEmpty());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("When built-in admin user has no roles, no-roles warning is skipped")
  void getWarningsWhenBuiltInAdminSkipsNoRolesWarning() {
    User user = mockCheckableUser("admin", true);

    when(userConfigurationRepository.findByUser(user)).thenReturn(List.of());
    when(userPositionRepository.findByUser(user)).thenReturn(List.of());

    List<String> warnings = userChecksService.getWarnings(user);

    assertNotNull(warnings);
    assertFalse(warnings.contains("entity.user.warning.no-roles"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("When built-in admin has roles without positions, position warnings are skipped")
  void getWarningsWhenBuiltInAdminSkipsRoleWithoutPositionWarning() {
    User user = mockCheckableUser("admin", true);

    Territory territory = mock(Territory.class);
    UserConfiguration userConfig = mock(UserConfiguration.class);
    when(userConfig.getTerritory()).thenReturn(territory);

    when(userConfigurationRepository.findByUser(user)).thenReturn(List.of(userConfig));
    when(userPositionRepository.findByUser(user)).thenReturn(new ArrayList<>());

    List<String> warnings = userChecksService.getWarnings(user);

    assertNotNull(warnings);
    assertTrue(warnings.isEmpty());
    verify(userPositionRepository, never()).save(any(UserPosition.class));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("When user is public user, role and position checks are skipped")
  void getWarningsWhenPublicUserSkipsPositionChecks() {
    // Arrange
    User user = mock(User.class);
    when(user.getUsername()).thenReturn(SecurityConstants.PUBLIC_PRINCIPAL);

    when(userConfigurationRepository.findByUser(user)).thenReturn(new ArrayList<>());

    // Act
    List<String> warnings = userChecksService.getWarnings(user);

    // Assert
    assertNotNull(warnings);
    assertTrue(warnings.isEmpty());
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
    assertPositionWarningWhen("Name", "   ");
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("When position has blank name, warning is added")
  void getWarningsWhenPositionHasBlankNameAddsWarning() {
    assertPositionWarningWhen("", "Org");
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("When position has blank email only, no position-details warning")
  void getWarningsWhenPositionHasBlankEmailOnlyNoPositionWarning() {
    assertNoPositionDetailsWarningWhen("Name", "Org", null, "");
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("When position has blank type only, no position-details warning")
  void getWarningsWhenPositionHasBlankTypeOnlyNoPositionWarning() {
    assertNoPositionDetailsWarningWhen("Name", "Org", "email@example.com", null);
  }

  private void assertPositionWarningWhen(String name, String organization) {
    User user = mockCheckableUser("testUser", true);

    Territory territory = mock(Territory.class);
    UserConfiguration userConfig = mock(UserConfiguration.class);
    when(userConfig.getTerritory()).thenReturn(territory);

    UserPosition position = mock(UserPosition.class);
    when(position.getTerritory()).thenReturn(territory);
    when(position.getName()).thenReturn(name);
    when(position.getOrganization()).thenReturn(organization);

    when(userConfigurationRepository.findByUser(user)).thenReturn(List.of(userConfig));
    when(userPositionRepository.findByUser(user)).thenReturn(List.of(position));

    List<String> warnings = userChecksService.getWarnings(user);

    assertNotNull(warnings);
    assertEquals(1, warnings.size());
    assertTrue(warnings.contains("entity.user.warning.position-without-details"));
  }

  private void assertNoPositionDetailsWarningWhen(
      String name, String organization, String email, String type) {
    User user = mockCheckableUser("testUser", true);

    Territory territory = mock(Territory.class);
    UserConfiguration userConfig = mock(UserConfiguration.class);
    when(userConfig.getTerritory()).thenReturn(territory);

    UserPosition position = mock(UserPosition.class);
    when(position.getTerritory()).thenReturn(territory);
    when(position.getName()).thenReturn(name);
    when(position.getOrganization()).thenReturn(organization);
    when(position.getEmail()).thenReturn(email);
    when(position.getType()).thenReturn(type);

    when(userConfigurationRepository.findByUser(user)).thenReturn(List.of(userConfig));
    when(userPositionRepository.findByUser(user)).thenReturn(List.of(position));

    List<String> warnings = userChecksService.getWarnings(user);

    assertNotNull(warnings);
    assertFalse(warnings.contains("entity.user.warning.position-without-details"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("When user has multiple issues, multiple warnings are returned")
  void getWarningsWhenMultipleIssuesReturnsMultipleWarnings() {
    // Arrange
    User user = mockCheckableUser("testUser", true);

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
    User user = mockCheckableUser("testUser", true);

    Territory territory = mock(Territory.class);
    UserConfiguration userConfig = mock(UserConfiguration.class);
    when(userConfig.getTerritory()).thenReturn(territory);

    UserPosition position = mock(UserPosition.class);
    when(position.getUser()).thenReturn(user);
    when(position.getTerritory()).thenReturn(territory);
    when(position.getName()).thenReturn("Valid Name");
    when(position.getOrganization()).thenReturn("Valid Org");

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
  @DisplayName("When position has name and organization only, no position-details warning")
  void getWarningsWhenNameAndOrganizationPresentNoPositionWarningDespiteBlankEmailAndType() {
    assertNoPositionDetailsWarningWhen("Valid Name", "Valid Org", null, null);
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("When user has positions for all territories, no role-without-position warning")
  void getWarningsWhenPositionsForAllTerritoriesNoRoleWarning() {
    // Arrange
    User user = mockCheckableUser("testUser", true);

    Territory territory = mock(Territory.class);
    UserConfiguration userConfig = mock(UserConfiguration.class);
    when(userConfig.getTerritory()).thenReturn(territory);

    UserPosition position = mock(UserPosition.class);
    when(position.getUser()).thenReturn(user);
    when(position.getTerritory()).thenReturn(territory);
    when(position.getName()).thenReturn("Valid Name");
    when(position.getOrganization()).thenReturn("Valid Org");

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
