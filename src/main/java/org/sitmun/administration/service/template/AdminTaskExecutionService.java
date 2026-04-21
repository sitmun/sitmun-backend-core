package org.sitmun.administration.service.template;

import lombok.RequiredArgsConstructor;
import org.sitmun.administration.controller.dto.TemplateTaskExecutionRequestDto;
import org.sitmun.administration.controller.dto.TemplateTaskExecutionResponseDto;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminTaskExecutionService {

  private final TemplateExecutionService templateExecutionService;

  public TemplateTaskExecutionResponseDto executeLinkedTask(TemplateTaskExecutionRequestDto requestDto) {
    return templateExecutionService.executeLinkedTask(requestDto);
  }
}
