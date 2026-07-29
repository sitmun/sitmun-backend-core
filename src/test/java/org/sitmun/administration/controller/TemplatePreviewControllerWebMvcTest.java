package org.sitmun.administration.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.administration.controller.dto.MoreInfoAdvancedRenderResponseDto;
import org.sitmun.administration.controller.dto.TemplatePreviewResponseDto;
import org.sitmun.administration.controller.dto.TemplateTaskExecutionResponseDto;
import org.sitmun.administration.service.template.TemplateExecutionService;
import org.sitmun.administration.service.template.TemplateRenderService;
import org.sitmun.authentication.service.CookieService;
import org.sitmun.authorization.access.UserApplicationAccessPolicy;
import org.sitmun.infrastructure.persistence.type.i18n.TranslationRepository;
import org.sitmun.infrastructure.web.config.RequestLocaleResolutionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@WebMvcTest(TemplatePreviewController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("TemplatePreviewController WebMvc")
class TemplatePreviewControllerWebMvcTest {

  @Autowired private MockMvc mvc;

  @MockitoBean private TemplateExecutionService templateExecutionService;

  @MockitoBean private TemplateRenderService templateRenderService;

  @MockitoBean private RequestLocaleResolutionService requestLocaleResolutionService;

  @MockitoBean private TranslationRepository translationRepository;

  @MockitoBean private CookieService cookieService;

  @MockitoBean private UserApplicationAccessPolicy userApplicationAccessPolicy;

  @Test
  @WithMockUser(roles = "USER")
  @DisplayName("render without appId/terId returns 400")
  void renderRequiresAppIdAndTerId() throws Exception {
    when(templateExecutionService.renderMoreInfoAdvanced(any(), any()))
        .thenThrow(
            new ResponseStatusException(HttpStatus.BAD_REQUEST, "appId and terId are required"));

    mvc.perform(
            post("/api/tasks/template/more-info-advanced/render")
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {"miaTaskIds":[42],"parameters":{"featureId":"123"}}
                    """))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("execute-child without appId/terId is not 400 (ADMIN god mode)")
  void executeChildWithoutCoordinatesIsNotBadRequest() throws Exception {
    when(templateExecutionService.executeLinkedTask(any()))
        .thenReturn(
            TemplateTaskExecutionResponseDto.builder()
                .taskId(200)
                .status("COMPLETED")
                .resultType("table")
                .build());

    mvc.perform(
            post("/api/tasks/template/execute-child")
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {"templateTaskId":100,"linkedTaskId":200,"parameters":{"id":"123"}}
                    """))
        .andExpect(status().isOk());

    verify(templateExecutionService).executeLinkedTask(any());
  }

  @Test
  @WithMockUser(username = "public", roles = "PUBLIC")
  @DisplayName("PUBLIC principal can reach more-info-advanced/render")
  void publicPrincipalCanReachRender() throws Exception {
    when(requestLocaleResolutionService.resolveLanguage(any(), any(), any(), any()))
        .thenReturn("ca");
    when(templateExecutionService.renderMoreInfoAdvanced(any(), any()))
        .thenReturn(MoreInfoAdvancedRenderResponseDto.builder().tasks(List.of()).build());

    mvc.perform(
            post("/api/tasks/template/more-info-advanced/render")
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {"miaTaskIds":[42],"appId":1,"terId":10,"parameters":{"featureId":"123"}}
                    """))
        .andExpect(status().isOk());

    verify(templateExecutionService).renderMoreInfoAdvanced(any(), any());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("ADMIN principal can reach more-info-advanced/render")
  void adminPrincipalCanReachRender() throws Exception {
    when(requestLocaleResolutionService.resolveLanguage(any(), any(), any(), any()))
        .thenReturn("ca");
    when(templateExecutionService.renderMoreInfoAdvanced(any(), any()))
        .thenReturn(MoreInfoAdvancedRenderResponseDto.builder().tasks(List.of()).build());

    mvc.perform(
            post("/api/tasks/template/more-info-advanced/render")
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {"miaTaskIds":[42],"appId":1,"terId":1,"parameters":{}}
                    """))
        .andExpect(status().isOk());

    verify(templateExecutionService).renderMoreInfoAdvanced(any(), any());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("preview does not require appId/terId")
  void previewDoesNotRequireCoordinates() throws Exception {
    when(requestLocaleResolutionService.resolveLanguage(any(), any(), any(), any()))
        .thenReturn("ca");
    when(templateRenderService.renderPreview(any(), any(), any(), any(), any(), any()))
        .thenReturn(TemplatePreviewResponseDto.builder().html("<p>ok</p>").build());

    mvc.perform(
            post("/api/tasks/template/preview")
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {"templateHtml":"<p>{{x}}</p>","context":{"x":"1"}}
                    """))
        .andExpect(status().isOk());

    verify(templateExecutionService, never()).executeLinkedTask(any());
    verify(templateExecutionService, never()).renderMoreInfoAdvanced(any(), any());
    verify(templateRenderService).renderPreview(any(), any(), any(), eq("ca"), isNull(), isNull());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("preview forwards optional appId/terId")
  void previewForwardsOptionalCoordinates() throws Exception {
    when(requestLocaleResolutionService.resolveLanguage(any(), any(), any(), any()))
        .thenReturn("ca");
    when(templateRenderService.renderPreview(any(), any(), any(), any(), any(), any()))
        .thenReturn(TemplatePreviewResponseDto.builder().html("<p>ok</p>").build());

    mvc.perform(
            post("/api/tasks/template/preview")
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {"templateHtml":"<p>{{#APP_NAME}}</p>","context":{},"appId":12,"terId":4}
                    """))
        .andExpect(status().isOk());

    verify(templateRenderService).renderPreview(any(), any(), any(), eq("ca"), eq(12), eq(4));
  }
}
