package org.sitmun.infrastructure.security.core;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.sitmun.infrastructure.web.dto.ProblemTypes;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

class SecurityAccessDeniedHandlerTest {

  @Test
  void returnsGenericForbiddenProblemWithoutLeakingAuthorizationDetails() throws Exception {
    var request = new MockHttpServletRequest("GET", "/api/admin");
    var response = new MockHttpServletResponse();
    var objectMapper = new ObjectMapper();
    var handler = new SecurityAccessDeniedHandler(new Rfc9457ResponseWriter(objectMapper));

    handler.handle(request, response, new AccessDeniedException("private role admin"));

    var problem = objectMapper.readTree(response.getContentAsByteArray());
    assertThat(response.getStatus()).isEqualTo(403);
    assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    assertThat(problem.get("type").textValue()).isEqualTo(ProblemTypes.FORBIDDEN);
    assertThat(problem.get("status").intValue()).isEqualTo(403);
    assertThat(problem.get("title").textValue()).isEqualTo("Forbidden");
    assertThat(problem.get("detail").textValue()).isEqualTo("Access is denied");
    assertThat(problem.get("instance").textValue()).isEqualTo("/api/admin");
  }
}
