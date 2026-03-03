package org.sitmun.authentication;

/**
 * Client type values for the {@code client_type} query parameter in the OIDC flow. Must match the
 * frontend constants (e.g. viewer app's {@code OIDC_CLIENT_TYPE_VIEWER}).
 */
public final class OidcClientTypes {

  /** Query parameter name (e.g. {@code ?client_type=viewer}). */
  public static final String QUERY_PARAM_NAME = "client_type";

  public static final String ADMIN = "admin";
  public static final String VIEWER = "viewer";

  private OidcClientTypes() {}
}
