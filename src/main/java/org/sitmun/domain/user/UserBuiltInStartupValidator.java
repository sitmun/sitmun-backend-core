package org.sitmun.domain.user;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.sitmun.domain.user.position.UserPosition;
import org.sitmun.domain.user.position.UserPositionRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Verifies built-in user invariants on application startup. */
@Component
@Slf4j
public class UserBuiltInStartupValidator implements ApplicationRunner {

  private static final String ADMIN_USERNAME = "admin";
  private static final String PUBLIC_USERNAME = "public";

  private final UserRepository userRepository;
  private final UserPositionRepository userPositionRepository;

  public UserBuiltInStartupValidator(
      UserRepository userRepository, UserPositionRepository userPositionRepository) {
    this.userRepository = userRepository;
    this.userPositionRepository = userPositionRepository;
  }

  @Override
  public void run(ApplicationArguments args) {
    validateAdmin();
    validatePublic();
  }

  private void validateAdmin() {
    User admin =
        userRepository
            .findByUsername(ADMIN_USERNAME)
            .orElseThrow(() -> invariantFailed("Built-in admin user 'admin' does not exist"));
    if (!Boolean.TRUE.equals(admin.getAdministrator())) {
      throw invariantFailed("Built-in admin user must have administrator=true");
    }
    if (Boolean.TRUE.equals(admin.getBlocked())) {
      throw invariantFailed("Built-in admin user must not be blocked");
    }
    if (admin.getPassword() == null || admin.getPassword().isEmpty()) {
      throw invariantFailed("Built-in admin user must have a non-empty password");
    }
    List<UserPosition> positions = userPositionRepository.findByUser(admin);
    if (!positions.isEmpty()) {
      throw invariantFailed("Built-in admin user must not have any UserPosition rows");
    }
  }

  private void validatePublic() {
    User pub =
        userRepository
            .findByUsername(PUBLIC_USERNAME)
            .orElseThrow(() -> invariantFailed("Built-in public user 'public' does not exist"));
    if (Boolean.TRUE.equals(pub.getAdministrator())) {
      throw invariantFailed("Built-in public user must have administrator=false");
    }
    if (pub.getPassword() != null && !pub.getPassword().isEmpty()) {
      throw invariantFailed("Built-in public user must not have a password");
    }
    if (pub.getFirstName() != null
        || pub.getLastName() != null
        || pub.getEmail() != null
        || pub.getIdentificationNumber() != null
        || pub.getIdentificationType() != null) {
      throw invariantFailed("Built-in public user must not have personal information");
    }
    List<UserPosition> positions = userPositionRepository.findByUser(pub);
    if (!positions.isEmpty()) {
      throw invariantFailed("Built-in public user must not have any UserPosition rows");
    }
  }

  private IllegalStateException invariantFailed(String message) {
    log.error("Built-in user invariant violated: {}", message);
    return new IllegalStateException(message);
  }
}
