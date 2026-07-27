package org.sitmun.infrastructure.security.core;

public class SecurityConstants {
  private SecurityConstants() {}

  public static final String PUBLIC_KEY = "anonymous";
  public static final String PUBLIC_PRINCIPAL = "public";
  public static final String BUILT_IN_ADMIN_PRINCIPAL = "admin";
  public static final String PROXY_MIDDLEWARE_PRINCIPAL = "middleware";
  public static final String PROXY_MIDDLEWARE_KEY = "X-SITMUN-Proxy-Key";

  public static boolean isPublicPrincipal(String string) {
    return PUBLIC_PRINCIPAL.equals(string);
  }

  public static boolean isBuiltInAdminPrincipal(String string) {
    return BUILT_IN_ADMIN_PRINCIPAL.equals(string);
  }

  /** True for either built-in principal (public or admin). */
  public static boolean isBuiltInPrincipal(String string) {
    return isPublicPrincipal(string) || isBuiltInAdminPrincipal(string);
  }
}
