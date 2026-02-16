package org.sitmun.infrastructure.web.controller;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import java.util.ArrayList;
import java.util.List;
import org.sitmun.infrastructure.web.dto.ProblemTypes;
import org.sitmun.infrastructure.web.dto.ValidationProblemDetail;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

/**
 * Exception handler for JSON deserialization errors.
 *
 * <p>Handles cases where the request body cannot be parsed or contains invalid data formats,
 * returning RFC 9457 Problem Detail responses with field-level error details.
 */
@RestControllerAdvice
public class DeserializationExceptionHandler {

  /**
   * Handles {@link HttpMessageNotReadableException} - request body deserialization errors.
   *
   * <p>This includes invalid JSON format, type mismatches, and other parsing errors.
   */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ValidationProblemDetail> handleHttpMessageNotReadable(
      HttpMessageNotReadableException ex, WebRequest request) {
    List<ValidationProblemDetail.FieldError> errors = new ArrayList<>();
    Throwable cause = ex.getCause();

    if (cause instanceof InvalidFormatException ife) {
      for (JsonMappingException.Reference ref : ife.getPath()) {
        errors.add(
            ValidationProblemDetail.FieldError.builder()
                .field(ref.getFieldName())
                .rejectedValue(String.valueOf(ife.getValue()))
                .expectedType(
                    ife.getTargetType() != null ? ife.getTargetType().getSimpleName() : null)
                .message(
                    String.format(
                        "Invalid value '%s' for field '%s'", ife.getValue(), ref.getFieldName()))
                .build());
      }
    } else if (cause instanceof MismatchedInputException mie) {
      for (JsonMappingException.Reference ref : mie.getPath()) {
        errors.add(
            ValidationProblemDetail.FieldError.builder()
                .field(ref.getFieldName())
                .rejectedValue(null)
                .expectedType(
                    mie.getTargetType() != null ? mie.getTargetType().getSimpleName() : null)
                .message(String.format("Mismatched input for field '%s'", ref.getFieldName()))
                .build());
      }
    }

    String requestPath = null;
    if (request instanceof ServletWebRequest servletRequest) {
      requestPath = servletRequest.getRequest().getRequestURI();
    }

    ValidationProblemDetail problem =
        ValidationProblemDetail.validationBuilder()
            .type(ProblemTypes.DESERIALIZATION_ERROR)
            .status(HttpStatus.UNPROCESSABLE_ENTITY.value())
            .title("Deserialization Error")
            .detail("Request body could not be parsed. Please check the format and field types.")
            .instance(requestPath)
            .errors(errors.isEmpty() ? null : errors)
            .build();

    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problem);
  }
}
