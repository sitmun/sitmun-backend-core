package org.sitmun.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sitmun.domain.user.position.UserPosition;
import org.sitmun.domain.user.position.UserPositionRepository;
import org.sitmun.infrastructure.security.core.SecurityConstants;
import org.sitmun.infrastructure.startup.BuiltInUserStartupStatus;
import org.sitmun.infrastructure.startup.BuiltInUsersStartupProperties;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@DisplayName("BuiltInUserStartupRepairer")
class BuiltInUserStartupRepairerTest {

  private static final Instant FIXED_INSTANT = Instant.parse("2026-07-17T12:00:00Z");
  private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
  private static final String BOOTSTRAP_PASSWORD = "bootstrap-secret-value";
  private static final String ENCODED_PASSWORD = "$2a$10$encodedBootstrapHash";

  @Mock private UserRepository userRepository;
  @Mock private UserPositionRepository userPositionRepository;
  @Mock private PasswordEncoder passwordEncoder;

  private BuiltInUsersStartupProperties properties;
  private BuiltInUserStartupStatus status;
  private BuiltInUserRepairService repairService;
  private BuiltInUserStartupRepairer repairer;

  @BeforeEach
  void setUp() {
    properties = new BuiltInUsersStartupProperties();
    status = new BuiltInUserStartupStatus();
    repairService =
        new BuiltInUserRepairService(
            userRepository, userPositionRepository, passwordEncoder, properties, FIXED_CLOCK);
    repairer = new BuiltInUserStartupRepairer(repairService, status);
  }

  @Test
  @DisplayName(
      "valid built-ins: delete public positions only; warn and preserve admin positions; READY")
  void validBuiltInsDeletePublicPositionsAndWarnOnAdminPositions() {
    User admin = validAdmin();
    User pub = validPublic();
    stubUsers(admin, pub);
    UserPosition adminPos = UserPosition.builder().id(1).user(admin).build();
    UserPosition publicPos = UserPosition.builder().id(2).user(pub).build();
    when(userPositionRepository.findByUser(admin)).thenReturn(List.of(adminPos));
    when(userPositionRepository.findByUser(pub)).thenReturn(List.of(publicPos));
    when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    repairer.run(new DefaultApplicationArguments());

    assertThat(status.getState()).isEqualTo(BuiltInUserStartupStatus.State.READY);
    assertThat(status.getReason()).isNull();
    assertThat(status.getWarnings())
        .containsExactly(BuiltInUserStartupStatus.WARNING_ADMIN_HAS_POSITIONS);
    verify(userPositionRepository).deleteAll(List.of(publicPos));
    verify(userPositionRepository, never()).deleteAll(List.of(adminPos));
    assertThat(admin.getPassword()).isEqualTo("existing-hash");
    assertThat(admin.getFirstName()).isEqualTo("Administrator");
  }

  @Test
  @DisplayName("admin without positions: READY with no warnings")
  void adminWithoutPositionsHasNoWarnings() {
    stubUsers(validAdmin(), validPublic());
    when(userPositionRepository.findByUser(any())).thenReturn(List.of());
    when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    repairer.run(new DefaultApplicationArguments());

    assertThat(status.getState()).isEqualTo(BuiltInUserStartupStatus.State.READY);
    assertThat(status.getWarnings()).isEmpty();
    verify(userPositionRepository, never()).deleteAll(anyList());
  }

  @Test
  @DisplayName("drifted admin: restore flags; preserve password, name, email, id")
  void driftedAdminFlagsRepaired() {
    User admin =
        User.builder()
            .id(1)
            .username(SecurityConstants.BUILT_IN_ADMIN_PRINCIPAL)
            .password("existing-hash")
            .firstName("Administrator")
            .email("admin@example.com")
            .administrator(false)
            .blocked(true)
            .build();
    User pub = validPublic();
    stubUsers(admin, pub);
    when(userPositionRepository.findByUser(any())).thenReturn(List.of());
    when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    repairer.run(new DefaultApplicationArguments());

    ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
    verify(userRepository, org.mockito.Mockito.atLeastOnce()).save(saved.capture());
    User savedAdmin =
        saved.getAllValues().stream()
            .filter(u -> SecurityConstants.BUILT_IN_ADMIN_PRINCIPAL.equals(u.getUsername()))
            .findFirst()
            .orElseThrow();
    assertThat(savedAdmin.getId()).isEqualTo(1);
    assertThat(savedAdmin.getAdministrator()).isTrue();
    assertThat(savedAdmin.getBlocked()).isFalse();
    assertThat(savedAdmin.getPassword()).isEqualTo("existing-hash");
    assertThat(savedAdmin.getFirstName()).isEqualTo("Administrator");
    assertThat(savedAdmin.getEmail()).isEqualTo("admin@example.com");
    assertThat(status.getState()).isEqualTo(BuiltInUserStartupStatus.State.READY);
    verify(passwordEncoder, never()).encode(any());
  }

  @Test
  @DisplayName("missing admin with bootstrap secret: encode once, set lastPasswordChange, READY")
  void missingAdminCreatedWithBootstrapSecret() {
    when(userRepository.findByUsername(SecurityConstants.BUILT_IN_ADMIN_PRINCIPAL))
        .thenReturn(Optional.empty());
    when(userRepository.findByUsername(SecurityConstants.PUBLIC_PRINCIPAL))
        .thenReturn(Optional.of(validPublic()));
    when(userPositionRepository.findByUser(any())).thenReturn(List.of());
    properties.setAdminPassword(BOOTSTRAP_PASSWORD);
    when(passwordEncoder.encode(BOOTSTRAP_PASSWORD)).thenReturn(ENCODED_PASSWORD);
    when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    repairer.run(new DefaultApplicationArguments());

    ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
    verify(userRepository, org.mockito.Mockito.atLeastOnce()).save(saved.capture());
    User savedAdmin =
        saved.getAllValues().stream()
            .filter(u -> SecurityConstants.BUILT_IN_ADMIN_PRINCIPAL.equals(u.getUsername()))
            .findFirst()
            .orElseThrow();
    assertThat(savedAdmin.getPassword()).isEqualTo(ENCODED_PASSWORD);
    assertThat(savedAdmin.getPassword()).isNotEqualTo(BOOTSTRAP_PASSWORD);
    assertThat(savedAdmin.getLastPasswordChange()).isEqualTo(Date.from(FIXED_INSTANT));
    assertThat(savedAdmin.getAdministrator()).isTrue();
    assertThat(savedAdmin.getBlocked()).isFalse();
    assertThat(status.getState()).isEqualTo(BuiltInUserStartupStatus.State.READY);
    verify(passwordEncoder).encode(BOOTSTRAP_PASSWORD);
  }

  @Test
  @DisplayName("empty-password admin with secret: restore encoded password")
  void emptyPasswordAdminRestoredWithSecret() {
    User admin =
        User.builder()
            .id(1)
            .username(SecurityConstants.BUILT_IN_ADMIN_PRINCIPAL)
            .password("")
            .administrator(true)
            .blocked(false)
            .build();
    stubUsers(admin, validPublic());
    when(userPositionRepository.findByUser(any())).thenReturn(List.of());
    properties.setAdminPassword(BOOTSTRAP_PASSWORD);
    when(passwordEncoder.encode(BOOTSTRAP_PASSWORD)).thenReturn(ENCODED_PASSWORD);
    when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    repairer.run(new DefaultApplicationArguments());

    assertThat(admin.getPassword()).isEqualTo(ENCODED_PASSWORD);
    assertThat(admin.getLastPasswordChange()).isEqualTo(Date.from(FIXED_INSTANT));
    assertThat(status.getState()).isEqualTo(BuiltInUserStartupStatus.State.READY);
  }

  @Test
  @DisplayName("missing admin without secret: no throw, no unusable admin, BLOCKED")
  void missingAdminWithoutSecretBlocks() {
    when(userRepository.findByUsername(SecurityConstants.BUILT_IN_ADMIN_PRINCIPAL))
        .thenReturn(Optional.empty());
    when(userRepository.findByUsername(SecurityConstants.PUBLIC_PRINCIPAL))
        .thenReturn(Optional.of(validPublic()));
    when(userPositionRepository.findByUser(any())).thenReturn(List.of());
    when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    assertThatCode(() -> repairer.run(new DefaultApplicationArguments()))
        .doesNotThrowAnyException();

    assertThat(status.getState()).isEqualTo(BuiltInUserStartupStatus.State.BLOCKED);
    assertThat(status.getReason())
        .isEqualTo(BuiltInUserStartupStatus.REASON_ADMIN_MISSING_BOOTSTRAP_PASSWORD);
    verify(userRepository, never())
        .save(
            org.mockito.ArgumentMatchers.argThat(
                u ->
                    u != null
                        && SecurityConstants.BUILT_IN_ADMIN_PRINCIPAL.equals(u.getUsername())));
    verify(passwordEncoder, never()).encode(any());
  }

  @Test
  @DisplayName("missing public: create with required booleans and null sensitive fields")
  void missingPublicIsCreated() {
    User admin = validAdmin();
    when(userRepository.findByUsername(SecurityConstants.BUILT_IN_ADMIN_PRINCIPAL))
        .thenReturn(Optional.of(admin));
    when(userRepository.findByUsername(SecurityConstants.PUBLIC_PRINCIPAL))
        .thenReturn(Optional.empty());
    when(userPositionRepository.findByUser(any())).thenReturn(List.of());
    when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    repairer.run(new DefaultApplicationArguments());

    ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
    verify(userRepository, org.mockito.Mockito.atLeastOnce()).save(saved.capture());
    User savedPublic =
        saved.getAllValues().stream()
            .filter(u -> SecurityConstants.PUBLIC_PRINCIPAL.equals(u.getUsername()))
            .findFirst()
            .orElseThrow();
    assertThat(savedPublic.getAdministrator()).isFalse();
    assertThat(savedPublic.getBlocked()).isFalse();
    assertThat(savedPublic.getPassword()).isNull();
    assertThat(savedPublic.getFirstName()).isNull();
    assertThat(savedPublic.getLastName()).isNull();
    assertThat(savedPublic.getEmail()).isNull();
    assertThat(savedPublic.getIdentificationNumber()).isNull();
    assertThat(savedPublic.getIdentificationType()).isNull();
    assertThat(status.getState()).isEqualTo(BuiltInUserStartupStatus.State.READY);
  }

  @Test
  @DisplayName("drifted public: clear sensitive fields and restore booleans")
  void driftedPublicIsCleared() {
    User pub =
        User.builder()
            .id(2)
            .username(SecurityConstants.PUBLIC_PRINCIPAL)
            .password("should-clear")
            .firstName("Public")
            .lastName("User")
            .email("public@example.com")
            .identificationNumber("X")
            .identificationType("NIF")
            .lastPasswordChange(new Date())
            .administrator(true)
            .blocked(true)
            .build();
    stubUsers(validAdmin(), pub);
    when(userPositionRepository.findByUser(any())).thenReturn(List.of());
    when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    repairer.run(new DefaultApplicationArguments());

    assertThat(pub.getAdministrator()).isFalse();
    assertThat(pub.getBlocked()).isFalse();
    assertThat(pub.getPassword()).isNull();
    assertThat(pub.getFirstName()).isNull();
    assertThat(pub.getLastName()).isNull();
    assertThat(pub.getEmail()).isNull();
    assertThat(pub.getIdentificationNumber()).isNull();
    assertThat(pub.getIdentificationType()).isNull();
    assertThat(pub.getLastPasswordChange()).isNull();
    assertThat(status.getState()).isEqualTo(BuiltInUserStartupStatus.State.READY);
  }

  @Test
  @DisplayName("second run over repaired values remains READY")
  void secondRunRemainsReady() {
    stubUsers(validAdmin(), validPublic());
    when(userPositionRepository.findByUser(any())).thenReturn(List.of());
    when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    repairer.run(new DefaultApplicationArguments());
    repairer.run(new DefaultApplicationArguments());

    assertThat(status.getState()).isEqualTo(BuiltInUserStartupStatus.State.READY);
  }

  @Test
  @DisplayName("repository failure is caught and records repair-failed")
  void repositoryFailureBlocksWithoutThrowing() {
    when(userRepository.findByUsername(SecurityConstants.PUBLIC_PRINCIPAL))
        .thenThrow(new RuntimeException("db down"));

    assertThatCode(() -> repairer.run(new DefaultApplicationArguments()))
        .doesNotThrowAnyException();

    assertThat(status.getState()).isEqualTo(BuiltInUserStartupStatus.State.BLOCKED);
    assertThat(status.getReason()).isEqualTo(BuiltInUserStartupStatus.REASON_REPAIR_FAILED);
    verify(userPositionRepository, never()).deleteAll(anyList());
  }

  @Test
  @DisplayName("empty-password admin without secret: BLOCKED; safe public repairs still saved")
  void emptyPasswordAdminWithoutSecretBlocksAfterSafeRepairs() {
    User admin =
        User.builder()
            .id(1)
            .username(SecurityConstants.BUILT_IN_ADMIN_PRINCIPAL)
            .password(null)
            .administrator(false)
            .blocked(true)
            .build();
    User pub = validPublic();
    stubUsers(admin, pub);
    when(userPositionRepository.findByUser(any())).thenReturn(List.of());
    when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    repairer.run(new DefaultApplicationArguments());

    assertThat(status.getState()).isEqualTo(BuiltInUserStartupStatus.State.BLOCKED);
    assertThat(status.getReason())
        .isEqualTo(BuiltInUserStartupStatus.REASON_ADMIN_MISSING_BOOTSTRAP_PASSWORD);
    assertThat(admin.getAdministrator()).isTrue();
    assertThat(admin.getBlocked()).isFalse();
    assertThat(admin.getPassword()).isNull();
    verify(passwordEncoder, never()).encode(any());
  }

  private void stubUsers(User admin, User pub) {
    when(userRepository.findByUsername(SecurityConstants.BUILT_IN_ADMIN_PRINCIPAL))
        .thenReturn(Optional.of(admin));
    when(userRepository.findByUsername(SecurityConstants.PUBLIC_PRINCIPAL))
        .thenReturn(Optional.of(pub));
  }

  private static User validAdmin() {
    return User.builder()
        .id(1)
        .username(SecurityConstants.BUILT_IN_ADMIN_PRINCIPAL)
        .password("existing-hash")
        .firstName("Administrator")
        .administrator(true)
        .blocked(false)
        .build();
  }

  private static User validPublic() {
    return User.builder()
        .id(2)
        .username(SecurityConstants.PUBLIC_PRINCIPAL)
        .administrator(false)
        .blocked(false)
        .build();
  }
}
