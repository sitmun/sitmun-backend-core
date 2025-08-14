package org.sitmun.resetPassword.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** DTO object for storing a user's credentials. */
@Setter
@Getter
public class ResetPasswordRequest {
  @Schema(description = "User password. It cannot be empty", example = "password")
  @NotNull
  @Size(min = 1, max = 50)
  private String newPassword;

  @NotNull private String email;

  @NotNull private String codeOTP;
}
