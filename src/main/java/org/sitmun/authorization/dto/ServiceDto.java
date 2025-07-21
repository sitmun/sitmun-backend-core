package org.sitmun.authorization.dto;

import java.util.Map;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ServiceDto {
  private String id;
  private String url;
  private String type;
  private Boolean isProxied;
  private Map<String, String> parameters;
}
