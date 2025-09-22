package org.sitmun.security.password.verification.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sitmun.authentication.dto.UserPasswordAuthenticationRequest;
import org.sitmun.domain.user.User;
import org.sitmun.domain.user.UserRepository;
import org.sitmun.infrastructure.security.password.controller.VerificationController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
@DisplayName("Verification Controller Unit tests")
class VerificationControllerUnitTest {

  @Mock private AuthenticationManager authenticationManager;

  @Mock private UserRepository userRepository;

  @Mock private Authentication authentication;

  @InjectMocks private VerificationController verificationController;

  private UserPasswordAuthenticationRequest validRequest;
  private UserPasswordAuthenticationRequest invalidRequest;

  @BeforeEach
  void setUp() {
    validRequest = new UserPasswordAuthenticationRequest();
    validRequest.setUsername("testuser");
    validRequest.setPassword("password");

    invalidRequest = new UserPasswordAuthenticationRequest();
    invalidRequest.setUsername("testuser");
    invalidRequest.setPassword("wrongpassword");
  }

  @Test
  @DisplayName("Verify password with valid credentials should return true")
  void verifyPasswordWithValidCredentials() {
    // Given
    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .thenReturn(authentication);
    when(authentication.isAuthenticated()).thenReturn(true);

    // When
    ResponseEntity<Boolean> response = verificationController.verifyPassword(validRequest);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isTrue();
  }

  @Test
  @DisplayName("Verify password with invalid credentials should return false")
  void verifyPasswordWithInvalidCredentials() {
    // Given
    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .thenThrow(new BadCredentialsException("Bad credentials"));

    // When
    ResponseEntity<Boolean> response = verificationController.verifyPassword(invalidRequest);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isFalse();
  }

  @Test
  @DisplayName("Verify password with unauthenticated user should return false")
  void verifyPasswordWithUnauthenticatedUser() {
    // Given
    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .thenReturn(authentication);
    when(authentication.isAuthenticated()).thenReturn(false);

    // When
    ResponseEntity<Boolean> response = verificationController.verifyPassword(validRequest);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
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
