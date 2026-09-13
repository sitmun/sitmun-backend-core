package org.sitmun.administration.controller.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class MoreInfoAdvancedRenderRequestDto {
  private List<Integer> miaTaskIds;
  @Positive private Integer appId;
  @Positive private Integer terId;
  private Map<String, Object> parameters;
  @Size(min = 4, max = 4)
  private List<Double> featureBbox;
}
