package org.sitmun.administration.controller.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class DatabaseConnectionDto {
  private boolean isValid;
  private String message;
}
