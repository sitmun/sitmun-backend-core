package org.sitmun.infrastructure.security.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.sitmun.infrastructure.web.dto.ProblemDetail;
import org.sitmun.infrastructure.web.dto.ProblemTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
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
  private static final ObjectMapper mapper = new ObjectMapper();

  @Override
  public void commence(
      HttpServletRequest httpServletRequest,
      HttpServletResponse httpServletResponse,
      AuthenticationException authException)
      throws IOException {
    logger.error("Unauthorized error: {}", authException.getMessage());

    httpServletResponse.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    httpServletResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

    ProblemDetail problem =
        ProblemDetail.builder()
            .type(ProblemTypes.UNAUTHORIZED)
            .status(HttpServletResponse.SC_UNAUTHORIZED)
            .title("Unauthorized")
            .detail(authException.getMessage())
            .instance(httpServletRequest.getRequestURI())
            .build();

    mapper.writeValue(httpServletResponse.getOutputStream(), problem);
  }
}
