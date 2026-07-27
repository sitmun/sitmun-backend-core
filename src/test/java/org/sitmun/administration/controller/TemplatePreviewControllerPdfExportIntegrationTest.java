package org.sitmun.administration.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_PDF;
import static org.springframework.http.MediaType.APPLICATION_XML;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sitmun.administration.controller.dto.MoreInfoAdvancedRenderResponseDto;
import org.sitmun.administration.service.template.TemplateExecutionService;
import org.sitmun.administration.service.template.TemplateRenderService;
import org.sitmun.administration.service.template.export.TemplateExportAuthorizationService;
import org.sitmun.administration.service.template.export.TemplateExportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Template PDF export endpoint with XML requests")
class TemplatePreviewControllerPdfExportIntegrationTest {

  @Autowired private MockMvc mvc;

  @MockBean private TemplateExecutionService templateExecutionService;
  @MockBean private TemplateRenderService templateRenderService;
  @MockBean private TemplateExportAuthorizationService templateExportAuthorizationService;
  @MockBean private TemplateExportService templateExportService;

  @BeforeEach
  void authorizeExport() {
    when(templateExportAuthorizationService.authorize(any(), any(), any(), any()))
        .thenReturn(new TemplateExportAuthorizationService.AuthorizedTasks(null, null));
  }

  @Test
  @DisplayName("POST /api/tasks/template/export accepts XML body with template")
  @WithMockUser(roles = "ADMIN")
  void exportAcceptsXmlTemplateBody() throws Exception {
    String xml =
        """
        <templateExportRequest>
          <output>pdf</output>
          <template><![CDATA[
            <html>
              <body>
                <h1>Informe</h1>
                <p>Contingut renderitzat</p>
              </body>
            </html>
          ]]></template>
        </templateExportRequest>
        """;

    when(templateExportService.exportHtml(eq("\n    <html>\n      <body>\n        <h1>Informe</h1>\n        <p>Contingut renderitzat</p>\n      </body>\n    </html>\n  "), eq("pdf"), any()))
        .thenReturn("ok".getBytes());
    when(templateExportService.resolveExportFilename(any(), any(), eq("pdf")))
        .thenReturn("report.pdf");

    mvc.perform(
        post("/api/tasks/template/export")
          .with(SecurityMockMvcRequestPostProcessors.csrf())
          .contentType(APPLICATION_XML)
          .content(xml))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Disposition", "attachment; filename=\"report.pdf\""))
        .andExpect(content().bytes("ok".getBytes()));
  }

  @Test
  @DisplayName("POST /api/tasks/template/export uses template task name for download filename")
  @WithMockUser(roles = "ADMIN")
  void exportUsesTemplateTaskNameForFilename() throws Exception {
    String xml =
        """
        <templateExportRequest>
          <output>pdf</output>
          <taskId>201</taskId>
          <templateTaskId>301</templateTaskId>
          <template><![CDATA[
            <html><body><h1>Informe</h1></body></html>
          ]]></template>
        </templateExportRequest>
        """;

    when(templateExportService.exportHtml(eq("\n    <html><body><h1>Informe</h1></body></html>\n  "), eq("pdf"), any()))
        .thenReturn("ok".getBytes());
    when(templateExportService.resolveExportFilename(any(), any(), eq("pdf")))
        .thenReturn("Plantilla territori.pdf");

    mvc.perform(
        post("/api/tasks/template/export")
          .with(SecurityMockMvcRequestPostProcessors.csrf())
          .contentType(APPLICATION_XML)
          .content(xml))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Disposition", "attachment; filename=\"Plantilla territori.pdf\""))
        .andExpect(content().bytes("ok".getBytes()));
  }

  @Test
  @DisplayName("POST /api/tasks/template/export rejects XML output")
  @WithMockUser(roles = "ADMIN")
  void exportRejectsXmlOutput() throws Exception {
    String xml =
        "<templateExportRequest><output>xml</output>"
            + "<template><![CDATA[<html/>]]></template></templateExportRequest>";

    mvc.perform(
            post("/api/tasks/template/export")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(APPLICATION_XML)
                .content(xml))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(templateExportService);
  }

  @Test
  @DisplayName("POST /api/tasks/template/export returns pdf media type for pdf output")
  @WithMockUser(roles = "ADMIN")
  void exportReturnsPdfMediaTypeForPdfOutput() throws Exception {
    String xml = "<templateExportRequest><output>pdf</output><template><![CDATA[<html><body>ok</body></html>]]></template></templateExportRequest>";

    when(templateExportService.exportHtml(eq("<html><body>ok</body></html>"), eq("pdf"), any()))
        .thenReturn("ok".getBytes());
    when(templateExportService.resolveExportFilename(any(), any(), eq("pdf")))
        .thenReturn("report.pdf");

    mvc.perform(
            post("/api/tasks/template/export")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(APPLICATION_XML)
                .content(xml))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(APPLICATION_PDF));
  }

  @Test
  @DisplayName("POST /api/tasks/template/export authorizes user profile context")
  @WithMockUser(roles = "USER")
  void exportAuthorizesUserProfileContext() throws Exception {
    String xml =
        "<templateExportRequest><output> PDF </output><taskId>201</taskId>"
            + "<templateTaskId>301</templateTaskId><applicationId>7</applicationId>"
            + "<territoryId>11</territoryId><template><![CDATA[<html/>]]></template>"
            + "</templateExportRequest>";
    when(templateExportService.exportHtml(eq("<html/>"), eq("pdf"), any()))
        .thenReturn("ok".getBytes());
    when(templateExportService.resolveExportFilename(any(), any(), eq("pdf")))
        .thenReturn("report.pdf");

    mvc.perform(
            post("/api/tasks/template/export")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(APPLICATION_XML)
                .content(xml))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(APPLICATION_PDF));

    verify(templateExportAuthorizationService).authorize(201, 301, 7, 11);
  }

  @Test
  @DisplayName("POST /api/tasks/template/export accepts authorized public profile context")
  @WithMockUser(username = "public", roles = "PUBLIC")
  void exportAcceptsPublicProfileContext() throws Exception {
    String xml =
        "<templateExportRequest><output>pdf</output><taskId>201</taskId>"
            + "<applicationId>7</applicationId><territoryId>11</territoryId>"
            + "<template><![CDATA[<html/>]]></template></templateExportRequest>";
    when(templateExportService.exportHtml(eq("<html/>"), eq("pdf"), any()))
        .thenReturn("ok".getBytes());
    when(templateExportService.resolveExportFilename(any(), any(), eq("pdf")))
        .thenReturn("report.pdf");

    mvc.perform(
            post("/api/tasks/template/export")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(APPLICATION_XML)
                .content(xml))
        .andExpect(status().isOk());

    verify(templateExportAuthorizationService).authorize(201, null, 7, 11);
  }

  @Test
  @DisplayName("POST MIA render accepts public profile role")
  @WithMockUser(username = "public", roles = "PUBLIC")
  void miaRenderAcceptsPublicProfileRole() throws Exception {
    when(templateExecutionService.renderMoreInfoAdvanced(any()))
        .thenReturn(MoreInfoAdvancedRenderResponseDto.builder().tasks(java.util.List.of()).build());

    mvc.perform(
            post("/api/tasks/template/more-info-advanced/render")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType("application/json")
                .content(
                    "{\"miaTaskIds\":[16],\"applicationId\":7,\"territoryId\":11,"
                        + "\"parameters\":{}}"))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("POST /api/tasks/template/export rejects missing output")
  @WithMockUser(roles = "ADMIN")
  void exportRejectsMissingOutput() throws Exception {
    String xml = "<templateExportRequest><template><![CDATA[<html/>]]></template></templateExportRequest>";

    mvc.perform(
            post("/api/tasks/template/export")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(APPLICATION_XML)
                .content(xml))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(templateExportService);
  }

  @Test
  @DisplayName("POST /api/tasks/template/export rejects missing template even with taskId")
  @WithMockUser(roles = "ADMIN")
  void exportRejectsMissingTemplateAndTaskId() throws Exception {
    String xml =
        "<templateExportRequest><output>pdf</output><taskId>201</taskId>"
            + "</templateExportRequest>";

    mvc.perform(
            post("/api/tasks/template/export")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(APPLICATION_XML)
                .content(xml))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("POST /api/tasks/template/export requires task and profile context for public")
  void exportRequiresTaskAndProfileContextForPublic() throws Exception {
    String xml = "<templateExportRequest><output>pdf</output><template><![CDATA[<html/>]]></template></templateExportRequest>";
    when(templateExportAuthorizationService.authorize(null, null, null, null))
        .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST));

    mvc.perform(
            post("/api/tasks/template/export")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(APPLICATION_XML)
                .content(xml))
        .andExpect(status().isBadRequest());
  }
}
