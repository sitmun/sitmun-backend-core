package org.sitmun.plugin.core.web.rest.dto;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * DTO object for storing a user's credentials.
 */
public class LoginDTO {

  @NotNull
  @Size(min = 1, max = 50)
  private String username;

  @NotNull
  private String password;


  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  @Override
  public String toString() {
    return "LoginDTO{" +
               "username='" + username + '\'' +
               '}';
  }
}
