package org.sitmun.administration.controller.dto;

import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class MoreInfoAdvancedRenderRequestDto {
  private List<Integer> miaTaskIds;
  private Integer appId;
  private Integer terId;
  private Map<String, Object> parameters;
}
