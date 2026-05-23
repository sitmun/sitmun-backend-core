package org.sitmun.domain.user;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sitmun.domain.user.position.UserPosition;
import org.sitmun.domain.user.position.UserPositionRepository;
import org.springframework.boot.DefaultApplicationArguments;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserBuiltInStartupValidator")
class UserBuiltInStartupValidatorTest {

  @Mock private UserRepository userRepository;
  @Mock private UserPositionRepository userPositionRepository;

  private UserBuiltInStartupValidator validator;

  private User validAdmin;
  private User validPublic;

  @BeforeEach
  void setUp() {
    validator = new UserBuiltInStartupValidator(userRepository, userPositionRepository);

    validAdmin =
        User.builder()
            .id(1)
            .username("admin")
            .administrator(true)
            .blocked(false)
            .password("$2a$encoded")
            .build();

    validPublic =
        User.builder()
            .id(2)
            .username("public")
            .administrator(false)
            .blocked(false)
            .password(null)
            .build();
  }

  @Test
  @DisplayName("Valid state: both built-in users pass all invariants")
  void validStatePasses() throws Exception {
    when(userRepository.findByUsername("admin")).thenReturn(Optional.of(validAdmin));
    when(userRepository.findByUsername("public")).thenReturn(Optional.of(validPublic));
    when(userPositionRepository.findByUser(validAdmin)).thenReturn(List.of());
    when(userPositionRepository.findByUser(validPublic)).thenReturn(List.of());

    assertThatNoException().isThrownBy(() -> validator.run(new DefaultApplicationArguments()));
  }

  @Test
  @DisplayName("Fails when admin user does not exist")
  void failsWhenAdminMissing() {
    when(userRepository.findByUsername("admin")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> validator.run(new DefaultApplicationArguments()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("admin");
  }

  @Test
  @DisplayName("Fails when admin is not administrator")
  void failsWhenAdminIsNotAdministrator() {
    User demoted = validAdmin.toBuilder().administrator(false).build();
    when(userRepository.findByUsername("admin")).thenReturn(Optional.of(demoted));

    assertThatThrownBy(() -> validator.run(new DefaultApplicationArguments()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("administrator=true");
  }

  @Test
  @DisplayName("Fails when admin is blocked")
  void failsWhenAdminIsBlocked() {
    User blocked = validAdmin.toBuilder().blocked(true).build();
    when(userRepository.findByUsername("admin")).thenReturn(Optional.of(blocked));

    assertThatThrownBy(() -> validator.run(new DefaultApplicationArguments()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("blocked");
  }

  @Test
  @DisplayName("Fails when admin has no password")
  void failsWhenAdminHasNoPassword() {
    User noPass = validAdmin.toBuilder().password(null).build();
    when(userRepository.findByUsername("admin")).thenReturn(Optional.of(noPass));

    assertThatThrownBy(() -> validator.run(new DefaultApplicationArguments()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("password");
  }

  @Test
  @DisplayName("Fails when admin has UserPosition rows")
  void failsWhenAdminHasPositions() throws Exception {
    when(userRepository.findByUsername("admin")).thenReturn(Optional.of(validAdmin));
    when(userPositionRepository.findByUser(validAdmin))
        .thenReturn(List.of(UserPosition.builder().user(validAdmin).build()));

    assertThatThrownBy(() -> validator.run(new DefaultApplicationArguments()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("UserPosition");
  }

  @Test
  @DisplayName("Fails when public user does not exist")
  void failsWhenPublicMissing() throws Exception {
    when(userRepository.findByUsername("admin")).thenReturn(Optional.of(validAdmin));
    when(userPositionRepository.findByUser(validAdmin)).thenReturn(List.of());
    when(userRepository.findByUsername("public")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> validator.run(new DefaultApplicationArguments()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("public");
  }

  @Test
  @DisplayName("Fails when public is marked as administrator")
  void failsWhenPublicIsAdministrator() throws Exception {
    User promoted = validPublic.toBuilder().administrator(true).build();
    when(userRepository.findByUsername("admin")).thenReturn(Optional.of(validAdmin));
    when(userPositionRepository.findByUser(validAdmin)).thenReturn(List.of());
    when(userRepository.findByUsername("public")).thenReturn(Optional.of(promoted));

    assertThatThrownBy(() -> validator.run(new DefaultApplicationArguments()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("administrator=false");
  }

  @Test
  @DisplayName("Public user blocked is allowed (no startup failure)")
  void publicBlockedIsAllowed() throws Exception {
    User blocked = validPublic.toBuilder().blocked(true).build();
    when(userRepository.findByUsername("admin")).thenReturn(Optional.of(validAdmin));
    when(userPositionRepository.findByUser(validAdmin)).thenReturn(List.of());
    when(userRepository.findByUsername("public")).thenReturn(Optional.of(blocked));
    when(userPositionRepository.findByUser(blocked)).thenReturn(List.of());

    assertThatNoException().isThrownBy(() -> validator.run(new DefaultApplicationArguments()));
  }

  @Test
  @DisplayName("Fails when public has a password")
  void failsWhenPublicHasPassword() throws Exception {
    User withPass = validPublic.toBuilder().password("secret").build();
    when(userRepository.findByUsername("admin")).thenReturn(Optional.of(validAdmin));
    when(userPositionRepository.findByUser(validAdmin)).thenReturn(List.of());
    when(userRepository.findByUsername("public")).thenReturn(Optional.of(withPass));

    assertThatThrownBy(() -> validator.run(new DefaultApplicationArguments()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("password");
  }

  @Test
  @DisplayName("Fails when public has personal information (firstName)")
  void failsWhenPublicHasPersonalInfo() throws Exception {
    User withInfo = validPublic.toBuilder().firstName("Public").build();
    when(userRepository.findByUsername("admin")).thenReturn(Optional.of(validAdmin));
    when(userPositionRepository.findByUser(validAdmin)).thenReturn(List.of());
    when(userRepository.findByUsername("public")).thenReturn(Optional.of(withInfo));

    assertThatThrownBy(() -> validator.run(new DefaultApplicationArguments()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("personal information");
  }

  @Test
  @DisplayName("Fails when public has UserPosition rows")
  void failsWhenPublicHasPositions() throws Exception {
    when(userRepository.findByUsername("admin")).thenReturn(Optional.of(validAdmin));
    when(userPositionRepository.findByUser(validAdmin)).thenReturn(List.of());
    when(userRepository.findByUsername("public")).thenReturn(Optional.of(validPublic));
    when(userPositionRepository.findByUser(validPublic))
        .thenReturn(List.of(UserPosition.builder().user(validPublic).build()));

    assertThatThrownBy(() -> validator.run(new DefaultApplicationArguments()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("UserPosition");
  }
}
