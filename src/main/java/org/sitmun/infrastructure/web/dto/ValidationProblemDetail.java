package org.sitmun.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * RFC 9457 Problem Detail for validation errors. Extends the standard Problem Detail format with a
 * list of field-specific validation errors.
 *
 * <p>This is used when a request fails validation, typically resulting in HTTP 400 Bad Request or
 * 422 Unprocessable Entity responses.
 */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ValidationProblemDetail extends ProblemDetail {

  /** List of field-specific validation errors. */
  private List<FieldError> errors;

  @Builder(builderMethodName = "validationBuilder")
  public ValidationProblemDetail(
      String type,
      Integer status,
      String title,
      String detail,
      String instance,
      List<FieldError> errors) {
    super(type, status, title, detail, instance, null);
    this.errors = errors != null ? errors : new ArrayList<>();
  }

  /**
   * Add a field error to the validation problem.
   *
   * @param fieldError the field error to add
   * @return this ValidationProblemDetail instance for method chaining
   */
  public ValidationProblemDetail addFieldError(FieldError fieldError) {
    if (this.errors == null) {
      this.errors = new ArrayList<>();
    }
    this.errors.add(fieldError);
    return this;
  }

  /** Represents a field-specific validation error. */
  @Builder
  @Getter
  @Setter
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class FieldError {

    /** The field name that has the validation error. */
    private String field;

    /** The value that was provided for the field and rejected. */
    private Object rejectedValue;

    /** The expected type or format for the field. */
    private String expectedType;

    /** The validation error message for this specific field. */
    private String message;
  }
}
