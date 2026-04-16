package org.sitmun.authentication.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class AuthenticationResponse {

  @JsonProperty("proxy_token")
  private String proxyToken;
}
