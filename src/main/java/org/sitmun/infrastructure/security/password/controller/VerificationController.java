package org.sitmun.infrastructure.security.password.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.sitmun.domain.user.UserRepository;
import org.sitmun.infrastructure.security.password.dto.PasswordVerificationRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/user-verification")
@Tag(name = "User verification")
@Validated
public class VerificationController {
  private final UserRepository userRepository;
  private final AuthenticationManager authenticationManager;

  VerificationController(
      AuthenticationManager authenticationManager, UserRepository userRepository) {
    this.userRepository = userRepository;
    this.authenticationManager = authenticationManager;
  }

  /** Verify if the current user's password is valid */
  @PostMapping("/verify-password")
  @SecurityRequirements
  public ResponseEntity<Boolean> verifyPassword(
      @Valid @RequestBody PasswordVerificationRequest request) {
    // Get current username from authentication
    Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
    String currentUsername = currentAuth.getName();
    try {

      // Check if the password is correct
      Authentication authentication =
          this.authenticationManager.authenticate(
              new UsernamePasswordAuthenticationToken(currentUsername, request.getPassword()));
      return new ResponseEntity<>(authentication.isAuthenticated(), HttpStatus.OK);
    } catch (BadCredentialsException e) {
      log.warn("Invalid credentials for user {}: {}", currentUsername, e.getMessage());
      return new ResponseEntity<>(false, HttpStatus.OK);
    }
  }

  /** Verify is this email is already used */
  @PostMapping("/verify-email")
  public ResponseEntity<Boolean> verifyEmail(@RequestBody(required = false) String email) {
    // Handle null or empty email
    if (email == null || email.trim().isEmpty()) {
      return ResponseEntity.ok(false);
    }

    // Validate email format using Jakarta Validation
    if (!isValidEmail(email.trim())) {
      return ResponseEntity.ok(false);
    }

    boolean emailAlreadyTaken = userRepository.findByEmail(email.trim()).isPresent();
    return ResponseEntity.ok(emailAlreadyTaken);
  }

  /**
   * Validates the email format using Jakarta Validation's email pattern. This uses the same
   * validation logic as the @Email annotation.
   */
  private boolean isValidEmail(@NotNull String email) {
    // Use the same pattern as Jakarta Validation's @Email annotation
    String emailPattern = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
    return email.matches(emailPattern);
  }
}
