package org.sitmun.security.web;

import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.sitmun.repository.UserRepository;
import org.sitmun.security.jwt.JwtUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * Controller to authenticate users.
 */
@RestController
@RequestMapping("/api/authenticate")
@Tag(name = "authentication", description = "authentication with JWT")
public class AuthenticationController {

  final
  AuthenticationManager authenticationManager;

  final
  UserRepository userRepository;

  final
  PasswordEncoder encoder;

  final
  JwtUtils jwtUtils;

  public AuthenticationController(AuthenticationManager authenticationManager, UserRepository userRepository, PasswordEncoder encoder, JwtUtils jwtUtils) {
    this.authenticationManager = authenticationManager;
    this.userRepository = userRepository;
    this.encoder = encoder;
    this.jwtUtils = jwtUtils;
  }

  /**
   * Authenticate a user an obtain a JWT token.
   *
   * @param loginRequest user login and password
   * @return JWT token
   */
  @PostMapping
  @SecurityRequirements
  public ResponseEntity<JwtResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
    Authentication authentication = authenticationManager.authenticate(
      new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

    SecurityContextHolder.getContext().setAuthentication(authentication);
    String token = jwtUtils.generateBearerToken(authentication);
    return ResponseEntity.ok()
      .header(HttpHeaders.AUTHORIZATION, token)
      .body(new JwtResponse(token.substring(7)));
  }
}
