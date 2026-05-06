package org.sitmun.administration.controller;

import lombok.RequiredArgsConstructor;
import org.sitmun.administration.controller.dto.MoreInfoAdvancedRenderRequestDto;
import org.sitmun.administration.controller.dto.MoreInfoAdvancedRenderResponseDto;
import org.sitmun.administration.controller.dto.TemplatePreviewRequestDto;
import org.sitmun.administration.controller.dto.TemplatePreviewResponseDto;
import org.sitmun.administration.controller.dto.TemplateTaskExecutionRequestDto;
import org.sitmun.administration.controller.dto.TemplateTaskExecutionResponseDto;
import org.sitmun.administration.service.template.AdminTaskExecutionService;
import org.sitmun.administration.service.template.TemplateRenderService;
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

  private final AdminTaskExecutionService adminTaskExecutionService;
  private final TemplateRenderService templateRenderService;

  @PostMapping("/execute-child")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<TemplateTaskExecutionResponseDto> executeChild(
      @RequestBody TemplateTaskExecutionRequestDto requestDto) {
    return ResponseEntity.ok(adminTaskExecutionService.executeLinkedTask(requestDto));
  }

  @PostMapping("/more-info-advanced/render")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<MoreInfoAdvancedRenderResponseDto> renderMoreInfoAdvanced(
      @RequestBody MoreInfoAdvancedRenderRequestDto requestDto) {
    return ResponseEntity.ok(adminTaskExecutionService.renderMoreInfoAdvanced(requestDto));
  }

  @PostMapping("/preview")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<TemplatePreviewResponseDto> preview(
      @RequestBody TemplatePreviewRequestDto requestDto) {
    return ResponseEntity.ok(
        templateRenderService.renderPreview(
            requestDto.getTemplateHtml(),
            requestDto.getContext(),
            requestDto.getTemplateTaskId(),
            requestDto.getKnownTaskReferences()));
  }
}
