package org.sitmun.infrastructure.security.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.sitmun.infrastructure.web.dto.ProblemDetail;
import org.sitmun.infrastructure.web.dto.ProblemTypes;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/** Writes generic RFC 9457 authentication and authorization failures. */
@Component
public class Rfc9457ResponseWriter {

  private final ObjectMapper objectMapper;

  public Rfc9457ResponseWriter(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public void writeUnauthorized(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    write(request, response, HttpStatus.UNAUTHORIZED);
  }

  public void writeForbidden(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    write(request, response, HttpStatus.FORBIDDEN);
  }

  public void writeServiceUnavailable(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    write(request, response, HttpStatus.SERVICE_UNAVAILABLE);
  }

  public void writeInternalServerError(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    write(request, response, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  private void write(
      HttpServletRequest request, HttpServletResponse response, HttpStatus httpStatus)
      throws IOException {
    response.setStatus(httpStatus.value());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    objectMapper.writeValue(response.getOutputStream(), problem(request, httpStatus));
  }

  public ProblemDetail problem(HttpServletRequest request, HttpStatus httpStatus) {
    ProblemDescription description = description(httpStatus);
    return ProblemDetail.builder()
        .type(description.type())
        .status(httpStatus.value())
        .title(httpStatus.getReasonPhrase())
        .detail(description.detail())
        .instance(request.getRequestURI())
        .build();
  }

  private static ProblemDescription description(HttpStatus httpStatus) {
    return switch (httpStatus) {
      case UNAUTHORIZED ->
          new ProblemDescription(ProblemTypes.UNAUTHORIZED, "Authentication is required");
      case FORBIDDEN -> new ProblemDescription(ProblemTypes.FORBIDDEN, "Access is denied");
      case SERVICE_UNAVAILABLE ->
          new ProblemDescription(
              ProblemTypes.SERVICE_UNAVAILABLE, "Authentication service is unavailable");
      case INTERNAL_SERVER_ERROR ->
          new ProblemDescription(
              ProblemTypes.INTERNAL_SERVER_ERROR, "Authentication processing failed");
      default -> throw new IllegalArgumentException("Unsupported generic problem status");
    };
  }

  private record ProblemDescription(String type, String detail) {}
}
