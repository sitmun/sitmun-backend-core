package org.sitmun.verification.controller;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.sitmun.authentication.dto.UserPasswordAuthenticationRequest;
import org.sitmun.domain.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/api/user-verification")
@Tag(name = "User verification")
public class VerificationController {
  private final AuthenticationManager authenticationManager;
  private final UserRepository userRepository;
  VerificationController(AuthenticationManager authenticationManager, UserRepository userRepository) {
    this.authenticationManager = authenticationManager;
    this.userRepository = userRepository;
  }
  /**
   * Verify if the user password is valid
   */
  @PostMapping("/verify-password")
  @SecurityRequirements
  public ResponseEntity<Boolean> verifyPassword(@Valid @RequestBody UserPasswordAuthenticationRequest body) {
    ResponseEntity<Boolean> response;
    try{
      Authentication authentication = authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(body.getUsername(), body.getPassword()));
      response = new ResponseEntity<>(authentication.isAuthenticated(), HttpStatus.OK);
    }
    catch (BadCredentialsException e){
      log.warn("Invalid credentials for user {}: {}", body.getUsername(), e.getMessage());
      response = new ResponseEntity<>(false, HttpStatus.BAD_REQUEST);
    }
    return response;
  }
  /**
   * Verify is this email is already used
   */
  @PostMapping("/verify-email")
  public ResponseEntity<Boolean> verifyEmail(@RequestBody(required = false) String email) {
    if (email == null) ResponseEntity.ok(true);
    boolean emailAlreadyTaken = userRepository.findByEmail(email).isPresent();
    return ResponseEntity.ok(emailAlreadyTaken);
  }
}