package org.sitmun.administration.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_PDF;
import static org.springframework.http.MediaType.APPLICATION_XML;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.administration.service.template.TemplateExecutionService;
import org.sitmun.administration.service.template.TemplateRenderService;
import org.sitmun.administration.service.template.export.TemplateExportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Template export XML endpoint")
class TemplatePreviewControllerXmlExportIntegrationTest {

  @Autowired private MockMvc mvc;

  @MockBean private TemplateExecutionService templateExecutionService;
  @MockBean private TemplateRenderService templateRenderService;
  @MockBean private TemplateExportService templateExportService;

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

    when(templateExportService.exportHtml(eq("\n    <html>\n      <body>\n        <h1>Informe</h1>\n        <p>Contingut renderitzat</p>\n      </body>\n    </html>\n  "), eq("pdf"), eq(null)))
        .thenReturn("ok".getBytes());
    when(templateExportService.resolveExportFilename(eq(null), eq("pdf")))
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
  @DisplayName("POST /api/tasks/template/export uses task name for download filename")
  @WithMockUser(roles = "ADMIN")
  void exportUsesTaskNameForFilename() throws Exception {
    String xml =
        """
        <templateExportRequest>
          <output>pdf</output>
          <taskId>201</taskId>
          <template><![CDATA[
            <html><body><h1>Informe</h1></body></html>
          ]]></template>
        </templateExportRequest>
        """;

    when(templateExportService.exportHtml(eq("\n    <html><body><h1>Informe</h1></body></html>\n  "), eq("pdf"), eq(201L)))
        .thenReturn("ok".getBytes());
    when(templateExportService.resolveExportFilename(eq(201L), eq("pdf")))
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
  @DisplayName("POST /api/tasks/template/export returns xml media type for xml output")
  @WithMockUser(roles = "ADMIN")
  void exportReturnsXmlMediaTypeForXmlOutput() throws Exception {
    String xml = "<templateExportRequest><output>xml</output><template><![CDATA[<report/>]]></template></templateExportRequest>";

    when(templateExportService.exportHtml(eq("<report/>"), eq("xml"), eq(null)))
        .thenReturn("<report/>".getBytes());
    when(templateExportService.resolveExportFilename(eq(null), eq("xml")))
        .thenReturn("report.xml");

    mvc.perform(
            post("/api/tasks/template/export")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(APPLICATION_XML)
                .content(xml))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(APPLICATION_XML))
        .andExpect(header().string("Content-Disposition", "attachment; filename=\"report.xml\""));
  }

  @Test
  @DisplayName("POST /api/tasks/template/export returns pdf media type for pdf output")
  @WithMockUser(roles = "ADMIN")
  void exportReturnsPdfMediaTypeForPdfOutput() throws Exception {
    String xml = "<templateExportRequest><output>pdf</output><template><![CDATA[<html><body>ok</body></html>]]></template></templateExportRequest>";

    when(templateExportService.exportHtml(eq("<html><body>ok</body></html>"), eq("pdf"), eq(null)))
        .thenReturn("ok".getBytes());
    when(templateExportService.resolveExportFilename(eq(null), eq("pdf")))
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
  @DisplayName("POST /api/tasks/template/export rejects missing template and taskId")
  @WithMockUser(roles = "ADMIN")
  void exportRejectsMissingTemplateAndTaskId() throws Exception {
    String xml = "<templateExportRequest><output>pdf</output></templateExportRequest>";

    mvc.perform(
            post("/api/tasks/template/export")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(APPLICATION_XML)
                .content(xml))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("POST /api/tasks/template/export requires authentication")
  void exportRequiresAuthentication() throws Exception {
    String xml = "<templateExportRequest><output>pdf</output><template><![CDATA[<html/>]]></template></templateExportRequest>";

    mvc.perform(
            post("/api/tasks/template/export")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(APPLICATION_XML)
                .content(xml))
        .andExpect(status().isUnauthorized());
  }
}
