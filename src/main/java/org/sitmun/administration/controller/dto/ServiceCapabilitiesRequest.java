package org.sitmun.administration.controller.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ServiceCapabilitiesRequest {

  private Integer id;
  private String url;
  private String type;
  private String authenticationMode;
  private String user;
  private String password;

  @JsonIgnore private boolean passwordPresent;

  public Integer getId() {
    if (id == null || id < 1) {
      return null;
    }
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getAuthenticationMode() {
    return authenticationMode;
  }

  public void setAuthenticationMode(String authenticationMode) {
    this.authenticationMode = authenticationMode;
  }

  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  public String getPassword() {
    return password;
  }

  @JsonProperty("password")
  public void setPassword(String password) {
    this.passwordPresent = true;
    this.password = password;
  }

  @JsonIgnore
  public boolean isPasswordPresent() {
    return passwordPresent;
  }
}
