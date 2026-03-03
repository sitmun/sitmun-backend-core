package org.sitmun.infrastructure.config;

/**
 * Spring profile names. Use these constants instead of string literals
 * for @Profile, @ActiveProfiles, @AdditiveActiveProfiles and runtime profile checks.
 */
public final class Profiles {

  public static final String OIDC = "oidc";
  public static final String LDAP = "ldap";
  public static final String TEST = "test";
  public static final String MAIL = "mail";
  public static final String OPENAPI = "openapi";
  public static final String POSTGRES = "postgres";
  public static final String DEV = "dev";

  private Profiles() {}
}
