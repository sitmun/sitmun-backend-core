package org.sitmun.administration.controller.dto;

import java.util.Map;
import lombok.Data;

@Data
public class TemplateTaskExecutionRequestDto {
  private Integer templateTaskId;
  private Integer linkedTaskId;
  private Map<String, Object> parameters;
}
