package org.sitmun.authentication.handler;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sitmun.authentication.service.OidcRedirectService;
import org.sitmun.domain.user.User;
import org.sitmun.domain.user.UserRepository;
import org.sitmun.infrastructure.config.Profiles;
import org.sitmun.infrastructure.security.service.JsonWebTokenService;
import org.springframework.beans.factory.annotation.Value;
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

  public static final String OIDC_TOKEN_COOKIE_NAME = "oidc_token";

  private final UserRepository userRepository;
  private final OidcRedirectService redirectService;
  private final UserDetailsService userDetailsService;
  private final JsonWebTokenService jsonWebTokenService;

  /**
   * Controls the {@code HttpOnly} flag on the {@code oidc_token} cookie. When {@code false}
   * (default), the cookie is accessible to frontend JavaScript via {@code document.cookie} /
   * cookie-service libraries. When {@code true}, the browser hides the cookie from JavaScript (XSS
   * mitigation), but current frontends cannot read the token. A future improvement will replace
   * cookie transfer with a URL fragment, making {@code true} the safe default.
   *
   * @see <a href="README.md">OIDC Configuration — Current limitation and future improvement</a>
   */
  @Value("${sitmun.authentication.oidc.http-only-cookie:false}")
  private Boolean oidcCookieHttpOnly;

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

      final Cookie jwtCookie = new Cookie(OIDC_TOKEN_COOKIE_NAME, jwtToken);
      jwtCookie.setHttpOnly(oidcCookieHttpOnly);
      jwtCookie.setSecure(request.isSecure());
      jwtCookie.setPath("/");
      jwtCookie.setMaxAge(3600);

      response.addCookie(jwtCookie);
    } catch (Exception e) {
      log.error("OIDC authentication processing failed", e);
      log.error("Error message: {}", e.getMessage());
    } finally {
      getRedirectStrategy().sendRedirect(request, response, frontendRedirectUrl);
    }
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
