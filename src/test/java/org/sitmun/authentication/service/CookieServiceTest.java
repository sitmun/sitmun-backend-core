package org.sitmun.authentication.service;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.authentication.controller.AuthenticationController;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("CookieService")
class CookieServiceTest {

  private CookieService cookieService;

  @BeforeEach
  void setUp() {
    cookieService = new CookieService();
    ReflectionTestUtils.setField(cookieService, "tokenCookieHttpOnly", true);
    ReflectionTestUtils.setField(cookieService, "tokenValidityInMillis", 3_600_000);
    ReflectionTestUtils.setField(cookieService, "sameSiteCookie", "Lax");
  }

  @Test
  @DisplayName("addCookieConfig applies path, SameSite, secure, httpOnly, maxAge")
  void addCookieConfig_setsAttributes() {
    Cookie cookie = new Cookie(AuthenticationController.VIEWER_ACCESS_TOKEN_COOKIE_NAME, "v");
    cookieService.addCookieConfig(cookie, true, 120);

    assertThat(cookie.getPath()).isEqualTo("/");
    assertThat(cookie.getAttribute("SameSite")).isEqualTo("Lax");
    assertThat(cookie.getSecure()).isTrue();
    assertThat(cookie.isHttpOnly()).isTrue();
    assertThat(cookie.getMaxAge()).isEqualTo(120);
  }

  @Test
  @DisplayName("customizeAccessTokenCookie with null maxAge uses token validity seconds")
  void customizeAccessTokenCookie_nullMaxAge_usesConfiguredValidity() {
    Cookie cookie = new Cookie(AuthenticationController.VIEWER_ACCESS_TOKEN_COOKIE_NAME, "jwt");
    cookieService.customizeAccessTokenCookie(cookie, false, null);

    assertThat(cookie.getMaxAge()).isEqualTo(3600);
    assertThat(cookie.getSecure()).isFalse();
    assertThat(cookie.isHttpOnly()).isTrue();
  }

  @Test
  @DisplayName("customizeAccessTokenCookie with explicit maxAge overrides default")
  void customizeAccessTokenCookie_explicitMaxAge() {
    Cookie cookie = new Cookie(AuthenticationController.VIEWER_ACCESS_TOKEN_COOKIE_NAME, "jwt");
    cookieService.customizeAccessTokenCookie(cookie, true, 0);

    assertThat(cookie.getMaxAge()).isEqualTo(0);
    assertThat(cookie.getSecure()).isTrue();
  }

  @Test
  @DisplayName("clearCookieByName emits a cookie with maxAge=0 and the given name")
  void clearCookieByName_emitsExpiringCookieWithCorrectName() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();

    cookieService.clearCookieByName(
        AuthenticationController.ADMIN_ACCESS_TOKEN_COOKIE_NAME, request, response);

    Cookie cleared = response.getCookie(AuthenticationController.ADMIN_ACCESS_TOKEN_COOKIE_NAME);
    assertThat(cleared).isNotNull();
    assertThat(cleared.getMaxAge()).isEqualTo(0);
    assertThat(cleared.getPath()).isEqualTo("/");
  }

  @Test
  @DisplayName("clearAccessTokenCookie clears viewer_access_token")
  void clearAccessTokenCookie_clearsViewerCookie() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();

    cookieService.clearAccessTokenCookie(request, response);

    assertThat(response.getCookie(AuthenticationController.VIEWER_ACCESS_TOKEN_COOKIE_NAME))
        .isNotNull()
        .satisfies(c -> assertThat(c.getMaxAge()).isEqualTo(0));
  }

  @Test
  @DisplayName("expireLegacyCookie expires the legacy access_token cookie")
  void expireLegacyCookie_expiresLegacyCookie() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();

    cookieService.expireLegacyCookie(request, response);

    Cookie legacy = response.getCookie(AuthenticationController.ACCESS_TOKEN_COOKIE_NAME);
    assertThat(legacy).isNotNull();
    assertThat(legacy.getMaxAge()).isEqualTo(0);
  }
}
