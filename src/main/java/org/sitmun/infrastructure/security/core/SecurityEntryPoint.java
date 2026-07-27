package org.sitmun.infrastructure.security.core;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * Authentication entry point that returns RFC 9457 Problem Detail responses for authentication
 * failures.
 *
 * <p>This is invoked when a user tries to access a protected resource without proper
 * authentication.
 */
@Component
public class SecurityEntryPoint implements AuthenticationEntryPoint {

  private static final Logger logger = LoggerFactory.getLogger(SecurityEntryPoint.class);
  private final Rfc9457ResponseWriter responseWriter;

  public SecurityEntryPoint(Rfc9457ResponseWriter responseWriter) {
    this.responseWriter = responseWriter;
  }

  @Override
  public void commence(
      HttpServletRequest httpServletRequest,
      HttpServletResponse httpServletResponse,
      AuthenticationException authException)
      throws IOException {
    logger.error("Unauthorized error: {}", authException.getMessage());
    responseWriter.writeUnauthorized(httpServletRequest, httpServletResponse);
  }
}
