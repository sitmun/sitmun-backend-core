package org.sitmun.domain.user.configuration;

import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.sitmun.domain.user.position.UserPositionBusinessLogic;
import org.springframework.data.rest.core.annotation.HandleAfterCreate;
import org.springframework.data.rest.core.annotation.HandleAfterSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.stereotype.Component;

/**
 * Event handler for UserConfiguration entities. Automatically creates a UserPosition when a
 * UserConfiguration is inserted or updated and the tuple (user, territory) doesn't exist in
 * UserPosition.
 */
@Component
@RepositoryEventHandler
@Slf4j
public class UserConfigurationEventHandler {

  private final UserPositionBusinessLogic userPositionBusinessLogic;

  public UserConfigurationEventHandler(UserPositionBusinessLogic userPositionBusinessLogic) {
    this.userPositionBusinessLogic = userPositionBusinessLogic;
  }

  /**
   * Handle UserConfiguration creation. Creates a UserPosition if the tuple (user, territory)
   * doesn't exist.
   *
   * @param userConfiguration the created user configuration
   */
  @HandleAfterCreate
  public void handleUserConfigurationCreate(@NotNull UserConfiguration userConfiguration) {
    userPositionBusinessLogic.createUserPositionIfNotExists(userConfiguration);
  }

  /**
   * Handle UserConfiguration update. Creates a UserPosition if the tuple (user, territory) doesn't
   * exist.
   *
   * @param userConfiguration the updated user configuration
   */
  @HandleAfterSave
  public void handleUserConfigurationUpdate(@NotNull UserConfiguration userConfiguration) {
    userPositionBusinessLogic.createUserPositionIfNotExists(userConfiguration);
  }
}
