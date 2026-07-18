package org.sitmun.infrastructure.security.jwt;

/** Claim names and values for mobile edition and mobile-derived proxy JWTs. */
public final class MobileJwtClaims {

  public static final String AUDIENCE = "aud";
  public static final String TOKEN_USE = "token_use";
  public static final String SCOPE = "scope";
  public static final String APPLICATION_TYPES = "application_types";
  public static final String LAST_PASSWORD_CHANGE = "lastPasswordChange";

  public static final String AUDIENCE_MOBILE_API = "sitmun-mobile-api";
  public static final String AUDIENCE_PROXY = "sitmun-proxy";

  public static final String TOKEN_USE_EDITION_ACCESS = "edition_access";
  public static final String TOKEN_USE_MOBILE_PROXY_ACCESS = "mobile_proxy_access";

  public static final String APPLICATION_TYPE_EDITION = "ED";

  private MobileJwtClaims() {}
}
