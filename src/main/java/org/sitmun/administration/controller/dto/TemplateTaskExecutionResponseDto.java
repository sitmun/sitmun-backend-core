package org.sitmun.administration.controller.dto;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TemplateTaskExecutionResponseDto {
  Integer taskId;
  String status;
  String resultType;
  Map<String, Object> context;
  List<Map<String, Object>> rows;
  String resourceUrl;
}
