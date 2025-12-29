package org.sitmun.infrastructure.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.sitmun.infrastructure.web.dto.ProblemDetail;
import org.sitmun.infrastructure.web.dto.ProblemTypes;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Handles authentication exceptions that occur during request processing.
 *
 * <p>Returns RFC 9457 Problem Detail responses for authentication failures.
 */
@ControllerAdvice
public class AccessDeniedHandler {

  private static final ObjectMapper mapper = new ObjectMapper();

  /**
   * Handles {@link AuthenticationException} - authentication failures during request processing.
   */
  @ExceptionHandler(AuthenticationException.class)
  public void handleAccessDeniedException(
      AuthenticationException ex, HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    response.setStatus(HttpStatus.UNAUTHORIZED.value());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

    ProblemDetail problem =
        ProblemDetail.builder()
            .type(ProblemTypes.UNAUTHORIZED)
            .status(HttpStatus.UNAUTHORIZED.value())
            .title("Unauthorized")
            .detail(ex.getMessage() != null ? ex.getMessage() : "Authentication is required")
            .instance(request.getRequestURI())
            .build();

    mapper.writeValue(response.getOutputStream(), problem);
  }
}
