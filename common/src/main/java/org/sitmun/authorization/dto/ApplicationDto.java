package org.sitmun.authorization.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ApplicationDto {
  private int id;
  private String title;
  private String type;
  private String theme;
  private String srs;
  @JsonProperty("situation-map")
  private String situationMap;
  private Double[] initialExtent;
}
