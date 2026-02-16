package org.sitmun.domain.user;

import jakarta.validation.constraints.NotNull;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.HandleBeforeDelete;
import org.springframework.data.rest.core.annotation.HandleBeforeSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RepositoryEventHandler
public class UserEventHandler {

  private static final String BUILT_IN_ADMIN_USERNAME = "admin";
  private static final String BUILT_IN_PUBLIC_USERNAME = "public";

  private final PasswordEncoder passwordEncoder;
  private final UserRepository userRepository;

  public UserEventHandler(PasswordEncoder passwordEncoder, UserRepository userRepository) {
    this.passwordEncoder = passwordEncoder;
    this.userRepository = userRepository;
  }

  /**
   * Check if a user is the built-in admin user.
   *
   * @param user the user to check
   * @return true if the user is the built-in admin
   */
  private boolean isBuiltInAdmin(User user) {
    return user != null && BUILT_IN_ADMIN_USERNAME.equals(user.getUsername());
  }

  /**
   * Check if a user is the built-in public user.
   *
   * @param user the user to check
   * @return true if the user is the built-in public user
   */
  private boolean isBuiltInPublic(User user) {
    return user != null && BUILT_IN_PUBLIC_USERNAME.equals(user.getUsername());
  }

  /**
   * If the password is null or empty, this method sets the password null, and otherwise encodes
   * its.
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
   * If the password is null, this method keeps the last value if exists, if the password is empty,
   * this method clears it, and otherwise the password is encoded. Also enforces built-in admin user
   * invariants.
   *
   * @param user the new user after being loaded from database and updated with PUT data
   */
  @HandleBeforeSave
  public void handleUserUpdate(@NotNull User user) {
    // Protect built-in admin user invariants
    if (user.getId() != null) {
      User originalUser = userRepository.findById(user.getId()).orElse(null);
      if (originalUser != null && isBuiltInAdmin(originalUser)) {
        // Prevent username change
        if (!BUILT_IN_ADMIN_USERNAME.equals(user.getUsername())) {
          throw new IllegalArgumentException("Cannot change username of built-in admin user");
        }
        // Prevent blocking
        if (Boolean.TRUE.equals(user.getBlocked())) {
          throw new IllegalArgumentException("Cannot block built-in admin user");
        }
        // Prevent demotion from administrator
        if (!Boolean.TRUE.equals(user.getAdministrator())) {
          throw new IllegalArgumentException(
              "Cannot remove administrator privilege from built-in admin user");
        }
      }
      // Protect built-in public user invariants
      if (originalUser != null && isBuiltInPublic(originalUser)) {
        // Prevent username change
        if (!BUILT_IN_PUBLIC_USERNAME.equals(user.getUsername())) {
          throw new IllegalArgumentException("Cannot change username of built-in public user");
        }
        // Prevent promotion to administrator
        if (Boolean.TRUE.equals(user.getAdministrator())) {
          throw new IllegalArgumentException(
              "Cannot grant administrator privilege to built-in public user");
        }
        // Public user should not have personal information
        if (user.getFirstName() != null
            || user.getLastName() != null
            || user.getEmail() != null
            || user.getIdentificationNumber() != null
            || user.getIdentificationType() != null) {
          throw new IllegalArgumentException(
              "Cannot set personal information for built-in public user");
        }
        // Public user should not have a password
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
          throw new IllegalArgumentException("Cannot set password for built-in public user");
        }
      }
    }

    // Handle password encoding
    if (user.getPassword() == null) {
      user.setPassword(user.getStoredPassword());
    } else if (user.getPassword().isEmpty()) {
      user.setPassword(null);
    } else {
      user.setPassword(passwordEncoder.encode(user.getPassword()));
    }
  }

  /**
   * Prevent deletion of the built-in admin and public users.
   *
   * @param user the user to be deleted
   */
  @HandleBeforeDelete
  public void handleUserDelete(@NotNull User user) {
    if (isBuiltInAdmin(user)) {
      throw new IllegalArgumentException("Cannot delete built-in admin user");
    }
    if (isBuiltInPublic(user)) {
      throw new IllegalArgumentException("Cannot delete built-in public user");
    }
  }
}
