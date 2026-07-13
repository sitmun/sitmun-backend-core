package org.sitmun.authentication.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.sitmun.authentication.controller.AuthenticationController;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CookieService {

  @Value("${sitmun.authentication.http-only-cookie:true}")
  private Boolean tokenCookieHttpOnly;

  @Value("${sitmun.user.token-validity-in-milliseconds:36000000}")
  private int tokenValidityInMillis;

  @Value("${sitmun.authentication.same-site-cookie:Strict}")
  private String sameSiteCookie;

  public void customizeAccessTokenCookie(Cookie cookie, boolean isSecure, Integer maxAge) {
    addCookieConfig(cookie, isSecure, maxAge != null ? maxAge : tokenValidityInMillis / 1000);
  }

  public void addCookieConfig(Cookie cookie, boolean isSecure, int maxAge) {
    cookie.setHttpOnly(tokenCookieHttpOnly);
    cookie.setSecure(isSecure);
    cookie.setPath("/");
    cookie.setAttribute("SameSite", sameSiteCookie);
    cookie.setMaxAge(maxAge);
  }

  /** Expires a cookie by name with the same security attributes as the session cookies. */
  public void clearCookieByName(
      String name, HttpServletRequest request, HttpServletResponse response) {
    Cookie cookie = new Cookie(name, null);
    customizeAccessTokenCookie(cookie, request.isSecure(), 0);
    response.addCookie(cookie);
  }

  /**
   * Expires the viewer session cookie. Convenience for call sites that handle viewer-only paths
   * with no client-selector header.
   */
  public void clearAccessTokenCookie(HttpServletRequest request, HttpServletResponse response) {
    clearCookieByName(AuthenticationController.VIEWER_ACCESS_TOKEN_COOKIE_NAME, request, response);
  }

  /**
   * Expires the legacy {@value AuthenticationController#ACCESS_TOKEN_COOKIE_NAME} cookie. Called on
   * every login and logout to force re-authentication from pre-migration sessions.
   */
  public void expireLegacyCookie(HttpServletRequest request, HttpServletResponse response) {
    clearCookieByName(AuthenticationController.ACCESS_TOKEN_COOKIE_NAME, request, response);
  }
}
