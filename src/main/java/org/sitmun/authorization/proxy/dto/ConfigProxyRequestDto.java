package org.sitmun.authorization.proxy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigProxyRequestDto {

  @JsonProperty("appId")
  private int appId;

  @JsonProperty("terId")
  private int terId;

  @JsonProperty("type")
  private String type;

  @JsonProperty("typeId")
  private int typeId;

  @JsonProperty("method")
  private String method;

  @JsonProperty("parameters")
  private Map<String, String> parameters;

  @JsonProperty("requestBody")
  private String requestBody;

  @JsonProperty("id_token")
  private String token;
}
