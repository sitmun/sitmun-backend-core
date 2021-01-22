package org.sitmun.plugin.core.web.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.sitmun.plugin.core.security.TokenProvider;
import org.sitmun.plugin.core.security.jwt.JWTConfigurer;
import org.sitmun.plugin.core.web.rest.dto.LoginRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * Controller to authenticate users.
 */
@RestController
@RequestMapping("/api")
@Tag(name = "authentication", description = "authentication with JWT")
public class AuthController {

  private final TokenProvider tokenProvider;

  private final AuthenticationManager authenticationManager;

  public AuthController(TokenProvider tokenProvider,
                        AuthenticationManager authenticationManager) {
    this.tokenProvider = tokenProvider;
    this.authenticationManager = authenticationManager;
  }

  /**
   * Authenticate a user an obtain a JWT token.
   *
   * @param loginRequest user login and password
   * @return JWT token
   */
  @PostMapping("/authenticate")
  @SecurityRequirements
  public ResponseEntity<JWTToken> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
    Authentication authentication = authenticationManager.authenticate(
      new UsernamePasswordAuthenticationToken(
        loginRequest.getUsername(),
        loginRequest.getPassword()));

    SecurityContextHolder.getContext().setAuthentication(authentication);
    String jwt = tokenProvider.createToken(authentication);
    JWTToken jwtToken = new JWTToken(jwt);

    return ResponseEntity.ok()
      .header(JWTConfigurer.AUTHORIZATION_HEADER, "Bearer " + jwt)
      .body(jwtToken);
  }

  /**
   * Object to return as body in JWT Authentication.
   */
  public static class JWTToken {

    private String idToken;

    public JWTToken() {
    }

    public JWTToken(String idToken) {
      this.idToken = idToken;
    }

    @JsonProperty("id_token")
    public String getIdToken() {
      return idToken;
    }

    void setIdToken(String idToken) {
      this.idToken = idToken;
    }
  }
}
