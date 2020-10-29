package org.sitmun.plugin.core.web.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import org.sitmun.plugin.core.security.TokenProvider;
import org.sitmun.plugin.core.security.jwt.JWTConfigurer;
import org.sitmun.plugin.core.web.rest.dto.LoginRequest;
import org.springframework.http.MediaType;
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
  @Operation(summary = "authenticate a user")
  @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "user's credentials",
      required = true,
      content = @Content(
          mediaType = MediaType.APPLICATION_JSON_VALUE,
          examples = {@ExampleObject(
              name = "admin",
              value = "{\n  \"username\": \"admin\",\n  \"password\": \"admin\"\n}",
              description = "system administrator credentials (in demo setups)"
          )},
          schema = @Schema(implementation = LoginRequest.class)))
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "user is authenticated", content =
      @Content(
          mediaType = MediaType.APPLICATION_JSON_VALUE,
          schema = @Schema(implementation = JWTToken.class))),
      @ApiResponse(responseCode = "401", description = "unauthorized")
  })
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
