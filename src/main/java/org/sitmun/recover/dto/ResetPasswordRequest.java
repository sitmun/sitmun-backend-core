package org.sitmun.recover.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
/**
 * DTO object for storing a user's credentials.
 */
@Setter
@Getter
public class ResetPasswordRequest {
  @Schema(description = "User password. It cannot be empty", example = "password")
  @NotNull
  @Size(min = 1, max = 50)
  private String password;

  @NotNull
  private String token;
}