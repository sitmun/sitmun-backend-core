package org.sitmun.authorization.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.sitmun.infrastructure.persistence.type.envelope.Envelope;

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
  private String logo;
  private String description;

  // The following fields are related to the Territory

  private Integer defaultZoomLevel;
  private PointOfInterestDto pointOfInterest;
  private Double[] initialExtent;

  public void setInitialExtentFromEnvelope(Envelope initialExtent) {
    setInitialExtent(new Double[]{
      initialExtent.getMinX(),
      initialExtent.getMinY(),
      initialExtent.getMaxX(),
      initialExtent.getMaxY()
    });
  }
}
