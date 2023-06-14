package org.sitmun.authorization.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@Builder
public class ServiceDto {
  private String id;
  private String url;
  private String type;
  private Map<String, String> parameters;
}
