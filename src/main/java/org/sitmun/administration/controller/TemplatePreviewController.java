package org.sitmun.administration.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.sitmun.administration.controller.dto.MoreInfoAdvancedRenderRequestDto;
import org.sitmun.administration.controller.dto.MoreInfoAdvancedRenderResponseDto;
import org.sitmun.administration.controller.dto.TemplatePreviewRequestDto;
import org.sitmun.administration.controller.dto.TemplatePreviewResponseDto;
import org.sitmun.administration.controller.dto.TemplateTaskExecutionRequestDto;
import org.sitmun.administration.controller.dto.TemplateTaskExecutionResponseDto;
import org.sitmun.administration.service.template.TemplateExecutionService;
import org.sitmun.administration.service.template.TemplateRenderService;
import org.sitmun.infrastructure.web.config.RequestLocaleResolutionService;
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
}
