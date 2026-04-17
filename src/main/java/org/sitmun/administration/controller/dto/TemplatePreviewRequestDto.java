package org.sitmun.administration.controller.dto;

import java.util.Map;
import lombok.Data;

@Data
public class TemplatePreviewRequestDto {
  private Integer templateTaskId;
  private String templateHtml;
  private Map<String, Object> context;
}
