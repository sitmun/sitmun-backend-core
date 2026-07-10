package org.sitmun.infrastructure.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.sitmun.infrastructure.security.core.Rfc9457ResponseWriter;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/** Handles authentication failures raised inside controller methods. */
@ControllerAdvice
public class AuthenticationExceptionHandler {

  private final Rfc9457ResponseWriter responseWriter;

  public AuthenticationExceptionHandler(ObjectMapper objectMapper) {
    this.responseWriter = new Rfc9457ResponseWriter(objectMapper);
  }

  @ExceptionHandler(AuthenticationException.class)
  public void handleAuthenticationException(
      HttpServletRequest request, HttpServletResponse response) throws IOException {
    responseWriter.writeUnauthorized(request, response);
  }
}
