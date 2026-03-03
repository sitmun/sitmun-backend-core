package org.sitmun.authentication;

/**
 * Auth provider identifiers returned by GET /api/auth/enabled-methods. LDAP and OIDC ids match
 * profile names; see {@link org.sitmun.infrastructure.config.Profiles}.
 */
public final class AuthProviderIds {

  public static final String DATABASE = "database";

  private AuthProviderIds() {}
}
