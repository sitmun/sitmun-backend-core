package org.sitmun.infrastructure.security.core;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/** Writes authorization failures produced by the Spring Security filter chain. */
@Component
public class SecurityAccessDeniedHandler implements AccessDeniedHandler {

  private final Rfc9457ResponseWriter responseWriter;

  public SecurityAccessDeniedHandler(Rfc9457ResponseWriter responseWriter) {
    this.responseWriter = responseWriter;
  }

  @Override
  public void handle(
      HttpServletRequest request,
      HttpServletResponse response,
      AccessDeniedException accessDeniedException)
      throws IOException, ServletException {
    responseWriter.writeForbidden(request, response);
  }
}
