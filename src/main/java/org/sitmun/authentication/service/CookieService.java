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

  /** Expires the session JWT cookie (same attributes as login/logout). */
  public void clearAccessTokenCookie(HttpServletRequest request, HttpServletResponse response) {
    Cookie cookie = new Cookie(AuthenticationController.ACCESS_TOKEN_COOKIE_NAME, null);
    customizeAccessTokenCookie(cookie, request.isSecure(), 0);
    response.addCookie(cookie);
  }
}
