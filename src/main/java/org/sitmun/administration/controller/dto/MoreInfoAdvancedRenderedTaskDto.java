package org.sitmun.administration.controller.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MoreInfoAdvancedRenderedTaskDto {
  Integer taskId;
  String title;
  String html;
}
