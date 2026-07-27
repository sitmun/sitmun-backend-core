package org.sitmun.administration.controller.dto;

import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class MoreInfoAdvancedRenderRequestDto {
  private List<Integer> miaTaskIds;
  @Positive private Integer applicationId;
  @Positive private Integer territoryId;
  private Map<String, Object> parameters;
  private List<Double> featureBbox;
}
