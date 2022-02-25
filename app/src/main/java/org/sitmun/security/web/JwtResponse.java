package org.sitmun.security.web;

import com.fasterxml.jackson.annotation.JsonProperty;

public class JwtResponse {

  private String idToken;

  public JwtResponse() {
  }

  public JwtResponse(String idToken) {
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