package org.sitmun.administration.controller.dto;

import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class MoreInfoAdvancedRenderRequestDto {
  private List<Integer> miaTaskIds;
  private Map<String, Object> parameters;
  private List<Double> bbox;
  private String queriedLayer;
  private String queriedService;
}
