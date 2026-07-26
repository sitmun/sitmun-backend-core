package org.sitmun.authentication.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MobileAuthenticationResponse(
    @JsonProperty("access_token") String accessToken,
    @JsonProperty("token_type") String tokenType,
    @JsonProperty("expires_in") long expiresIn) {

  public static MobileAuthenticationResponse bearer(String accessToken, long expiresInSeconds) {
    return new MobileAuthenticationResponse(accessToken, "Bearer", expiresInSeconds);
  }
}
