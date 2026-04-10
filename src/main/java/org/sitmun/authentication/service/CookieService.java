package org.sitmun.authentication.service;

import jakarta.servlet.http.Cookie;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CookieService {

  @Value("${sitmun.authentication.http-only-cookie:false}")
  private Boolean tokenCookieHttpOnly;

  @Value("${sitmun.user.token-validity-in-milliseconds:36000000}")
  private int tokenValidityInMillis;

  @Value("${sitmun.authentication.same-site-cookie:Strict}")
  private String sameSiteCookie;

  public void customizeAccessTokenCookie(Cookie cookie, boolean isSecure, Integer maxAge) {
    addCookieConfig(
        cookie, isSecure, maxAge != null ? maxAge : tokenValidityInMillis / 1000);
  }

  public void addCookieConfig(Cookie cookie, boolean isSecure, int maxAge) {
    cookie.setHttpOnly(tokenCookieHttpOnly);
    cookie.setSecure(isSecure);
    cookie.setPath("/");
    cookie.setAttribute("SameSite", sameSiteCookie);
    cookie.setMaxAge(maxAge);
  }
}
