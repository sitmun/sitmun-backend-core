package org.sitmun.authentication.handler;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sitmun.authentication.OidcClientTypes;
import org.sitmun.authentication.controller.AuthenticationController;
import org.sitmun.authentication.service.CookieService;
import org.sitmun.authentication.service.OidcRedirectService;
import org.sitmun.domain.user.User;
import org.sitmun.domain.user.UserRepository;
import org.sitmun.infrastructure.config.Profiles;
import org.sitmun.infrastructure.security.service.JsonWebTokenService;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Handler for successful OIDC authentication. Obtains user from database and generates JWT token.
 */
@Slf4j
@Profile(Profiles.OIDC)
@Component
@RequiredArgsConstructor
public class OidcAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

  private final UserRepository userRepository;
  private final OidcRedirectService redirectService;
  private final UserDetailsService userDetailsService;
  private final JsonWebTokenService jsonWebTokenService;
  private final CookieService cookieService;

  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication)
      throws IOException {
    final String frontendRedirectUrl = redirectService.selectRedirectUrl(request);

    try {
      final OidcUser oidcUser = getOidcUser(authentication);

      String username = oidcUser.getPreferredUsername();
      if (!StringUtils.hasText(username)) {
        username = oidcUser.getSubject();
      }

      log.info("OIDC authentication for user: {}", username);

      final User user =
          userRepository
              .findByUsername(username)
              .orElseThrow(() -> new RuntimeException("User not found in database"));
      final UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
      final String jwtToken =
          jsonWebTokenService.generateToken(userDetails, user.getLastPasswordChange());

      String cookieName = resolveOidcCookieName(request);
      final Cookie cookie = new Cookie(cookieName, jwtToken);
      cookieService.customizeAccessTokenCookie(cookie, request.isSecure(), null);
      response.addCookie(cookie);
      cookieService.expireLegacyCookie(request, response);
    } catch (Exception e) {
      log.error("OIDC authentication processing failed", e);
      log.error("Error message: {}", e.getMessage());
    } finally {
      getRedirectStrategy().sendRedirect(request, response, frontendRedirectUrl);
    }
  }

  private String resolveOidcCookieName(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    Object clientType =
        session != null ? session.getAttribute(OidcRedirectService.CLIENT_TYPE) : null;
    return OidcClientTypes.ADMIN.equals(String.valueOf(clientType))
        ? AuthenticationController.ADMIN_ACCESS_TOKEN_COOKIE_NAME
        : AuthenticationController.VIEWER_ACCESS_TOKEN_COOKIE_NAME;
  }

  private OidcUser getOidcUser(Authentication authentication) {
    if (!(authentication instanceof OAuth2AuthenticationToken oauth2Token)) {
      throw new OAuth2AuthenticationException(
          "Invalid authentication type: " + authentication.getClass().getName());
    }

    if (!(oauth2Token.getPrincipal() instanceof OidcUser)) {
      throw new OAuth2AuthenticationException(
          "Invalid principal type: " + oauth2Token.getPrincipal().getClass().getName());
    }

    return (OidcUser) oauth2Token.getPrincipal();
  }
}
