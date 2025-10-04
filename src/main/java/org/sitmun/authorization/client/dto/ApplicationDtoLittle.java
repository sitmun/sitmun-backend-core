package org.sitmun.authorization.client.dto;

import java.util.Date;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApplicationDtoLittle {
  private int id;
  private String title;
  private String type;
  private String logo;
  private String description;
  private String maintenanceInformation;
  private Boolean isUnavailable;
  private Boolean appPrivate;
  private Date lastUpdate;
  private String creator;
  private Map<String, Object> headerParams;
  private Map<String, String> config;
}
