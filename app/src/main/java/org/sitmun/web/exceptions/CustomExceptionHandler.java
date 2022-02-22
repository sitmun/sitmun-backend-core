package org.sitmun.web.exceptions;

import org.sitmun.domain.DatabaseConnection;
import org.sitmun.repository.handlers.RequirementException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Controller advice.
 */
@ControllerAdvice
public class CustomExceptionHandler extends ResponseEntityExceptionHandler {

  /**
   * Handles a {@link DatabaseConnection} driver missing exception.
   *
   * @param exception the exception.
   * @return a 500 error
   */
  @ExceptionHandler(DatabaseConnectionDriverNotFoundException.class)
  public ResponseEntity<ApiErrorResponse> databaseConnectionDriverNotFound(DatabaseConnectionDriverNotFoundException exception, @NonNull WebRequest request) {
    ApiErrorResponse response = ApiErrorResponse.builder()
      .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
      .message(exception.getLocalizedMessage())
      .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
      .timestamp(LocalDateTime.now(ZoneOffset.UTC))
      .path(((ServletWebRequest) request).getRequest().getRequestURI())
      .build();
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
  }

  /**
   * Handles a {@link DatabaseConnection} SQL exception.
   *
   * @param exception the exception.
   * @return a 500 error
   */
  @ExceptionHandler(DatabaseSQLException.class)
  public ResponseEntity<ApiErrorResponse> databaseSQLException(DatabaseSQLException exception, @NonNull WebRequest request) {
    ApiErrorResponse response = ApiErrorResponse.builder()
      .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
      .message(exception.getLocalizedMessage())
      .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
      .timestamp(LocalDateTime.now(ZoneOffset.UTC))
      .path(((ServletWebRequest) request).getRequest().getRequestURI())
      .build();
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
  }

  /**
   * Handles a {@link RequirementException} SQL exception.
   *
   * @param exception the exception.
   * @return a 400 error
   */
  @ExceptionHandler(RequirementException.class)
  public ResponseEntity<ApiErrorResponse> databaseSQLException(RequirementException exception, @NonNull WebRequest request) {
    ApiErrorResponse response = ApiErrorResponse.builder()
      .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
      .message(exception.getLocalizedMessage())
      .status(HttpStatus.BAD_REQUEST.value())
      .timestamp(LocalDateTime.now(ZoneOffset.UTC))
      .path(((ServletWebRequest) request).getRequest().getRequestURI())
      .build();
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }

  @Override
  @NonNull
  protected ResponseEntity<Object> handleHttpMessageNotReadable(
    HttpMessageNotReadableException exception, @NonNull HttpHeaders headers, @NonNull HttpStatus status, @NonNull WebRequest request) {
    ApiErrorResponse response = ApiErrorResponse.builder()
      .error(HttpStatus.UNPROCESSABLE_ENTITY.getReasonPhrase())
      .message(exception.getLocalizedMessage())
      .status(HttpStatus.UNPROCESSABLE_ENTITY.value())
      .timestamp(LocalDateTime.now(ZoneOffset.UTC))
      .path(((ServletWebRequest) request).getRequest().getRequestURI())
      .build();
    return handleExceptionInternal(exception, response, headers, HttpStatus.UNPROCESSABLE_ENTITY, request);
  }
}
