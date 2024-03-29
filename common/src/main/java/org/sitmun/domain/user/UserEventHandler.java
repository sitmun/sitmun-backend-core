package org.sitmun.domain.user;

import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.HandleBeforeSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.validation.constraints.NotNull;

@Component
@RepositoryEventHandler
public class UserEventHandler {

  private final PasswordEncoder passwordEncoder;

  public UserEventHandler(PasswordEncoder passwordEncoder) {
    Assert.notNull(passwordEncoder, "PasswordEncoder must be set");
    this.passwordEncoder = passwordEncoder;
  }

  /**
   * If the password is null or empty, this method sets the password null, and otherwise encodes its.
   *
   * @param user the new user
   */
  @HandleBeforeCreate
  public void handleUserCreate(@NotNull User user) {
    if (user.getPassword() != null) {
      if (user.getPassword().isEmpty()) {
        user.setPassword(null);
      } else {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
      }
    }
  }

  /**
   * If the password is null, this method keeps the last value if exists,
   * if the password is empty, this method clears it,
   * and otherwise the password is encoded.
   *
   * @param user the new user after being loaded from database and updated with PUT data
   */
  @HandleBeforeSave
  public void handleUserUpdate(@NotNull User user) {
    if (user.getPassword() == null) {
      user.setPassword(user.getStoredPassword());
    } else if (user.getPassword().isEmpty()) {
      user.setPassword(null);
    } else {
      user.setPassword(passwordEncoder.encode(user.getPassword()));
    }
  }
}