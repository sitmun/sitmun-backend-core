package org.sitmun.authentication.service;

import jakarta.servlet.http.Cookie;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CookieService {

  @Value("${sitmun.authentication.http-only-cookie:false}")
  private Boolean tokenCookieHttpOnly;

  @Value("${sitmun.authentication.access-token-max-age:3600}")
  private int accessTokenMaxAge;

  @Value("${sitmun.authentication.same-site-cookie:Strict}")
  private String sameSiteCookie;

  public static final String OIDC_TOKEN_COOKIE_NAME = "jwt_token";

  public Cookie createJwtCookie(String jwtToken, boolean isSecure) {
    final Cookie jwtCookie = new Cookie(OIDC_TOKEN_COOKIE_NAME, jwtToken);
    jwtCookie.setHttpOnly(tokenCookieHttpOnly);
    jwtCookie.setSecure(isSecure);
    jwtCookie.setPath("/");
    jwtCookie.setMaxAge(accessTokenMaxAge);
    jwtCookie.setAttribute("SameSite", sameSiteCookie);

    return jwtCookie;
  }
}
