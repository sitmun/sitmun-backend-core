package org.sitmun.infrastructure.security.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sitmun.infrastructure.web.dto.ProblemTypes;
import org.springframework.http.MediaType;
import org.springframework.mock.web.DelegatingServletOutputStream;
import org.springframework.security.authentication.BadCredentialsException;

@ExtendWith(MockitoExtension.class)
class SecurityEntryPointTest {

  @Mock private jakarta.servlet.http.HttpServletRequest request;
  @Mock private jakarta.servlet.http.HttpServletResponse response;

  @Test
  void returnsGenericUnauthorizedProblemWithoutLeakingAuthenticationDetails() throws Exception {
    var output = new ByteArrayOutputStream();
    when(request.getRequestURI()).thenReturn("/api/config/client/application");
    when(response.getOutputStream()).thenReturn(new DelegatingServletOutputStream(output));

    new SecurityEntryPoint(new Rfc9457ResponseWriter(new ObjectMapper()))
        .commence(request, response, new BadCredentialsException("blocked account alice"));

    var problem = new ObjectMapper().readTree(output.toByteArray());
    assertThat(problem.get("type").textValue()).isEqualTo(ProblemTypes.UNAUTHORIZED);
    assertThat(problem.get("status").intValue()).isEqualTo(401);
    assertThat(problem.get("title").textValue()).isEqualTo("Unauthorized");
    assertThat(problem.get("detail").textValue()).isEqualTo("Authentication is required");
    assertThat(problem.get("instance").textValue()).isEqualTo("/api/config/client/application");
    org.mockito.Mockito.verify(response).setStatus(401);
    org.mockito.Mockito.verify(response).setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
  }
}
