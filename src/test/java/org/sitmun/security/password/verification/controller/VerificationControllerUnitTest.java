package org.sitmun.security.password.verification.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.domain.user.User;
import org.sitmun.domain.user.UserRepository;
import org.sitmun.infrastructure.security.password.controller.VerificationController;
import org.sitmun.infrastructure.security.password.dto.PasswordVerificationRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@DisplayName("Verification Controller Unit tests")
class VerificationControllerUnitTest {

  @MockitoBean
  private UserRepository userRepository;

  @MockitoBean private PasswordEncoder passwordEncoder;

  @Autowired private VerificationController verificationController;

  private PasswordVerificationRequest validRequest;
  private PasswordVerificationRequest invalidRequest;

  @BeforeEach
  void setUp() {
    validRequest = new PasswordVerificationRequest();
    validRequest.setPassword("password");

    invalidRequest = new PasswordVerificationRequest();
    invalidRequest.setPassword("wrongpassword");
  }

  @Test
  @DisplayName("Verify password with valid credentials should return true")
  @WithMockUser(username = "testuser")
  void verifyPasswordWithValidCredentials() {
    // Given
    User user = new User();
    user.setUsername("testuser");
    user.setPassword("encodedPassword");
    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("password", "encodedPassword")).thenReturn(true);

    // When
    ResponseEntity<Boolean> response = verificationController.verifyPassword(validRequest);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isTrue();
  }

  @Test
  @DisplayName("Verify password with invalid credentials should return false")
  @WithMockUser(username = "testuser")
  void verifyPasswordWithInvalidCredentials() {
    // Given
    User user = new User();
    user.setUsername("testuser");
    user.setPassword("encodedPassword");
    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("wrongpassword", "encodedPassword")).thenReturn(false);

    // When
    ResponseEntity<Boolean> response = verificationController.verifyPassword(invalidRequest);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isFalse();
  }

  @Test
  @DisplayName("Verify password with non-existent user should return false")
  @WithMockUser(username = "nonexistentuser")
  void verifyPasswordWithNonExistentUser() {
    // Given
    when(userRepository.findByUsername("nonexistentuser")).thenReturn(Optional.empty());

    // When
    ResponseEntity<Boolean> response = verificationController.verifyPassword(validRequest);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isFalse();
  }

  @Test
  @DisplayName("Verify email with existing email should return true")
  void verifyEmailWithExistingEmail() {
    // Given
    String email = "test@example.com";
    User existingUser = new User();
    existingUser.setEmail(email);
    when(userRepository.findByEmail(email)).thenReturn(Optional.of(existingUser));
    // When
    ResponseEntity<Boolean> response = verificationController.verifyEmail(email);
    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isTrue();
  }

  @Test
  @DisplayName("Verify email with non-existing email should return false")
  void verifyEmailWithNonExistingEmail() {
    // Given
    String email = "nonexistent@example.com";
    when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
    // When
    ResponseEntity<Boolean> response = verificationController.verifyEmail(email);
    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isFalse();
  }

  @Test
  @DisplayName("Verify email should be case insensitive")
  void verifyEmailCaseInsensitive() {
    // Given
    String email = "TEST@EXAMPLE.COM";
    User existingUser = new User();
    existingUser.setEmail("test@example.com");
    when(userRepository.findByEmail(email)).thenReturn(Optional.of(existingUser));
    // When
    ResponseEntity<Boolean> response = verificationController.verifyEmail(email);
    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isTrue();
  }

  @Test
  @DisplayName("Verify email with empty string should return false")
  void verifyEmailWithEmptyString() {
    // When
    ResponseEntity<Boolean> response = verificationController.verifyEmail("");
    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isFalse();
  }

  @Test
  @DisplayName("Verify email with null should return false")
  void verifyEmailWithNull() {
    // When
    ResponseEntity<Boolean> response = verificationController.verifyEmail(null);
    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isFalse();
  }
}
