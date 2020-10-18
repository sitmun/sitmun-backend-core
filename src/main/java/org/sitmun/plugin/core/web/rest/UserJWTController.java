package org.sitmun.plugin.core.web.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import org.sitmun.plugin.core.security.TokenProvider;
import org.sitmun.plugin.core.security.jwt.JWTConfigurer;
import org.sitmun.plugin.core.web.rest.dto.LoginDTO;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller to authenticate users.
 */
@RestController
@RequestMapping("/api")
@Tag(name = "login")
public class UserJWTController {

  private final TokenProvider tokenProvider;

  private final AuthenticationManager authenticationManager;

  public UserJWTController(TokenProvider tokenProvider,
                           AuthenticationManager authenticationManager) {
    this.tokenProvider = tokenProvider;
    this.authenticationManager = authenticationManager;
  }

  /**
   * Authenticate a user an obtain a JWT token.
   *
   * @param login user login and password
   * @return JWT token
   */
  @PostMapping("/authenticate")
  @Operation(summary = "authenticate a user and obtain a JWT token")
  @SecurityRequirements
  public ResponseEntity<JWTToken> authorize(
      @Parameter(description = "user's credentials", required = true,
          schema = @Schema(implementation = LoginDTO.class))
      @Valid @RequestBody LoginDTO login) {

    UsernamePasswordAuthenticationToken authenticationToken =
        new UsernamePasswordAuthenticationToken(login.getUsername(), login.getPassword());

    Authentication authentication = this.authenticationManager.authenticate(authenticationToken);
    SecurityContextHolder.getContext().setAuthentication(authentication);
    String jwt = tokenProvider.createToken(authentication);
    HttpHeaders httpHeaders = new HttpHeaders();
    httpHeaders.add(JWTConfigurer.AUTHORIZATION_HEADER, "Bearer " + jwt);
    return new ResponseEntity<>(new JWTToken(jwt), httpHeaders, HttpStatus.OK);
  }

  /**
   * Object to return as body in JWT Authentication.
   */
  static class JWTToken {

    private String idToken;

    JWTToken(String idToken) {
      this.idToken = idToken;
    }

    @JsonProperty("id_token")
    String getIdToken() {
      return idToken;
    }

    void setIdToken(String idToken) {
      this.idToken = idToken;
    }
  }
}
