package org.sitmun.common.web;

import org.sitmun.common.domain.RequirementException;
import org.springframework.context.MessageSource;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.data.rest.core.RepositoryConstraintViolationException;
import org.springframework.data.rest.webmvc.support.RepositoryConstraintViolationExceptionMessage;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;
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
public class CommonExceptionHandler extends ResponseEntityExceptionHandler {

  private final MessageSourceAccessor messageSourceAccessor;

  public CommonExceptionHandler(MessageSource messageSource) {
    Assert.notNull(messageSource, "MessageSource must not be null!");
    this.messageSourceAccessor = new MessageSourceAccessor(messageSource);
  }

  /**
   * Handles a {@link RequirementException} SQL exception.
   *
   * @param exception the exception.
   * @return a 400 error
   */
  @ExceptionHandler(RequirementException.class)
  public ResponseEntity<ErrorResponse> databaseSQLException(RequirementException exception, @NonNull WebRequest request) {
    ErrorResponse response = ErrorResponse.builder()
      .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
      .message(exception.getLocalizedMessage())
      .status(HttpStatus.BAD_REQUEST.value())
      .timestamp(LocalDateTime.now(ZoneOffset.UTC))
      .path(((ServletWebRequest) request).getRequest().getRequestURI())
      .build();
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }

  @ExceptionHandler(RepositoryConstraintViolationException.class)
  ResponseEntity<RepositoryConstraintViolationExceptionMessage> handleRepositoryConstraintViolationException(
    RepositoryConstraintViolationException o_O) {
    return new ResponseEntity<>(new RepositoryConstraintViolationExceptionMessage(o_O, messageSourceAccessor),
      new HttpHeaders(), HttpStatus.BAD_REQUEST);
  }

  @Override
  @NonNull
  protected ResponseEntity<Object> handleHttpMessageNotReadable(
    HttpMessageNotReadableException exception, @NonNull HttpHeaders headers, @NonNull HttpStatus status, @NonNull WebRequest request) {
    ErrorResponse response = ErrorResponse.builder()
      .error(HttpStatus.UNPROCESSABLE_ENTITY.getReasonPhrase())
      .message(exception.getLocalizedMessage())
      .status(HttpStatus.UNPROCESSABLE_ENTITY.value())
      .timestamp(LocalDateTime.now(ZoneOffset.UTC))
      .path(((ServletWebRequest) request).getRequest().getRequestURI())
      .build();
    return handleExceptionInternal(exception, response, headers, HttpStatus.UNPROCESSABLE_ENTITY, request);
  }
}
