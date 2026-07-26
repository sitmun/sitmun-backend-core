package org.sitmun.authentication;

/**
 * HTTP request header for identifying the SITMUN client application type. Admin sends {@value
 * #HEADER_NAME}: {@code admin} on every backend request; viewer omits the header (treated as
 * default). Cookie selection and targeted cookie cleanup are based on this header.
 *
 * <p>Client type string values are shared with {@link OidcClientTypes}.
 */
public final class SitmunClientTypes {

  /** HTTP header name sent by the admin frontend on every backend API request. */
  public static final String HEADER_NAME = "X-SITMUN-Client";

  private SitmunClientTypes() {}
}
