package org.sitmun.infrastructure.web.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Builder
@Getter
@Setter
public class DomainExceptionResponse {

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

  /**
   * Detailed validation errors for specific fields.
   */
  private List<FieldError> errors;

  /**
   * Additional error details or context.
   */
  private Map<String, Object> details;

  /**
   * Represents a field-specific validation error.
   */
  @Builder
  @Getter
  @Setter
  public static class FieldError {
    /**
     * The field name that has the error.
     */
    private String field;

    /**
     * The value that was provided for the field.
     */
    private Object rejectedValue;

    /**
     * The expected type or format for the field.
     */
    private String expectedType;

    /**
     * The error message for this specific field.
     */
    private String message;
  }
}
