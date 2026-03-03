package org.sitmun.authentication.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.sitmun.authentication.OidcClientTypes;
import org.sitmun.infrastructure.config.Profiles;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile(Profiles.OIDC)
public class OidcRedirectService {
  public static final String CLIENT_TYPE = "OAUTH2_CLIENT_TYPE";

  @Value(
      "${sitmun.authentication.oidc.frontend-redirect-url:http://localhost:9000/viewer/callback}")
  private String defaultUrl;

  @Value(
      "${sitmun.authentication.oidc.frontend-redirect-url-admin:http://localhost:9000/admin/#/callback}")
  private String adminUrl;

  @Value(
      "${sitmun.authentication.oidc.frontend-redirect-url-viewer:http://localhost:9000/viewer/callback}")
  private String viewerUrl;

  public String selectRedirectUrl(HttpServletRequest request) {
    final HttpSession session = request.getSession(false);
    final Object clientType = session != null ? session.getAttribute(CLIENT_TYPE) : null;

    return switch (String.valueOf(clientType)) {
      case OidcClientTypes.ADMIN -> adminUrl;
      case OidcClientTypes.VIEWER -> viewerUrl;
      default -> defaultUrl;
    };
  }
}
