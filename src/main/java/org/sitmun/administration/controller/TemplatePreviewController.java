package org.sitmun.administration.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.sitmun.administration.controller.dto.MoreInfoAdvancedRenderRequestDto;
import org.sitmun.administration.controller.dto.MoreInfoAdvancedRenderResponseDto;
import org.sitmun.administration.controller.dto.TemplatePreviewRequestDto;
import org.sitmun.administration.controller.dto.TemplatePreviewResponseDto;
import org.sitmun.administration.controller.dto.TemplateExportRequestDto;
import org.sitmun.administration.controller.dto.TemplateTaskExecutionRequestDto;
import org.sitmun.administration.controller.dto.TemplateTaskExecutionResponseDto;
import org.sitmun.administration.service.template.TemplateExecutionService;
import org.sitmun.administration.service.template.TemplateRenderService;
import org.sitmun.administration.service.template.export.TemplateExportAuthorizationService;
import org.sitmun.administration.service.template.export.TemplateExportService;
import org.sitmun.infrastructure.web.config.RequestLocaleResolutionService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks/template")
@RequiredArgsConstructor
public class TemplatePreviewController {

  private final TemplateExecutionService templateExecutionService;
  private final TemplateRenderService templateRenderService;
  private final TemplateExportAuthorizationService templateExportAuthorizationService;
  private final TemplateExportService templateExportService;
  private final RequestLocaleResolutionService requestLocaleResolutionService;

  @PostMapping("/execute-child")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<TemplateTaskExecutionResponseDto> executeChild(
      @RequestBody TemplateTaskExecutionRequestDto requestDto) {
    return ResponseEntity.ok(templateExecutionService.executeLinkedTask(requestDto));
  }

  @PostMapping("/more-info-advanced/render")
  @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'PUBLIC')")
  public ResponseEntity<MoreInfoAdvancedRenderResponseDto> renderMoreInfoAdvanced(
      @RequestBody @Valid MoreInfoAdvancedRenderRequestDto requestDto,
      HttpServletRequest request,
      HttpServletResponse response) {
    String language = requestLocaleResolutionService.resolveLanguage(request, response, this, null);
    return ResponseEntity.ok(templateExecutionService.renderMoreInfoAdvanced(requestDto, language));
  }

  @PostMapping(
      value = "/export",
      consumes = MediaType.APPLICATION_XML_VALUE,
      produces = MediaType.APPLICATION_PDF_VALUE)
  @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'PUBLIC')")
  public ResponseEntity<byte[]> export(@RequestBody @Valid TemplateExportRequestDto request) {
    String output = TemplateExportService.normalizeOutput(request.output());
    TemplateExportAuthorizationService.AuthorizedTasks authorizedTasks =
        templateExportAuthorizationService.authorize(
            request.taskId(),
            request.templateTaskId(),
            request.applicationId(),
            request.territoryId());
    byte[] content =
        templateExportService.exportHtml(request.template(), output, authorizedTasks.exportTask());
    String filename =
        templateExportService.resolveExportFilename(
            authorizedTasks.templateTask(), authorizedTasks.exportTask(), output);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_PDF);
    headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
    headers.setContentLength(content.length);
    return ResponseEntity.ok().headers(headers).body(content);
  }

  @PostMapping("/preview")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<TemplatePreviewResponseDto> preview(
      @RequestBody TemplatePreviewRequestDto requestDto,
      HttpServletRequest request,
      HttpServletResponse response) {
    String language = requestLocaleResolutionService.resolveLanguage(request, response, this, null);
    return ResponseEntity.ok(
        templateRenderService.renderPreview(
            requestDto.getTemplateHtml(),
            requestDto.getContext(),
            requestDto.getKnownTaskReferences(),
            language));
  }
}
