package org.sitmun.domain.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.sitmun.test.Fixtures.createRoles;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.domain.user.configuration.UserConfigurationRepository;
import org.sitmun.infrastructure.security.core.SecurityConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@DisplayName("Application Checks Service Tests")
class ApplicationChecksServiceTest {

  @MockitoBean
  private UserConfigurationRepository userConfigurationRepository;

  @Autowired private ApplicationChecksService applicationChecksService;

  @Test
  @DisplayName("Should return warning when private application has PUBLIC user")
  @WithMockUser(roles = "ADMIN")
  void shouldReturnWarningWhenPrivateAppHasPublicUserRoles() {
    // Given
    Application app =
        Application.builder()
            .id(1)
            .name("Private App")
            .appPrivate(true)
            .availableRoles(createRoles(1, 2))
            .build();

    when(userConfigurationRepository.existsByUserUsernameAndRoleIn(
            eq(SecurityConstants.PUBLIC_PRINCIPAL), any(Set.class)))
        .thenReturn(true);

    // When
    List<String> warnings = applicationChecksService.getWarnings(app);

    // Then
    assertThat(warnings).hasSize(1);
    assertThat(warnings.get(0))
        .contains("entity.application.warning.private-application-with-public-user");
  }

  @Test
  @DisplayName("Should return no warnings when private application has no PUBLIC user")
  @WithMockUser(roles = "ADMIN")
  void shouldReturnNoWarningsWhenPrivateAppHasNoPublicUserRoles() {
    // Given
    Application app =
        Application.builder()
            .id(1)
            .name("Private App")
            .appPrivate(true)
            .availableRoles(createRoles(1, 2))
            .build();

    when(userConfigurationRepository.existsByUserUsernameAndRoleIn(
            eq(SecurityConstants.PUBLIC_PRINCIPAL), any(Set.class)))
        .thenReturn(false);

    // When
    List<String> warnings = applicationChecksService.getWarnings(app);

    // Then
    assertThat(warnings).isEmpty();
  }

  @Test
  @DisplayName("Should return no warnings in public applications")
  @WithMockUser(roles = "ADMIN")
  void shouldReturnNoWarningsWhenPublicAppHasPublicUserRoles() {
    // Given
    Application app =
        Application.builder()
            .id(1)
            .name("Public App")
            .appPrivate(false)
            .availableRoles(createRoles(1, 2))
            .build();

    // When
    List<String> warnings = applicationChecksService.getWarnings(app);

    // Then
    assertThat(warnings).isEmpty();
  }

  @Test
  @DisplayName("Should return no warnings when user is not admin")
  @WithMockUser(roles = "USER")
  void shouldReturnNullWhenUserIsNotAdmin() {
    // Given
    Application app =
        Application.builder()
            .id(1)
            .name("Private App")
            .appPrivate(true)
            .availableRoles(createRoles(1, 2))
            .build();

    when(userConfigurationRepository.existsByUserUsernameAndRoleIn(
            eq(SecurityConstants.PUBLIC_PRINCIPAL), any(Set.class)))
        .thenReturn(true);

    // When
    List<String> warnings = applicationChecksService.getWarnings(app);

    // Then
    assertThat(warnings).isNull();
  }
}
