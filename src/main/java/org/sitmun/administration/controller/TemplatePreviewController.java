package org.sitmun.administration.controller;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.sitmun.administration.controller.dto.MoreInfoAdvancedRenderRequestDto;
import org.sitmun.administration.controller.dto.MoreInfoAdvancedRenderResponseDto;
import org.sitmun.administration.controller.dto.TemplateExportRequestDto;
import org.sitmun.administration.controller.dto.TemplatePreviewRequestDto;
import org.sitmun.administration.controller.dto.TemplatePreviewResponseDto;
import org.sitmun.administration.controller.dto.TemplateTaskExecutionRequestDto;
import org.sitmun.administration.controller.dto.TemplateTaskExecutionResponseDto;
import org.sitmun.administration.service.template.TemplateExecutionService;
import org.sitmun.administration.service.template.TemplateRenderService;
import org.sitmun.administration.service.template.export.TemplateExportService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.sitmun.infrastructure.web.config.RequestLocaleResolutionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/tasks/template")
@RequiredArgsConstructor
public class TemplatePreviewController {

  private final TemplateExecutionService templateExecutionService;
  private final TemplateRenderService templateRenderService;
  private final TemplateExportService templateExportService;
  private final RequestLocaleResolutionService requestLocaleResolutionService;

  @PostMapping("/execute-child")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<TemplateTaskExecutionResponseDto> executeChild(
      @RequestBody TemplateTaskExecutionRequestDto requestDto) {
    return ResponseEntity.ok(templateExecutionService.executeLinkedTask(requestDto));
  }

  @PostMapping("/more-info-advanced/render")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<MoreInfoAdvancedRenderResponseDto> renderMoreInfoAdvanced(
      @RequestBody MoreInfoAdvancedRenderRequestDto requestDto,
      HttpServletRequest request,
      HttpServletResponse response) {
    requestLocaleResolutionService.resolveLanguage(request, response, this, null);
    return ResponseEntity.ok(templateExecutionService.renderMoreInfoAdvanced(requestDto));
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
            requestDto.getTemplateTaskId(),
            requestDto.getKnownTaskReferences(),
            language));
  }

  /**
   * Exports a rendered MIA template to a downloadable file.
   *
   * <p>Source resolution priority:
   * <ul>
   *   <li>If {@code template} contains runtime HTML, it is used as the export source.
   *   <li>Otherwise, when {@code taskId} is present, the server reads the configured
   *       {@code downloadSource} property from the task and uses that server-side content.
   * </ul>
   */
  @PostMapping("/export")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<byte[]> export(@RequestBody @Valid TemplateExportRequestDto request) {
    String output = request.output().toLowerCase();
    if (!StringUtils.hasText(request.template()) && request.taskId() == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Either runtime HTML content or a taskId with configured source must be provided");
    }

    byte[] content = templateExportService.exportHtml(request.template(), output, request.taskId());

    String filename =
        templateExportService.resolveExportFilename(request.templateTaskId(), request.taskId(), output);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(resolveMediaType(output));
    headers.setContentDisposition(
        ContentDisposition.attachment().filename(filename).build());
    headers.setContentLength(content.length);

    return ResponseEntity.ok().headers(headers).body(content);
  }

  private MediaType resolveMediaType(String output) {
    return switch (output) {
      case "pdf" -> MediaType.APPLICATION_PDF;
      case "xml" -> MediaType.APPLICATION_XML;
      default -> MediaType.APPLICATION_OCTET_STREAM;
    };
  }
}

