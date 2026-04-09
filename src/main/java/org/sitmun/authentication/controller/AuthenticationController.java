package org.sitmun.authentication.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.Date;
import java.util.Optional;
import org.sitmun.authentication.dto.AuthenticationResponse;
import org.sitmun.authentication.dto.UserPasswordAuthenticationRequest;
import org.sitmun.authentication.service.CookieService;
import org.sitmun.domain.user.User;
import org.sitmun.domain.user.UserRepository;
import org.sitmun.infrastructure.security.service.JsonWebTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Controller to authenticate users. */
@RestController
@RequestMapping("/api/authenticate")
@Tag(name = "authentication", description = "authentication with JWT")
@Validated
public class AuthenticationController {

  public static final String ACCESS_TOKEN_COOKIE_NAME = "access_token";
  public static final String PROXY_TOKEN_COOKIE_NAME = "proxy_token";

  @Value("${sitmun.proxy-middleware.token-validity-in-milliseconds}")
  private int validity;

  private final AuthenticationManager authenticationManager;

  private final UserDetailsService userDetailsService;

  private final UserRepository userRepository;

  private final PasswordEncoder encoder;

  private final JsonWebTokenService jsonWebTokenService;

  private final CookieService cookieService;

  public AuthenticationController(
      AuthenticationManager authenticationManager,
      UserDetailsService userDetailsService,
      PasswordEncoder encoder,
      JsonWebTokenService jsonWebTokenService,
      UserRepository userRepository,
      CookieService cookieService) {
    this.authenticationManager = authenticationManager;
    this.userDetailsService = userDetailsService;
    this.encoder = encoder;
    this.jsonWebTokenService = jsonWebTokenService;
    this.userRepository = userRepository;
    this.cookieService = cookieService;
  }

  /**
   * Authenticate a user and obtain a JWT token.
   *
   * @param body user login and password
   * @return JWT token
   */
  @PostMapping
  @SecurityRequirements
  public ResponseEntity<AuthenticationResponse> authenticateUser(
      @Valid @RequestBody UserPasswordAuthenticationRequest body,
      HttpServletRequest request,
      HttpServletResponse response) {
    Authentication authentication =
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(body.getUsername(), body.getPassword()));
    if (authentication.isAuthenticated()) {
      UserDetails userDetails = userDetailsService.loadUserByUsername(body.getUsername());
      Optional<User> user = this.userRepository.findByUsername(body.getUsername());
      if (user.isEmpty()) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
      }

      String token =
          jsonWebTokenService.generateToken(userDetails, user.get().getLastPasswordChange());

      final Cookie cookie = new Cookie(ACCESS_TOKEN_COOKIE_NAME, token);
      response.addCookie(
          cookieService.customizeAccessTokenCookie(cookie, request.isSecure(), null));
      return ResponseEntity.status(HttpStatus.OK).build();
    }
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
  }

  @PostMapping("/proxy")
  public ResponseEntity<AuthenticationResponse> authenticateProxy(
      Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
    final String username = authentication.getName();

    final String shortLivedToken =
        jsonWebTokenService.generateToken(username, new Date(), validity);
    final Cookie cookie = new Cookie(PROXY_TOKEN_COOKIE_NAME, shortLivedToken);

    response.addCookie(
        cookieService.customizeAccessTokenCookie(cookie, request.isSecure(), validity / 1000));
    return ResponseEntity.status(HttpStatus.OK).build();
  }
}
