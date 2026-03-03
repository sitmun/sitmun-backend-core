package org.sitmun.authentication.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sitmun.authentication.service.OidcRedirectService;
import org.sitmun.infrastructure.config.Profiles;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile(Profiles.OIDC)
@RequiredArgsConstructor
public class OidcAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

  private final OidcRedirectService redirectService;

  @Override
  public void onAuthenticationFailure(
      HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
      throws IOException {

    log.error("OIDC authentication failure", exception);
    log.error("Exception message: {}", exception.getMessage());

    getRedirectStrategy()
        .sendRedirect(request, response, redirectService.selectRedirectUrl(request));
  }
}
