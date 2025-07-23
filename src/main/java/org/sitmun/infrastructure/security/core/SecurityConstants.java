package org.sitmun.infrastructure.security.core;

public class SecurityConstants {
  private SecurityConstants() {
    // Private constructor to prevent instantiation
  }

  public static final String PUBLIC_KEY = "anonymous";
  public static final String PUBLIC_PRINCIPAL = "public";
  public static final String PROXY_MIDDLEWARE_PRINCIPAL = "middleware";
  public static final String PROXY_MIDDLEWARE_KEY = "X-SITMUN-Proxy-Key";

  public static boolean isPublicPrincipal(String string) {
    return PUBLIC_PRINCIPAL.equals(string);
  }
}
