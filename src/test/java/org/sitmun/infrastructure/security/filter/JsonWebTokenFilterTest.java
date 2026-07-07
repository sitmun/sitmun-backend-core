package org.sitmun.infrastructure.security.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sitmun.authentication.controller.AuthenticationController;
import org.sitmun.authentication.service.CookieService;
import org.sitmun.domain.user.User;
import org.sitmun.domain.user.UserRepository;
import org.sitmun.infrastructure.security.core.userdetails.UserDetailsImplementation;
import org.sitmun.infrastructure.security.service.JsonWebTokenService;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;

@ExtendWith(MockitoExtension.class)
@DisplayName("JsonWebTokenFilter")
class JsonWebTokenFilterTest {

  private static final String BLOCKED_USERNAME = "blockeduser";
  private static final String ACTIVE_USERNAME = "activeuser";
  private static final String JWT = "valid.jwt.token";

  @Mock private UserDetailsService userDetailsService;
  @Mock private JsonWebTokenService jsonWebTokenService;
  @Mock private UserRepository userRepository;
  @Mock private CookieService cookieService;
  @Mock private FilterChain filterChain;

  private JsonWebTokenFilter filter;
  private MockHttpServletRequest request;
  private MockHttpServletResponse response;

  @BeforeEach
  void setUp() {
    filter =
        new JsonWebTokenFilter(
            userDetailsService, jsonWebTokenService, userRepository, cookieService);
    request = new MockHttpServletRequest();
    response = new MockHttpServletResponse();
    SecurityContextHolder.clearContext();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("continues filter chain when no JWT cookie is present")
  void continuesFilterChainWhenNoJwtCookie() throws Exception {
    filter.doFilter(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  @DisplayName("rejects blocked account JWT with 401 and does not continue filter chain")
  void rejectsBlockedAccountJwtWith401() throws Exception {
    request.setCookies(
        new jakarta.servlet.http.Cookie(AuthenticationController.ACCESS_TOKEN_COOKIE_NAME, JWT));
    when(jsonWebTokenService.getUsernameFromToken(JWT)).thenReturn(BLOCKED_USERNAME);
    UserDetailsImplementation blockedDetails =
        UserDetailsImplementation.build(
            User.builder()
                .id(1)
                .username(BLOCKED_USERNAME)
                .blocked(true)
                .administrator(false)
                .build());
    when(userDetailsService.loadUserByUsername(BLOCKED_USERNAME)).thenReturn(blockedDetails);

    filter.doFilter(request, response, filterChain);

    assertThat(response.getStatus()).isEqualTo(401);
    verify(cookieService).clearAccessTokenCookie(request, response);
    verify(filterChain, never()).doFilter(any(), any());
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  @DisplayName("continues filter chain when cookie name does not match access_token")
  void continuesFilterChainWhenCookieNameDoesNotMatch() throws Exception {
    request.setCookies(new Cookie("other_cookie", JWT));

    filter.doFilter(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    verify(jsonWebTokenService, never()).getUsernameFromToken(any());
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  @DisplayName("authenticates valid unlocked user and continues filter chain")
  void authenticatesValidUnlockedUser() throws Exception {
    request.setCookies(new Cookie(AuthenticationController.ACCESS_TOKEN_COOKIE_NAME, JWT));
    when(jsonWebTokenService.getUsernameFromToken(JWT)).thenReturn(ACTIVE_USERNAME);
    User active =
        User.builder().id(2).username(ACTIVE_USERNAME).blocked(false).administrator(false).build();
    UserDetailsImplementation activeDetails = UserDetailsImplementation.build(active);
    when(userDetailsService.loadUserByUsername(ACTIVE_USERNAME)).thenReturn(activeDetails);
    when(userRepository.findByUsername(ACTIVE_USERNAME)).thenReturn(Optional.of(active));
    when(jsonWebTokenService.validateToken(eq(JWT), eq(activeDetails), nullable(Date.class)))
        .thenReturn(true);

    filter.doFilter(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    assertThat(SecurityContextHolder.getContext().getAuthentication().getName())
        .isEqualTo(ACTIVE_USERNAME);
  }

  @Test
  @DisplayName("continues filter chain without authentication when user is not in the database")
  void continuesFilterChainWhenUserNotFound() throws Exception {
    request.setCookies(new Cookie(AuthenticationController.ACCESS_TOKEN_COOKIE_NAME, JWT));
    when(jsonWebTokenService.getUsernameFromToken(JWT)).thenReturn(ACTIVE_USERNAME);
    UserDetailsImplementation activeDetails =
        UserDetailsImplementation.build(
            User.builder()
                .id(2)
                .username(ACTIVE_USERNAME)
                .blocked(false)
                .administrator(false)
                .build());
    when(userDetailsService.loadUserByUsername(ACTIVE_USERNAME)).thenReturn(activeDetails);
    when(userRepository.findByUsername(ACTIVE_USERNAME)).thenReturn(Optional.empty());

    filter.doFilter(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    verify(cookieService).clearAccessTokenCookie(request, response);
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  @DisplayName("clears stale cookie and continues filter chain when token validation fails")
  void clearsStaleCookieWhenTokenValidationFails() throws Exception {
    request.setCookies(new Cookie(AuthenticationController.ACCESS_TOKEN_COOKIE_NAME, JWT));
    when(jsonWebTokenService.getUsernameFromToken(JWT)).thenReturn(ACTIVE_USERNAME);
    User active =
        User.builder().id(2).username(ACTIVE_USERNAME).blocked(false).administrator(false).build();
    UserDetailsImplementation activeDetails = UserDetailsImplementation.build(active);
    when(userDetailsService.loadUserByUsername(ACTIVE_USERNAME)).thenReturn(activeDetails);
    when(userRepository.findByUsername(ACTIVE_USERNAME)).thenReturn(Optional.of(active));
    when(jsonWebTokenService.validateToken(eq(JWT), eq(activeDetails), nullable(Date.class)))
        .thenReturn(false);

    filter.doFilter(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    verify(cookieService).clearAccessTokenCookie(request, response);
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  @DisplayName("clears stale cookie and continues filter chain when JWT is malformed")
  void clearsStaleCookieWhenJwtMalformed() throws Exception {
    request.setCookies(new Cookie(AuthenticationController.ACCESS_TOKEN_COOKIE_NAME, JWT));
    when(jsonWebTokenService.getUsernameFromToken(JWT))
        .thenThrow(new IllegalArgumentException("invalid token"));

    filter.doFilter(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    verify(cookieService).clearAccessTokenCookie(request, response);
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  @DisplayName("does not clear cookie when user details cannot be loaded")
  void doesNotClearCookieOnTransientUserDetailsFailure() throws Exception {
    request.setCookies(new Cookie(AuthenticationController.ACCESS_TOKEN_COOKIE_NAME, JWT));
    when(jsonWebTokenService.getUsernameFromToken(JWT)).thenReturn(ACTIVE_USERNAME);
    when(userDetailsService.loadUserByUsername(ACTIVE_USERNAME))
        .thenThrow(new RuntimeException("database unavailable"));

    filter.doFilter(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    verify(cookieService, never()).clearAccessTokenCookie(request, response);
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  @DisplayName("clears stale cookie and continues filter chain when JWT is expired")
  void clearsStaleCookieWhenJwtExpired() throws Exception {
    request.setCookies(new Cookie(AuthenticationController.ACCESS_TOKEN_COOKIE_NAME, JWT));
    when(jsonWebTokenService.getUsernameFromToken(JWT))
        .thenThrow(new ExpiredJwtException(null, null, "expired"));

    filter.doFilter(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    verify(cookieService).clearAccessTokenCookie(request, response);
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  @DisplayName("skips user details loading when authentication is already in security context")
  void skipsUserDetailsLoadingWhenAlreadyAuthenticated() throws Exception {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(ACTIVE_USERNAME, null, List.of()));
    request.setCookies(new Cookie(AuthenticationController.ACCESS_TOKEN_COOKIE_NAME, JWT));
    when(jsonWebTokenService.getUsernameFromToken(JWT)).thenReturn(ACTIVE_USERNAME);

    filter.doFilter(request, response, filterChain);

    verify(userDetailsService, never()).loadUserByUsername(any());
    verify(filterChain).doFilter(request, response);
  }
}
