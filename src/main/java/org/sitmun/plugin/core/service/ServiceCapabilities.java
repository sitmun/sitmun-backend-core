package org.sitmun.plugin.core.service;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Builder
@Getter
@Setter
public class ServiceCapabilities {
  private Boolean success;
  private String reason;
  private String type;
  private String asText;
  private Map<String, Object> asJson;
}
