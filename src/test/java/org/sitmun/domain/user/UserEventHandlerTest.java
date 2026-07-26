package org.sitmun.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sitmun.infrastructure.security.core.SecurityConstants.BUILT_IN_ADMIN_PRINCIPAL;
import static org.sitmun.infrastructure.security.core.SecurityConstants.PUBLIC_PRINCIPAL;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserEventHandler unit tests")
class UserEventHandlerTest {

  @Mock private PasswordEncoder passwordEncoder;
  @Mock private UserRepository userRepository;

  @InjectMocks private UserEventHandler handler;

  @Test
  @DisplayName("create: encodes non-empty password")
  void createEncodesPassword() {
    User user = User.builder().username("alice").password("plain").build();
    when(passwordEncoder.encode("plain")).thenReturn("encoded");

    handler.handleUserCreate(user);

    assertThat(user.getPassword()).isEqualTo("encoded");
    verify(passwordEncoder).encode("plain");
  }

  @Test
  @DisplayName("create: rejects empty password")
  void createRejectsEmptyPassword() {
    User user = User.builder().username("alice").password("").build();

    assertThatThrownBy(() -> handler.handleUserCreate(user))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(UserEventHandler.PASSWORD_CANNOT_BE_EMPTY);
    verify(passwordEncoder, never()).encode(anyString());
  }

  @Test
  @DisplayName("create: leaves null password unchanged")
  void createLeavesNullPassword() {
    User user = User.builder().username("alice").password(null).build();

    handler.handleUserCreate(user);

    assertThat(user.getPassword()).isNull();
    verify(passwordEncoder, never()).encode(anyString());
  }

  @Test
  @DisplayName("update: null password restores stored password")
  void updateKeepsStoredPassword() {
    User user = User.builder().id(1).username("alice").password(null).build();
    user.setStoredPassword("stored-hash");
    when(userRepository.findById(1)).thenReturn(Optional.of(user));

    handler.handleUserUpdate(user);

    assertThat(user.getPassword()).isEqualTo("stored-hash");
    verify(passwordEncoder, never()).encode(anyString());
  }

  @Test
  @DisplayName("update: rejects empty password")
  void updateRejectsEmptyPassword() {
    User user = User.builder().id(1).username("alice").password("").build();
    when(userRepository.findById(1)).thenReturn(Optional.of(user));

    assertThatThrownBy(() -> handler.handleUserUpdate(user))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(UserEventHandler.PASSWORD_CANNOT_BE_EMPTY);
  }

  @Test
  @DisplayName("update: encodes non-empty password")
  void updateEncodesPassword() {
    User user = User.builder().id(1).username("alice").password("new-plain").build();
    when(userRepository.findById(1)).thenReturn(Optional.of(user));
    when(passwordEncoder.encode("new-plain")).thenReturn("new-encoded");

    handler.handleUserUpdate(user);

    assertThat(user.getPassword()).isEqualTo("new-encoded");
  }

  @Test
  @DisplayName("update: rejects built-in admin username change")
  void updateRejectsAdminUsernameChange() {
    User original = User.builder().id(1).username(BUILT_IN_ADMIN_PRINCIPAL).build();
    User updated =
        User.builder()
            .id(1)
            .username("not-admin")
            .administrator(true)
            .blocked(false)
            .password(null)
            .build();
    updated.setStoredPassword("hash");
    when(userRepository.findById(1)).thenReturn(Optional.of(original));

    assertThatThrownBy(() -> handler.handleUserUpdate(updated))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Cannot change username of built-in admin");
  }

  @Test
  @DisplayName("update: rejects blocking built-in admin")
  void updateRejectsBlockingAdmin() {
    User original = User.builder().id(1).username(BUILT_IN_ADMIN_PRINCIPAL).build();
    User updated =
        User.builder()
            .id(1)
            .username(BUILT_IN_ADMIN_PRINCIPAL)
            .administrator(true)
            .blocked(true)
            .password(null)
            .build();
    updated.setStoredPassword("hash");
    when(userRepository.findById(1)).thenReturn(Optional.of(original));

    assertThatThrownBy(() -> handler.handleUserUpdate(updated))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Cannot block built-in admin");
  }

  @Test
  @DisplayName("update: rejects demoting built-in admin")
  void updateRejectsDemotingAdmin() {
    User original = User.builder().id(1).username(BUILT_IN_ADMIN_PRINCIPAL).build();
    User updated =
        User.builder()
            .id(1)
            .username(BUILT_IN_ADMIN_PRINCIPAL)
            .administrator(false)
            .blocked(false)
            .password(null)
            .build();
    updated.setStoredPassword("hash");
    when(userRepository.findById(1)).thenReturn(Optional.of(original));

    assertThatThrownBy(() -> handler.handleUserUpdate(updated))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Cannot remove administrator privilege");
  }

  @Test
  @DisplayName("update: rejects built-in public username change")
  void updateRejectsPublicUsernameChange() {
    User original = User.builder().id(2).username(PUBLIC_PRINCIPAL).build();
    User updated =
        User.builder().id(2).username("not-public").administrator(false).password(null).build();
    updated.setStoredPassword(null);
    when(userRepository.findById(2)).thenReturn(Optional.of(original));

    assertThatThrownBy(() -> handler.handleUserUpdate(updated))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Cannot change username of built-in public");
  }

  @Test
  @DisplayName("update: rejects promoting built-in public to administrator")
  void updateRejectsPromotingPublic() {
    User original = User.builder().id(2).username(PUBLIC_PRINCIPAL).build();
    User updated =
        User.builder().id(2).username(PUBLIC_PRINCIPAL).administrator(true).password(null).build();
    when(userRepository.findById(2)).thenReturn(Optional.of(original));

    assertThatThrownBy(() -> handler.handleUserUpdate(updated))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Cannot grant administrator privilege to built-in public");
  }

  @Test
  @DisplayName("update: rejects personal information on built-in public")
  void updateRejectsPublicPersonalInfo() {
    User original = User.builder().id(2).username(PUBLIC_PRINCIPAL).build();
    User updated =
        User.builder()
            .id(2)
            .username(PUBLIC_PRINCIPAL)
            .administrator(false)
            .firstName("Nope")
            .password(null)
            .build();
    when(userRepository.findById(2)).thenReturn(Optional.of(original));

    assertThatThrownBy(() -> handler.handleUserUpdate(updated))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Cannot set personal information for built-in public");
  }

  @Test
  @DisplayName("update: rejects password on built-in public")
  void updateRejectsPublicPassword() {
    User original = User.builder().id(2).username(PUBLIC_PRINCIPAL).build();
    User updated =
        User.builder()
            .id(2)
            .username(PUBLIC_PRINCIPAL)
            .administrator(false)
            .password("secret")
            .build();
    when(userRepository.findById(2)).thenReturn(Optional.of(original));

    assertThatThrownBy(() -> handler.handleUserUpdate(updated))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Cannot set password for built-in public");
  }

  @Test
  @DisplayName("delete: rejects built-in admin")
  void deleteRejectsAdmin() {
    User admin = User.builder().username(BUILT_IN_ADMIN_PRINCIPAL).build();

    assertThatThrownBy(() -> handler.handleUserDelete(admin))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Cannot delete built-in admin");
  }

  @Test
  @DisplayName("delete: rejects built-in public")
  void deleteRejectsPublic() {
    User publicUser = User.builder().username(PUBLIC_PRINCIPAL).build();

    assertThatThrownBy(() -> handler.handleUserDelete(publicUser))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Cannot delete built-in public");
  }

  @Test
  @DisplayName("delete: allows normal user")
  void deleteAllowsNormalUser() {
    User user = User.builder().username("alice").build();

    handler.handleUserDelete(user);
  }
}
