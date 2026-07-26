package org.sitmun.administration.controller.dto;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MoreInfoAdvancedRenderResponseDto {
  List<MoreInfoAdvancedRenderedTaskDto> tasks;
}
