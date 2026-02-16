package org.sitmun.domain.user;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for checking application configuration and generating warnings. Checks are only run when
 * the user has an ROLE_ADMIN role.
 */
@Service
@Slf4j
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
    try {
      List<String> warnings = new ArrayList<>();
      checkUserConfiguration(user, warnings);
      checkPositionsForUserRole(user, warnings);
      checkRoleWithoutPosition(user, warnings);
      return warnings;
    } catch (Exception e) {
      log.warn("Could not compute user warnings", e);
      return List.of();
    }
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

  /** Read-only check: adds warning if user has roles (configurations) without a position. */
  private void checkRoleWithoutPosition(User user, List<String> warnings) {
    if (SecurityConstants.isPublicPrincipal(user.getUsername())) {
      return;
    }
    List<Territory> territories =
        userConfigurationRepository.findByUser(user).stream()
            .map(UserConfiguration::getTerritory)
            .distinct()
            .toList();
    List<UserPosition> positions = userPositionRepository.findByUser(user);
    boolean missing = false;
    for (Territory territory : territories) {
      if (positions.stream().noneMatch(it -> it.getTerritory().equals(territory))) {
        missing = true;
        break;
      }
    }
    if (missing) {
      warnings.add("entity.user.warning.role-without-position");
    }
  }

  /**
   * Creates a UserPosition for each (user, territory) from the user's configurations when missing.
   * Call from a write context (e.g. after saving UserConfiguration), not from projection
   * serialization. The UI does not allow adding positions for roles; this enforces the invariant.
   */
  @Transactional
  public void enforcePositionsForUser(User user) {
    if (user == null || SecurityConstants.isPublicPrincipal(user.getUsername())) {
      return;
    }
    List<Territory> territories =
        userConfigurationRepository.findByUser(user).stream()
            .map(UserConfiguration::getTerritory)
            .distinct()
            .toList();
    List<UserPosition> positions = userPositionRepository.findByUser(user);
    for (Territory territory : territories) {
      if (positions.stream().noneMatch(it -> it.getTerritory().equals(territory))) {
        UserPosition newPosition = UserPosition.builder().user(user).territory(territory).build();
        userPositionRepository.save(newPosition);
        positions.add(newPosition);
      }
    }
  }
}
