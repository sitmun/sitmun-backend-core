package org.sitmun.authorization.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class TaskDto {
  private String id;

  @JsonProperty("ui-control")
  private String uiControl;

  @JsonProperty("url")
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String url;

  @JsonProperty("type")
  private String type;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Map<String, Object> parameters;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Map<String, Object> fields;
}
