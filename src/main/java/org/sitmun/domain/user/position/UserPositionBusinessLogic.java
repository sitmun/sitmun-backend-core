package org.sitmun.domain.user.position;

import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.sitmun.domain.user.User;
import org.sitmun.domain.user.configuration.UserConfiguration;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic component for UserPosition operations. Handles the creation of UserPosition
 * entities when UserConfiguration is created or updated.
 */
@Component
@Slf4j
public class UserPositionBusinessLogic {

  private final UserPositionRepository userPositionRepository;

  public UserPositionBusinessLogic(UserPositionRepository userPositionRepository) {
    this.userPositionRepository = userPositionRepository;
  }

  /**
   * Creates a UserPosition if the tuple (user, territory) doesn't exist.
   *
   * @param userConfiguration the user configuration
   */
  @Transactional(propagation = Propagation.REQUIRED)
  public void createUserPositionIfNotExists(@NotNull UserConfiguration userConfiguration) {
    User user = userConfiguration.getUser();
    var territory = userConfiguration.getTerritory();

    if (user == null || territory == null) {
      log.warn("Cannot create UserPosition: user or territory is null");
      return;
    }

    // Check if UserPosition already exists for this user-territory tuple
    var existingPositions = userPositionRepository.findByUserAndTerritory(user, territory);

    if (existingPositions.isEmpty()) {
      // Create new UserPosition
      UserPosition newPosition = UserPosition.builder().user(user).territory(territory).build();

      userPositionRepository.save(newPosition);
      log.info(
          "Created new UserPosition for user {} in territory {}",
          user.getUsername(),
          territory.getName());
    } else {
      log.debug(
          "UserPosition already exists for user {} in territory {}",
          user.getUsername(),
          territory.getName());
    }
  }
}
