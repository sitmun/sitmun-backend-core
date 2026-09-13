package org.sitmun.administration.controller.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

@Data
public class MapImageRenderRequestDto {
  @NotNull
  private Integer taskId;

  @NotNull
  @Size(min = 4, max = 4)
  private List<Double> bbox;

  @Min(1)
  private Integer width;

  @Min(1)
  private Integer height;

  private String format;

  private String srs;
}
