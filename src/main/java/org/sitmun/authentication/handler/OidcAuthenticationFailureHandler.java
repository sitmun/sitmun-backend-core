package org.sitmun.authentication.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Profile("oidc")
@Component
public class OidcAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

  @Value("${sitmun.authentication.oidc.frontend-redirect-url:http://localhost:9000/viewer/callback}")
  private String frontendRedirectUrl;

  @Override
  public void onAuthenticationFailure(
    HttpServletRequest request,
    HttpServletResponse response,
    AuthenticationException exception) throws IOException, ServletException {

    log.error("OIDC authentication failure", exception);
    log.error("Exception message: {}", exception.getMessage());

    getRedirectStrategy().sendRedirect(request, response, frontendRedirectUrl);
  }
}
