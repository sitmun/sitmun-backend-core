package org.sitmun.domain.user;

import java.time.Clock;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.sitmun.domain.user.position.UserPosition;
import org.sitmun.domain.user.position.UserPositionRepository;
import org.sitmun.infrastructure.security.core.SecurityConstants;
import org.sitmun.infrastructure.startup.BuiltInUserRepairResult;
import org.sitmun.infrastructure.startup.BuiltInUserStartupStatus;
import org.sitmun.infrastructure.startup.BuiltInUsersStartupProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Transactional repair of built-in admin/public user invariants. */
@Service
@Slf4j
public class BuiltInUserRepairService {

  private final UserRepository userRepository;
  private final UserPositionRepository userPositionRepository;
  private final PasswordEncoder passwordEncoder;
  private final BuiltInUsersStartupProperties properties;
  private final Clock clock;

  public BuiltInUserRepairService(
      UserRepository userRepository,
      UserPositionRepository userPositionRepository,
      PasswordEncoder passwordEncoder,
      BuiltInUsersStartupProperties properties,
      Clock clock) {
    this.userRepository = userRepository;
    this.userPositionRepository = userPositionRepository;
    this.passwordEncoder = passwordEncoder;
    this.properties = properties;
    this.clock = clock;
  }

  @Transactional
  public BuiltInUserRepairResult repair() {
    User publicUser = loadOrCreatePublic();
    repairPublic(publicUser);
    userRepository.save(publicUser);
    deletePositions(publicUser);

    Optional<User> existingAdmin =
        userRepository.findByUsername(SecurityConstants.BUILT_IN_ADMIN_PRINCIPAL);
    boolean passwordNeeded =
        existingAdmin.isEmpty() || StringUtils.isBlank(existingAdmin.get().getPassword());

    if (passwordNeeded && properties.normalizedAdminPassword().isEmpty()) {
      existingAdmin.ifPresent(
          admin -> {
            repairAdminFlags(admin);
            userRepository.save(admin);
            deletePositions(admin);
          });
      return BuiltInUserRepairResult.blocked(
          BuiltInUserStartupStatus.REASON_ADMIN_MISSING_BOOTSTRAP_PASSWORD);
    }

    User admin = existingAdmin.orElseGet(this::newAdminShell);
    repairAdminFlags(admin);
    if (StringUtils.isBlank(admin.getPassword())) {
      String plaintext = properties.normalizedAdminPassword().orElseThrow();
      admin.setPassword(passwordEncoder.encode(plaintext));
      admin.setLastPasswordChange(Date.from(clock.instant()));
    }
    userRepository.save(admin);
    deletePositions(admin);

    return BuiltInUserRepairResult.succeeded();
  }

  private User loadOrCreatePublic() {
    return userRepository
        .findByUsername(SecurityConstants.PUBLIC_PRINCIPAL)
        .orElseGet(this::newPublicShell);
  }

  private User newPublicShell() {
    return User.builder()
        .username(SecurityConstants.PUBLIC_PRINCIPAL)
        .administrator(false)
        .blocked(false)
        .build();
  }

  private User newAdminShell() {
    return User.builder()
        .username(SecurityConstants.BUILT_IN_ADMIN_PRINCIPAL)
        .administrator(true)
        .blocked(false)
        .build();
  }

  void repairPublic(User user) {
    user.setAdministrator(false);
    user.setBlocked(false);
    user.setPassword(null);
    user.setFirstName(null);
    user.setLastName(null);
    user.setEmail(null);
    user.setIdentificationNumber(null);
    user.setIdentificationType(null);
    user.setLastPasswordChange(null);
  }

  void repairAdminFlags(User user) {
    user.setAdministrator(true);
    user.setBlocked(false);
  }

  private void deletePositions(User user) {
    List<UserPosition> positions = userPositionRepository.findByUser(user);
    if (!positions.isEmpty()) {
      userPositionRepository.deleteAll(positions);
    }
  }
}
