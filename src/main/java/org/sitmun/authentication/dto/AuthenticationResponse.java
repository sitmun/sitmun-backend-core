package org.sitmun.authentication.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AuthenticationResponse {

  private String idToken;

  public AuthenticationResponse() {
  }

  public AuthenticationResponse(String idToken) {
    this.idToken = idToken;
  }

  @JsonProperty("id_token")
  public String getIdToken() {
    return idToken;
  }

  void setIdToken(String idToken) {
    this.idToken = idToken;
  }
}