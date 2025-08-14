package org.sitmun.resetPassword.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** DTO object for storing a user's credentials. */
@Setter
@Getter
public class RequestNewPassword {
  @Schema(
      description = "User identifier. It cannot be empty",
      example = "some_user or some_user@mail.com")
  @NotNull
  @Size(min = 1, max = 50)
  private String email;

  @Override
  public String toString() {
    return "LoginDTO{login='" + email + '}';
  }
}
