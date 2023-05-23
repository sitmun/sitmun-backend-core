package org.sitmun.authorization.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class TerritoryDto {
  private Double[] initialExtent;
  private Double[] center;
  private Integer defaultZoomLevel;
}
