package org.sitmun.infrastructure.security.password.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** DTO for password verification - only contains the password to verify. */
@Setter
@Getter
public class PasswordVerificationRequest {

  @Schema(description = "Password to verify for the currently authenticated user", example = "myPassword123")
  @NotNull
  private String password;
}
