package org.sitmun.domain.application;

import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;
import org.sitmun.domain.user.configuration.UserConfigurationRepository;
import org.sitmun.infrastructure.security.core.SecurityConstants;
import org.sitmun.infrastructure.security.core.SecurityRole;
import org.springframework.stereotype.Service;

/**
 * Service for checking application configuration and generating warnings. Checks are only run when
 * the user has ROLE_ADMIN role.
 */
@Service
public class ApplicationChecksService {

  private final UserConfigurationRepository userConfigurationRepository;

  public ApplicationChecksService(UserConfigurationRepository userConfigurationRepository) {
    this.userConfigurationRepository = userConfigurationRepository;
  }

  /**
   * Get warnings for an application configuration.
   *
   * @param app the application to check
   * @return list of warning messages
   */
  public @Nullable List<String> getWarnings(Application app) {
    if (!SecurityRole.isAdmin()) {
      return null;
    }

    List<String> warnings = new ArrayList<>();

    checkPrivateApplicationWithPublicUser(app, warnings);

    return warnings;
  }

  private void checkPrivateApplicationWithPublicUser(Application app, List<String> warnings) {
    if (Boolean.TRUE.equals(app.getAppPrivate()) && hasPublicUserRole(app)) {
      warnings.add("entity.application.warning.private-application-with-public-user");
    }
  }

  /**
   * Check if the public user has any role in the application.
   *
   * @param app the application to check
   * @return true if public user has roles, false otherwise
   */
  private boolean hasPublicUserRole(Application app) {
    return userConfigurationRepository.existsByUserUsernameAndRoleIn(
        SecurityConstants.PUBLIC_PRINCIPAL, app.getAvailableRoles());
  }
}
