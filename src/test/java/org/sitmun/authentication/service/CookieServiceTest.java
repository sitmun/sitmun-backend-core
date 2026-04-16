package org.sitmun.authentication.service;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.authentication.controller.AuthenticationController;
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
    Cookie cookie = new Cookie(AuthenticationController.ACCESS_TOKEN_COOKIE_NAME, "v");
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
    Cookie cookie = new Cookie(AuthenticationController.ACCESS_TOKEN_COOKIE_NAME, "jwt");
    cookieService.customizeAccessTokenCookie(cookie, false, null);

    assertThat(cookie.getMaxAge()).isEqualTo(3600);
    assertThat(cookie.getSecure()).isFalse();
    assertThat(cookie.isHttpOnly()).isTrue();
  }

  @Test
  @DisplayName("customizeAccessTokenCookie with explicit maxAge overrides default")
  void customizeAccessTokenCookie_explicitMaxAge() {
    Cookie cookie = new Cookie(AuthenticationController.ACCESS_TOKEN_COOKIE_NAME, "jwt");
    cookieService.customizeAccessTokenCookie(cookie, true, 0);

    assertThat(cookie.getMaxAge()).isEqualTo(0);
    assertThat(cookie.getSecure()).isTrue();
  }
}
