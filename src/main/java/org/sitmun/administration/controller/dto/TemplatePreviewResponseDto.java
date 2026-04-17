package org.sitmun.administration.controller.dto;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TemplatePreviewResponseDto {
  String html;
  List<String> placeholders;
}
