package org.sitmun.authorization.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.springframework.beans.factory.annotation.Value;

import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigProxyRequest {

  @JsonProperty("appId")
  private int appId;

  @JsonProperty("terId")
  private int terId;

  @JsonProperty("type")
  private String type;

  @JsonProperty("typeId")
  private int typeId;

  @JsonProperty("method")
  @Value("GET")
  private String method;

  @JsonProperty("parameters")
  private Map<String, String> parameters;

  @JsonProperty("requestBody")
  private Map<String, String> requestBody;

  @JsonProperty("id_token")
  private String token;

}