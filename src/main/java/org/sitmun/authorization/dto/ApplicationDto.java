package org.sitmun.authorization.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Date;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.util.Map;

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
  private Map<String, Object> headerParams;
  private String maintenanceInformation;
  private Boolean isUnavailable;
  private Boolean appPrivate;
  private Date lastUpdate;
  private String creator;

  private Integer defaultZoomLevel;
  private PointOfInterestDto pointOfInterest;
  private Double[] initialExtent;

  public void setInitialExtentFromEnvelope(Envelope initialExtent) {
    setInitialExtent(
        new Double[] {
          initialExtent.getMinX(),
          initialExtent.getMinY(),
          initialExtent.getMaxX(),
          initialExtent.getMaxY()
        });
  }
}
