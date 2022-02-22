package org.sitmun.web.exceptions;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Builder
@Getter
@Setter
public class ApiErrorResponse {

  /**
   * http status code.
   */
  private Integer status;

  /**
   * In case we want to provide API based custom error code.
   */
  private String error;

  /**
   * customer error message to the client API
   */
  private String message;

  /**
   * Any further details which can help client API.
   */
  private String path;

  /**
   * Time of the error.make sure to define a standard time zone to avoid any confusion.
   */
  private LocalDateTime timestamp;
}
