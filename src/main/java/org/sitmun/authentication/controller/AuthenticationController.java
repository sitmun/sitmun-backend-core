package org.sitmun.authentication.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.Date;
import java.util.Optional;
import org.sitmun.authentication.OidcClientTypes;
import org.sitmun.authentication.SitmunClientTypes;
import org.sitmun.authentication.dto.AuthenticationResponse;
import org.sitmun.authentication.dto.MobileAuthenticationResponse;
import org.sitmun.authentication.dto.UserPasswordAuthenticationRequest;
import org.sitmun.authentication.service.CookieService;
import org.sitmun.authorization.client.service.MobileEditionAccessService;
import org.sitmun.domain.user.User;
import org.sitmun.domain.user.UserRepository;
import org.sitmun.infrastructure.security.core.Rfc9457ResponseWriter;
import org.sitmun.infrastructure.security.core.SecurityRole;
import org.sitmun.infrastructure.security.service.JsonWebTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.util.StringUtils;
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

  /**
   * Cookie name for viewer sessions. Default cookie when no {@code X-SITMUN-Client} header is
   * present.
   */
  public static final String VIEWER_ACCESS_TOKEN_COOKIE_NAME = "viewer_access_token";

  /** Cookie name for admin sessions. Set when {@code X-SITMUN-Client: admin} header is present. */
  public static final String ADMIN_ACCESS_TOKEN_COOKIE_NAME = "admin_access_token";

  /**
   * Legacy cookie name from before session isolation. Expired on login and logout to force
   * re-authentication from pre-migration sessions.
   *
   * @deprecated Use {@link #VIEWER_ACCESS_TOKEN_COOKIE_NAME} or {@link
   *     #ADMIN_ACCESS_TOKEN_COOKIE_NAME}.
   */
  @Deprecated public static final String ACCESS_TOKEN_COOKIE_NAME = "access_token";

  @Value("${sitmun.proxy-middleware.token-validity-in-milliseconds}")
  private int validity;

  private final AuthenticationManager authenticationManager;

  private final UserDetailsService userDetailsService;

  private final UserRepository userRepository;

  private final JsonWebTokenService jsonWebTokenService;

  private final CookieService cookieService;

  private final Rfc9457ResponseWriter responseWriter;

  private final MobileEditionAccessService mobileEditionAccessService;

  public AuthenticationController(
      AuthenticationManager authenticationManager,
      UserDetailsService userDetailsService,
      JsonWebTokenService jsonWebTokenService,
      UserRepository userRepository,
      CookieService cookieService,
      Rfc9457ResponseWriter responseWriter,
      MobileEditionAccessService mobileEditionAccessService) {
    this.authenticationManager = authenticationManager;
    this.userDetailsService = userDetailsService;
    this.jsonWebTokenService = jsonWebTokenService;
    this.userRepository = userRepository;
    this.cookieService = cookieService;
    this.responseWriter = responseWriter;
    this.mobileEditionAccessService = mobileEditionAccessService;
  }

  /**
   * Authenticate a viewer user and issue a {@value #VIEWER_ACCESS_TOKEN_COOKIE_NAME} cookie.
   * Expires any legacy {@value #ACCESS_TOKEN_COOKIE_NAME} cookie.
   */
  @PostMapping
  @SecurityRequirements
  public ResponseEntity<?> authenticateUser(
      @Valid @RequestBody UserPasswordAuthenticationRequest body,
      HttpServletRequest request,
      HttpServletResponse response) {
    return authenticate(body, request, response, VIEWER_ACCESS_TOKEN_COOKIE_NAME);
  }

  /**
   * Authenticate an admin user and issue an {@value #ADMIN_ACCESS_TOKEN_COOKIE_NAME} cookie.
   * Expires any legacy {@value #ACCESS_TOKEN_COOKIE_NAME} cookie.
   */
  @PostMapping("/admin")
  @SecurityRequirements
  public ResponseEntity<?> authenticateAdmin(
      @Valid @RequestBody UserPasswordAuthenticationRequest body,
      HttpServletRequest request,
      HttpServletResponse response) {
    return authenticate(body, request, response, ADMIN_ACCESS_TOKEN_COOKIE_NAME);
  }

  /**
   * Authenticate a mobile edition client. Returns a Bearer access token in JSON and never sets a
   * cookie. Requires at least one accessible ED application.
   */
  @PostMapping("/mobile")
  @SecurityRequirements
  public ResponseEntity<?> authenticateMobile(
      @Valid @RequestBody UserPasswordAuthenticationRequest body, HttpServletRequest request) {
    Authentication authentication =
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(body.getUsername(), body.getPassword()));
    if (!authentication.isAuthenticated()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .contentType(MediaType.APPLICATION_PROBLEM_JSON)
          .body(responseWriter.problem(request, HttpStatus.UNAUTHORIZED));
    }

    Optional<User> user = userRepository.findByUsername(body.getUsername());
    if (user.isEmpty()) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    if (!mobileEditionAccessService.hasAccessibleEditionApplication(body.getUsername())) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .header(HttpHeaders.CACHE_CONTROL, "no-store")
          .header("Pragma", "no-cache")
          .contentType(MediaType.APPLICATION_PROBLEM_JSON)
          .body(responseWriter.problem(request, HttpStatus.FORBIDDEN));
    }

    String token =
        jsonWebTokenService.generateEditionAccessToken(
            body.getUsername(), user.get().getLastPasswordChange());
    long expiresIn = jsonWebTokenService.getMobileTokenValidityMillis() / 1000L;

    return ResponseEntity.status(HttpStatus.OK)
        .header(HttpHeaders.CACHE_CONTROL, "no-store")
        .header("Pragma", "no-cache")
        .contentType(MediaType.APPLICATION_JSON)
        .body(MobileAuthenticationResponse.bearer(token, expiresIn));
  }

  @PostMapping("/proxy")
  public ResponseEntity<AuthenticationResponse> authenticateProxy(Authentication authentication) {
    final String username = authentication.getName();
    Optional<User> user = userRepository.findByUsername(username);
    Date lastPasswordChange = user.map(User::getLastPasswordChange).orElse(null);

    final String shortLivedToken =
        SecurityRole.isMobileEdition()
            ? jsonWebTokenService.generateMobileProxyToken(username, lastPasswordChange)
            : jsonWebTokenService.generateToken(username, new Date(), validity);

    AuthenticationResponse authResponse = new AuthenticationResponse();
    authResponse.setProxyToken(shortLivedToken);

    return ResponseEntity.status(HttpStatus.OK).body(authResponse);
  }

  /**
   * Logout. Clears the cookie selected by {@code X-SITMUN-Client} header (admin → {@value
   * #ADMIN_ACCESS_TOKEN_COOKIE_NAME}; absent → {@value #VIEWER_ACCESS_TOKEN_COOKIE_NAME}). Also
   * expires the legacy {@value #ACCESS_TOKEN_COOKIE_NAME} cookie.
   */
  @PostMapping("/logout")
  public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
    String cookieName = resolveSessionCookieName(request);
    cookieService.clearCookieByName(cookieName, request, response);
    cookieService.expireLegacyCookie(request, response);
    return ResponseEntity.status(HttpStatus.OK).build();
  }

  /**
   * Resolves the session cookie name from the {@code X-SITMUN-Client} header. Admin header → admin
   * cookie; absent or unknown → viewer cookie.
   */
  public static String resolveSessionCookieName(HttpServletRequest request) {
    String clientHeader = request.getHeader(SitmunClientTypes.HEADER_NAME);
    if (StringUtils.hasText(clientHeader) && OidcClientTypes.ADMIN.equals(clientHeader.trim())) {
      return ADMIN_ACCESS_TOKEN_COOKIE_NAME;
    }
    return VIEWER_ACCESS_TOKEN_COOKIE_NAME;
  }

  private ResponseEntity<?> authenticate(
      UserPasswordAuthenticationRequest body,
      HttpServletRequest request,
      HttpServletResponse response,
      String cookieName) {
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

      final Cookie cookie = new Cookie(cookieName, token);
      cookieService.customizeAccessTokenCookie(cookie, request.isSecure(), null);
      response.addCookie(cookie);

      cookieService.expireLegacyCookie(request, response);
      return ResponseEntity.status(HttpStatus.OK).build();
    }
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(responseWriter.problem(request, HttpStatus.UNAUTHORIZED));
  }
}
