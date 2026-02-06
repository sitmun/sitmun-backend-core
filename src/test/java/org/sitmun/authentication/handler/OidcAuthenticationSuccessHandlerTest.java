package org.sitmun.authentication.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.Cookie;
import java.util.Date;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sitmun.authentication.service.OidcRedirectService;
import org.sitmun.domain.user.User;
import org.sitmun.domain.user.UserRepository;
import org.sitmun.infrastructure.security.service.JsonWebTokenService;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("OidcAuthenticationSuccessHandler unit tests")
class OidcAuthenticationSuccessHandlerTest {

  private static final String REDIRECT_URL = "http://localhost:4200/callback";
  private static final String JWT_TOKEN = "test-jwt-token";

  @Mock private UserRepository userRepository;

  @Mock private OidcRedirectService redirectService;

  @Mock private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

  @Mock private JsonWebTokenService jsonWebTokenService;

  private OidcAuthenticationSuccessHandler handler;

  @BeforeEach
  void setUp() {
    handler =
        new OidcAuthenticationSuccessHandler(
            userRepository, redirectService, userDetailsService, jsonWebTokenService);
    ReflectionTestUtils.setField(handler, "oidcCookieHttpOnly", false);
  }

  private static OAuth2AuthenticationToken oauth2TokenWithOidcUser(
      String preferredUsername, String subject) {
    OidcUser oidcUser = org.mockito.Mockito.mock(OidcUser.class);
    when(oidcUser.getPreferredUsername()).thenReturn(preferredUsername);
    lenient().when(oidcUser.getSubject()).thenReturn(subject);
    OAuth2AuthenticationToken token = org.mockito.Mockito.mock(OAuth2AuthenticationToken.class);
    when(token.getPrincipal()).thenReturn(oidcUser);
    return token;
  }

  @Test
  @DisplayName("happy path: sets cookie and redirects")
  void happyPath_setsCookieAndRedirects() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    when(redirectService.selectRedirectUrl(request)).thenReturn(REDIRECT_URL);
    OAuth2AuthenticationToken token = oauth2TokenWithOidcUser("testuser", "sub-123");
    User user =
        User.builder()
            .username("testuser")
            .administrator(false)
            .lastPasswordChange(new Date())
            .build();
    UserDetails userDetails = org.mockito.Mockito.mock(UserDetails.class);
    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
    when(userDetailsService.loadUserByUsername("testuser")).thenReturn(userDetails);
    when(jsonWebTokenService.generateToken(any(UserDetails.class), any(Date.class)))
        .thenReturn(JWT_TOKEN);

    handler.onAuthenticationSuccess(request, response, token);

    assertThat(response.getRedirectedUrl()).isEqualTo(REDIRECT_URL);
    Cookie[] cookies = response.getCookies();
    assertThat(cookies).isNotNull().hasSize(1);
    assertThat(cookies[0].getName())
        .isEqualTo(OidcAuthenticationSuccessHandler.OIDC_TOKEN_COOKIE_NAME);
    assertThat(cookies[0].getValue()).isEqualTo(JWT_TOKEN);
    assertThat(cookies[0].isHttpOnly()).isFalse();
    assertThat(cookies[0].getPath()).isEqualTo("/");
    assertThat(cookies[0].getMaxAge()).isEqualTo(3600);
  }

  @Test
  @DisplayName("falls back to subject when preferredUsername empty")
  void fallsBackToSubject_whenPreferredUsernameEmpty() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    when(redirectService.selectRedirectUrl(request)).thenReturn(REDIRECT_URL);
    OAuth2AuthenticationToken token = oauth2TokenWithOidcUser(null, "sub-123");
    User user = User.builder().username("sub-123").administrator(false).build();
    UserDetails userDetails = org.mockito.Mockito.mock(UserDetails.class);
    when(userRepository.findByUsername("sub-123")).thenReturn(Optional.of(user));
    when(userDetailsService.loadUserByUsername("sub-123")).thenReturn(userDetails);
    when(jsonWebTokenService.generateToken(any(UserDetails.class), any())).thenReturn(JWT_TOKEN);

    handler.onAuthenticationSuccess(request, response, token);

    verify(userRepository).findByUsername("sub-123");
    assertThat(response.getRedirectedUrl()).isEqualTo(REDIRECT_URL);
  }

  @Test
  @DisplayName("user not found: still redirects, no cookie")
  void userNotFound_stillRedirects_noCookie() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    when(redirectService.selectRedirectUrl(request)).thenReturn(REDIRECT_URL);
    OAuth2AuthenticationToken token = oauth2TokenWithOidcUser("unknown", "sub-123");
    when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

    handler.onAuthenticationSuccess(request, response, token);

    assertThat(response.getRedirectedUrl()).isEqualTo(REDIRECT_URL);
    assertThat(response.getCookies()).isEmpty();
  }

  @Test
  @DisplayName("invalid auth type: still redirects, no cookie")
  void invalidAuthType_stillRedirects_noCookie() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    when(redirectService.selectRedirectUrl(request)).thenReturn(REDIRECT_URL);
    org.springframework.security.core.Authentication auth =
        org.mockito.Mockito.mock(org.springframework.security.core.Authentication.class);

    handler.onAuthenticationSuccess(request, response, auth);

    assertThat(response.getRedirectedUrl()).isEqualTo(REDIRECT_URL);
    assertThat(response.getCookies()).isEmpty();
  }

  @Test
  @DisplayName("invalid principal type: still redirects, no cookie")
  void invalidPrincipalType_stillRedirects_noCookie() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    when(redirectService.selectRedirectUrl(request)).thenReturn(REDIRECT_URL);
    OAuth2AuthenticationToken token = org.mockito.Mockito.mock(OAuth2AuthenticationToken.class);
    when(token.getPrincipal()).thenReturn(org.mockito.Mockito.mock(OAuth2User.class));

    handler.onAuthenticationSuccess(request, response, token);

    assertThat(response.getRedirectedUrl()).isEqualTo(REDIRECT_URL);
    assertThat(response.getCookies()).isEmpty();
  }

  @Test
  @DisplayName("cookie httpOnly flag respects config")
  void cookieHttpOnlyFlag_respectsConfig() throws Exception {
    ReflectionTestUtils.setField(handler, "oidcCookieHttpOnly", true);
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    when(redirectService.selectRedirectUrl(request)).thenReturn(REDIRECT_URL);
    OAuth2AuthenticationToken token = oauth2TokenWithOidcUser("testuser", "sub-123");
    User user =
        User.builder()
            .username("testuser")
            .administrator(false)
            .lastPasswordChange(new Date())
            .build();
    UserDetails userDetails = org.mockito.Mockito.mock(UserDetails.class);
    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
    when(userDetailsService.loadUserByUsername("testuser")).thenReturn(userDetails);
    when(jsonWebTokenService.generateToken(any(UserDetails.class), any(Date.class)))
        .thenReturn(JWT_TOKEN);

    handler.onAuthenticationSuccess(request, response, token);

    Cookie[] cookies = response.getCookies();
    assertThat(cookies).hasSize(1);
    assertThat(cookies[0].isHttpOnly()).isTrue();
  }
}
