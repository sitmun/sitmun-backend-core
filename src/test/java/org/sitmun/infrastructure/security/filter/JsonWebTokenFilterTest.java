package org.sitmun.infrastructure.security.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
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
import org.sitmun.infrastructure.security.core.Rfc9457ResponseWriter;
import org.sitmun.infrastructure.security.core.userdetails.UserDetailsImplementation;
import org.sitmun.infrastructure.security.service.JsonWebTokenService;
import org.sitmun.infrastructure.web.dto.ProblemTypes;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
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
            userDetailsService,
            jsonWebTokenService,
            userRepository,
            cookieService,
            new Rfc9457ResponseWriter(new ObjectMapper()));
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

    assertUnauthorizedProblem();
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
  @DisplayName("rejects empty JWT cookie with 401 and clears stale cookie")
  void rejectsEmptyJwtCookie() throws Exception {
    request.setCookies(new Cookie(AuthenticationController.ACCESS_TOKEN_COOKIE_NAME, ""));

    filter.doFilter(request, response, filterChain);

    assertUnauthorizedProblem();
    verify(filterChain, never()).doFilter(any(), any());
    verify(cookieService).clearAccessTokenCookie(request, response);
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
  @DisplayName("rejects JWT with 401 when user is not in the database")
  void rejectsJwtWhenUserNotFound() throws Exception {
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

    assertUnauthorizedProblem();
    verify(filterChain, never()).doFilter(any(), any());
    verify(cookieService).clearAccessTokenCookie(request, response);
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  @DisplayName("rejects invalid JWT with 401 and clears stale cookie")
  void rejectsJwtWhenTokenValidationFails() throws Exception {
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

    assertUnauthorizedProblem();
    verify(filterChain, never()).doFilter(any(), any());
    verify(cookieService).clearAccessTokenCookie(request, response);
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  @DisplayName("rejects malformed JWT with 401 and clears stale cookie")
  void rejectsJwtWhenMalformed() throws Exception {
    request.setCookies(new Cookie(AuthenticationController.ACCESS_TOKEN_COOKIE_NAME, JWT));
    when(jsonWebTokenService.getUsernameFromToken(JWT))
        .thenThrow(new MalformedJwtException("invalid token"));

    filter.doFilter(request, response, filterChain);

    assertUnauthorizedProblem();
    verify(filterChain, never()).doFilter(any(), any());
    verify(cookieService).clearAccessTokenCookie(request, response);
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  @DisplayName("returns 503 and preserves cookie when identity store is unavailable")
  void returnsServiceUnavailableOnTransientIdentityStoreFailure() throws Exception {
    request.setCookies(new Cookie(AuthenticationController.ACCESS_TOKEN_COOKIE_NAME, JWT));
    when(jsonWebTokenService.getUsernameFromToken(JWT)).thenReturn(ACTIVE_USERNAME);
    when(userDetailsService.loadUserByUsername(ACTIVE_USERNAME))
        .thenThrow(new DataAccessResourceFailureException("database unavailable"));

    filter.doFilter(request, response, filterChain);

    assertProblem(
        503,
        "https://sitmun.org/problems/service-unavailable",
        "Service Unavailable",
        "Authentication service is unavailable");
    verify(filterChain, never()).doFilter(any(), any());
    verify(cookieService, never()).clearAccessTokenCookie(request, response);
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  @DisplayName("returns 500 and preserves cookie on unexpected authentication failure")
  void returnsInternalServerErrorOnUnexpectedAuthenticationFailure() throws Exception {
    request.setCookies(new Cookie(AuthenticationController.ACCESS_TOKEN_COOKIE_NAME, JWT));
    when(jsonWebTokenService.getUsernameFromToken(JWT)).thenReturn(ACTIVE_USERNAME);
    when(userDetailsService.loadUserByUsername(ACTIVE_USERNAME))
        .thenThrow(new IllegalStateException("sensitive implementation detail"));

    filter.doFilter(request, response, filterChain);

    assertProblem(
        500,
        ProblemTypes.INTERNAL_SERVER_ERROR,
        "Internal Server Error",
        "Authentication processing failed");
    assertThat(response.getContentAsString()).doesNotContain("sensitive implementation detail");
    verify(filterChain, never()).doFilter(any(), any());
    verify(cookieService, never()).clearAccessTokenCookie(request, response);
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  @DisplayName("rejects expired JWT with 401 and clears stale cookie")
  void rejectsJwtWhenExpired() throws Exception {
    request.setCookies(new Cookie(AuthenticationController.ACCESS_TOKEN_COOKIE_NAME, JWT));
    when(jsonWebTokenService.getUsernameFromToken(JWT))
        .thenThrow(new ExpiredJwtException(null, null, "expired"));

    filter.doFilter(request, response, filterChain);

    assertUnauthorizedProblem();
    verify(filterChain, never()).doFilter(any(), any());
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

  private void assertUnauthorizedProblem() throws Exception {
    assertProblem(401, ProblemTypes.UNAUTHORIZED, "Unauthorized", "Authentication is required");
  }

  private void assertProblem(int status, String type, String title, String detail)
      throws Exception {
    assertThat(response.getStatus()).isEqualTo(status);
    assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    var problem = new ObjectMapper().readTree(response.getContentAsByteArray());
    assertThat(problem.get("type").textValue()).isEqualTo(type);
    assertThat(problem.get("status").intValue()).isEqualTo(status);
    assertThat(problem.get("title").textValue()).isEqualTo(title);
    assertThat(problem.get("detail").textValue()).isEqualTo(detail);
    assertThat(problem.get("instance").textValue()).isEqualTo("");
  }
}
