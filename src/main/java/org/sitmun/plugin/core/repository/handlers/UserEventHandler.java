package org.sitmun.plugin.core.repository.handlers;

import org.sitmun.plugin.core.domain.User;
import org.sitmun.plugin.core.repository.UserRepository;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.HandleBeforeSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import javax.validation.constraints.NotNull;
import java.util.Objects;

@RepositoryEventHandler
public class UserEventHandler {

  private final BCryptPasswordEncoder passwordEncoder;

  private final UserRepository userRepository;

  public UserEventHandler(BCryptPasswordEncoder passwordEncoder, UserRepository userRepository) {
    this.passwordEncoder = passwordEncoder;
    this.userRepository = userRepository;
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
   * and otherwise if it is different from the last value, encodes its,
   * otherwise, the password is encoded.
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
      if (!Objects.equals(user.getPassword(), user.getStoredPassword())) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
      }
    }
  }
}