package org.sitmun.infrastructure.security.jwt;

import java.util.List;

/** Scopes embedded in mobile edition and mobile-derived proxy JWTs. */
public final class MobileTokenScopes {

  public static final String CLIENT_APPLICATIONS_READ = "client:applications:read";
  public static final String CLIENT_TERRITORIES_READ = "client:territories:read";
  public static final String CLIENT_PROFILES_READ = "client:profiles:read";
  public static final String PROXY_TOKEN_ISSUE = "proxy:token:issue";

  public static final String PROXY_REQUEST = "proxy:request";
  public static final String MBTILES_ESTIMATE = "mbtiles:estimate";
  public static final String MBTILES_CREATE = "mbtiles:create";
  public static final String MBTILES_READ = "mbtiles:read";

  public static final List<String> EDITION_ACCESS =
      List.of(
          CLIENT_APPLICATIONS_READ,
          CLIENT_TERRITORIES_READ,
          CLIENT_PROFILES_READ,
          PROXY_TOKEN_ISSUE);

  public static final List<String> MOBILE_PROXY_ACCESS =
      List.of(PROXY_REQUEST, MBTILES_ESTIMATE, MBTILES_CREATE, MBTILES_READ);

  private MobileTokenScopes() {}

  public static String springAuthority(String scope) {
    return "SCOPE_" + scope;
  }

  public static String forMbtilesAction(String action) {
    return switch (action) {
      case "estimate" -> MBTILES_ESTIMATE;
      case "create" -> MBTILES_CREATE;
      case "status", "file" -> MBTILES_READ;
      default -> null;
    };
  }
}
