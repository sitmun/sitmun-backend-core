package org.sitmun.infrastructure.security.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * HTTP security for {@code POST /api/tasks/template/more-info-advanced/render} must match {@code
 * TemplatePreviewController}'s {@code @PreAuthorize} (USER, ADMIN, PUBLIC). Seeded H2 task 42 (MIA
 * parent with ATS on territory 1) is the render target.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("More Info Advanced render HTTP security")
class MoreInfoAdvancedRenderSecurityTest {

  private static final String RENDER_URI = "/api/tasks/template/more-info-advanced/render";
  private static final String BODY =
      """
      {"appId":1,"terId":1,"miaTaskIds":[42],"parameters":{}}
      """;

  @Autowired private MockMvc mvc;

  @Test
  @DisplayName("POST render allows ROLE_ADMIN and returns seeded parent")
  @WithMockUser(username = "admin", roles = "ADMIN")
  void renderAllowsAdmin() throws Exception {
    mvc.perform(post(RENDER_URI).contentType(APPLICATION_JSON).content(BODY))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.tasks[0].taskId").value(42));
  }

  @Test
  @DisplayName("POST render allows ROLE_USER with map session coordinates")
  @WithMockUser(username = "admin", roles = "USER")
  void renderAllowsUser() throws Exception {
    mvc.perform(post(RENDER_URI).contentType(APPLICATION_JSON).content(BODY))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.tasks[0].taskId").value(42));
  }

  @Test
  @DisplayName("POST render does not return 401 for anonymous PUBLIC (filter allows ROLE_PUBLIC)")
  void renderDoesNotReturnUnauthorizedForAnonymous() throws Exception {
    MvcResult result =
        mvc.perform(post(RENDER_URI).contentType(APPLICATION_JSON).content(BODY)).andReturn();
    assertThat(result.getResponse().getStatus())
        .as("anonymous ROLE_PUBLIC must pass the HTTP matcher (not 401)")
        .isNotEqualTo(HttpStatus.UNAUTHORIZED.value());
  }
}
