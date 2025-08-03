package org.sitmun.authentication.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** DTO object for storing a user's credentials. */
@Setter
@Getter
public class UserPasswordAuthenticationRequest {

  @Schema(description = "User identifier. It cannot be empty", example = "some_user")
  @NotNull
  @Size(min = 1, max = 50)
  private String username;

  @Schema(description = "Password", example = "some_password")
  @NotNull
  private String password;

  @Override
  public String toString() {
    return "LoginDTO{username='" + username + '}';
  }
}
