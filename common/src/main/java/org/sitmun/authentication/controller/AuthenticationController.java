package org.sitmun.authentication.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.sitmun.authentication.dto.AuthenticationResponse;
import org.sitmun.authentication.dto.UserPasswordAuthenticationRequest;
import org.sitmun.infrastructure.security.service.JsonWebTokenService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
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
  JsonWebTokenService jsonWebTokenService;

  public AuthenticationController(AuthenticationManager authenticationManager, JsonWebTokenService jsonWebTokenService) {
    this.authenticationManager = authenticationManager;
    this.jsonWebTokenService = jsonWebTokenService;
  }

  /**
   * Authenticate a user an obtain a JWT token.
   *
   * @param userPasswordAuthenticationRequest user login and password
   * @return JWT token
   */
  @PostMapping
  @SecurityRequirements
  public ResponseEntity<AuthenticationResponse> authenticateUser(@Valid @RequestBody UserPasswordAuthenticationRequest userPasswordAuthenticationRequest) {
	  Authentication authentication = authenticationManager.authenticate(
		      new UsernamePasswordAuthenticationToken(userPasswordAuthenticationRequest.getUsername(), userPasswordAuthenticationRequest.getPassword()));
	  String token = jsonWebTokenService.generateToken((UserDetails)authentication.getPrincipal());
	  return ResponseEntity.ok()
	    .header(HttpHeaders.AUTHORIZATION, token)
	    .body(new AuthenticationResponse(token));
  }
}
