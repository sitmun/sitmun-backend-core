package org.sitmun.infrastructure.security.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sitmun.authentication.controller.AuthenticationController;
import org.sitmun.authentication.service.CookieService;
import org.sitmun.domain.user.User;
import org.sitmun.domain.user.UserRepository;
import org.sitmun.infrastructure.security.core.Rfc9457ResponseWriter;
import org.sitmun.infrastructure.security.core.SecurityRole;
import org.sitmun.infrastructure.security.core.userdetails.UserDetailsImplementation;
import org.sitmun.infrastructure.security.service.JsonWebTokenService;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("JsonWebTokenFilter scoped cookies")
class JsonWebTokenFilterScopedCookieTest {

  private static final String USERNAME = "editor";
  private static final Date PASSWORD_CHANGE = Date.from(Instant.parse("2026-01-01T00:00:00Z"));

  @Mock private UserDetailsService userDetailsService;
  @Mock private UserRepository userRepository;
  @Mock private CookieService cookieService;
  @Mock private FilterChain filterChain;

  private JsonWebTokenService tokens;
  private JsonWebTokenFilter filter;

  @BeforeEach
  void setUp() {
    tokens = new JsonWebTokenService(Clock.systemUTC());
    ReflectionTestUtils.setField(tokens, "secret", "0123456789abcdef0123456789abcdef");
    ReflectionTestUtils.setField(tokens, "validity", 900_000);
    ReflectionTestUtils.setField(tokens, "mobileValidity", 3_600_000);
    ReflectionTestUtils.setField(tokens, "proxyValidity", 900_000);
    ReflectionTestUtils.invokeMethod(tokens, "buildKey");
    filter =
        new JsonWebTokenFilter(
            userDetailsService,
            tokens,
            userRepository,
            cookieService,
            new Rfc9457ResponseWriter(new ObjectMapper()));
    SecurityContextHolder.clearContext();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @ParameterizedTest(name = "{0} cookie for administrator={1}")
  @MethodSource("scopedCookies")
  @DisplayName("rejects a scoped mobile token presented as the session cookie")
  void rejectsScopedTokenInSessionCookie(ScopedTokenKind kind, boolean administrator)
      throws Exception {
    User user = user(administrator);
    UserDetailsImplementation details = UserDetailsImplementation.build(user);
    when(userDetailsService.loadUserByUsername(USERNAME)).thenReturn(details);
    when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setCookies(
        new Cookie(AuthenticationController.VIEWER_ACCESS_TOKEN_COOKIE_NAME, scopedToken(kind)));
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, filterChain);

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    assertThat(authentication).isNull();
    assertThat(response.getStatus()).isEqualTo(401);
    verify(filterChain, never()).doFilter(request, response);
  }

  @Test
  @DisplayName("accepts a session token and installs the user's database roles")
  void acceptsSessionToken() throws Exception {
    User user = user(true);
    UserDetailsImplementation details = UserDetailsImplementation.build(user);
    when(userDetailsService.loadUserByUsername(USERNAME)).thenReturn(details);
    when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setCookies(
        new Cookie(
            AuthenticationController.VIEWER_ACCESS_TOKEN_COOKIE_NAME,
            tokens.generateToken(details, PASSWORD_CHANGE)));
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, filterChain);

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    assertThat(authentication).isNotNull();
    assertThat(authentication.getAuthorities())
        .extracting("authority")
        .contains(SecurityRole.ADMIN.authority(), SecurityRole.USER.authority());
    assertThat(response.getStatus()).isEqualTo(200);
    verify(filterChain).doFilter(request, response);
  }

  private String scopedToken(ScopedTokenKind kind) {
    return switch (kind) {
      case EDITION_ACCESS -> tokens.generateEditionAccessToken(USERNAME, PASSWORD_CHANGE);
      case MOBILE_PROXY_ACCESS -> tokens.generateMobileProxyToken(USERNAME, PASSWORD_CHANGE);
    };
  }

  private static User user(boolean administrator) {
    return User.builder()
        .id(1)
        .username(USERNAME)
        .password("secret")
        .blocked(false)
        .administrator(administrator)
        .lastPasswordChange(PASSWORD_CHANGE)
        .build();
  }

  static Stream<Arguments> scopedCookies() {
    return Stream.of(
        Arguments.of(ScopedTokenKind.EDITION_ACCESS, true),
        Arguments.of(ScopedTokenKind.EDITION_ACCESS, false),
        Arguments.of(ScopedTokenKind.MOBILE_PROXY_ACCESS, true),
        Arguments.of(ScopedTokenKind.MOBILE_PROXY_ACCESS, false));
  }

  enum ScopedTokenKind {
    EDITION_ACCESS,
    MOBILE_PROXY_ACCESS
  }
}
