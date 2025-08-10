package org.sitmun.domain.user;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.sitmun.domain.territory.Territory;
import org.sitmun.domain.user.configuration.UserConfiguration;
import org.sitmun.domain.user.configuration.UserConfigurationRepository;
import org.sitmun.domain.user.position.UserPosition;
import org.sitmun.domain.user.position.UserPositionRepository;
import org.sitmun.infrastructure.security.core.SecurityConstants;
import org.sitmun.infrastructure.security.core.SecurityRole;
import org.springframework.stereotype.Service;

/**
 * Service for checking application configuration and generating warnings. Checks are only run when
 * the user has an ROLE_ADMIN role.
 */
@Service
public class UserChecksService {

  private final UserConfigurationRepository userConfigurationRepository;
  private final UserPositionRepository userPositionRepository;

  public UserChecksService(
      UserConfigurationRepository userConfigurationRepository,
      UserPositionRepository userPositionRepository) {
    this.userPositionRepository = userPositionRepository;
    this.userConfigurationRepository = userConfigurationRepository;
  }

  /**
   * Get warnings for a user.
   *
   * @param user the user to check
   * @return list of warning messages
   */
  public @Nullable List<String> getWarnings(User user) {
    if (!SecurityRole.isAdmin()) {
      return null;
    }

    List<String> warnings = new ArrayList<>();

    checkUserConfiguration(user, warnings);
    checkPositionsForUserRole(user, warnings);
    enforcePositionForUserRole(user, warnings);

    return warnings;
  }

  private void checkUserConfiguration(User user, List<String> warnings) {
    List<UserConfiguration> configurations = userConfigurationRepository.findByUser(user);
    boolean flagWarnings = configurations.isEmpty();
    if (flagWarnings) {
      warnings.add("entity.user.warning.no-roles");
    }
  }

  private void checkPositionsForUserRole(User user, List<String> warnings) {
    if (SecurityConstants.isPublicPrincipal(user.getUsername())) {
      return;
    }
    List<UserPosition> positions = userPositionRepository.findByUser(user);
    boolean flagWarnings = false;
    for (UserPosition position : positions) {
      flagWarnings =
          StringUtils.isBlank(position.getName())
              || StringUtils.isBlank(position.getOrganization())
              || StringUtils.isBlank(position.getEmail())
              || StringUtils.isBlank(position.getType());
      if (flagWarnings) {
        break;
      }
    }
    if (flagWarnings) {
      warnings.add("entity.user.warning.position-without-details");
    }
  }

  private void enforcePositionForUserRole(User user, List<String> warnings) {
    if (SecurityConstants.isPublicPrincipal(user.getUsername())) {
      return;
    }
    List<Territory> territories =
        userConfigurationRepository.findByUser(user).stream()
            .map(UserConfiguration::getTerritory)
            .distinct()
            .toList();
    List<UserPosition> positions = userPositionRepository.findByUser(user);
    boolean flagWarnings = false;
    for (Territory territory : territories) {
      if (positions.stream().anyMatch(it -> it.getTerritory().equals(territory))) {
        continue;
      }
      flagWarnings = true;
      UserPosition newPosition = UserPosition.builder().user(user).territory(territory).build();
      userPositionRepository.save(newPosition);
    }
    if (flagWarnings) {
      warnings.add("entity.user.warning.role-without-position");
    }
  }
}
