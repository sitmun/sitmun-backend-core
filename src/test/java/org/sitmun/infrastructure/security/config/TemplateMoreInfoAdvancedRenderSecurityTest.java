package org.sitmun.infrastructure.security.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.administration.controller.dto.MoreInfoAdvancedRenderResponseDto;
import org.sitmun.administration.controller.dto.TemplateTaskExecutionResponseDto;
import org.sitmun.administration.service.template.TemplateExecutionService;
import org.sitmun.authentication.controller.AuthenticationController;
import org.sitmun.infrastructure.security.service.JsonWebTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Real {@link org.springframework.security.web.SecurityFilterChain} (filters on). Semantics match
 * {@link DashboardInfoSecurityTest}: anonymous → 401 on ADMIN paths; USER → 403; ADMIN → 200.
 * Render remains USER|PUBLIC.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("MIA render SecurityFilterChain")
class TemplateMoreInfoAdvancedRenderSecurityTest {

  @Autowired private MockMvc mvc;

  @Autowired private JsonWebTokenService jsonWebTokenService;

  @MockitoBean private TemplateExecutionService templateExecutionService;

  @Test
  @DisplayName("POST render without Authorization is not rejected as unauthenticated")
  void renderWithoutBearerReachesServiceAsPublic() throws Exception {
    when(templateExecutionService.renderMoreInfoAdvanced(any()))
        .thenReturn(MoreInfoAdvancedRenderResponseDto.builder().tasks(List.of()).build());

    mvc.perform(
            post("/api/tasks/template/more-info-advanced/render")
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {"miaTaskIds":[42],"appId":1,"terId":10,"parameters":{"featureId":"123"}}
                    """))
        .andExpect(status().isOk());

    verify(templateExecutionService).renderMoreInfoAdvanced(any());
  }

  @Test
  @DisplayName("POST render with viewer_access_token reaches service as USER")
  void renderWithViewerAccessTokenReachesServiceAsUser() throws Exception {
    when(templateExecutionService.renderMoreInfoAdvanced(any()))
        .thenReturn(MoreInfoAdvancedRenderResponseDto.builder().tasks(List.of()).build());

    String token = jsonWebTokenService.generateToken("admin", new Date());

    mvc.perform(
            post("/api/tasks/template/more-info-advanced/render")
                .contentType(APPLICATION_JSON)
                .cookie(new Cookie(AuthenticationController.VIEWER_ACCESS_TOKEN_COOKIE_NAME, token))
                .content(
                    """
                    {"miaTaskIds":[42],"appId":1,"terId":10,"parameters":{"featureId":"123"}}
                    """))
        .andExpect(status().isOk());

    verify(templateExecutionService).renderMoreInfoAdvanced(any());
  }

  @Test
  @DisplayName("POST execute-child without Authorization is 401 (ADMIN filter chain)")
  void executeChildWithoutBearerIsUnauthorized() throws Exception {
    mvc.perform(
            post("/api/tasks/template/execute-child")
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {"templateTaskId":100,"linkedTaskId":200,"parameters":{"id":"123"}}
                    """))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("POST execute-child as ROLE_USER is 403")
  @WithMockUser(authorities = "ROLE_USER")
  void executeChildWithUserRoleIsForbidden() throws Exception {
    mvc.perform(
            post("/api/tasks/template/execute-child")
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {"templateTaskId":100,"linkedTaskId":200,"parameters":{"id":"123"}}
                    """))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("POST execute-child with ADMIN token reaches service")
  void executeChildWithAdminTokenReachesService() throws Exception {
    when(templateExecutionService.executeLinkedTask(any()))
        .thenReturn(
            TemplateTaskExecutionResponseDto.builder().taskId(200).status("COMPLETED").build());

    String token = jsonWebTokenService.generateToken("admin", new Date());

    mvc.perform(
            post("/api/tasks/template/execute-child")
                .contentType(APPLICATION_JSON)
                .cookie(new Cookie(AuthenticationController.VIEWER_ACCESS_TOKEN_COOKIE_NAME, token))
                .content(
                    """
                    {"templateTaskId":100,"linkedTaskId":200,"parameters":{"id":"123"}}
                    """))
        .andExpect(status().isOk());

    verify(templateExecutionService).executeLinkedTask(any());
  }
}
