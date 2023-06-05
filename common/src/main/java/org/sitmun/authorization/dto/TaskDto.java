package org.sitmun.authorization.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@Builder
public class TaskDto {
  private String id;
  @JsonProperty("ui-control")
  private String uiControl;

  private Map<String, Object> parameters;
}
