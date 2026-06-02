package org.sitmun.authorization.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Date;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.sitmun.infrastructure.persistence.type.envelope.Envelope;

@Getter
@Setter
@Builder
public class ApplicationDto {
  private int id;
  private String name;
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
  private String territoryCode;
  private String territoryName;
  private String territoryDescription;
  private String territorialAuthorityName;
  private String territorialAuthorityAddress;
  private String territoryTypeName;

  /**
   * Sets {@link #initialExtent} from an envelope, or clears it when {@code initialExtent} is null.
   */
  public void setInitialExtentFromEnvelope(Envelope initialExtent) {
    if (initialExtent == null) {
      setInitialExtent(null);
      return;
    }
    setInitialExtent(
        new Double[] {
          initialExtent.getMinX(),
          initialExtent.getMinY(),
          initialExtent.getMaxX(),
          initialExtent.getMaxY()
        });
  }
}
