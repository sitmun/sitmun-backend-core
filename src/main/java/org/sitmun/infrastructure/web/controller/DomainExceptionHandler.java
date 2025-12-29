package org.sitmun.infrastructure.web.controller;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.NonUniqueResultException;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PessimisticLockException;
import jakarta.persistence.TransactionRequiredException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import org.sitmun.infrastructure.persistence.exception.BusinessRuleException;
import org.sitmun.infrastructure.persistence.exception.RequirementException;
import org.sitmun.infrastructure.persistence.type.codelist.ImmutableSystemCodeListValueException;
import org.sitmun.infrastructure.security.password.exception.EmailTemplateException;
import org.sitmun.infrastructure.security.password.exception.MailNotImplementedException;
import org.sitmun.infrastructure.web.dto.ProblemDetail;
import org.sitmun.infrastructure.web.dto.ProblemTypes;
import org.sitmun.infrastructure.web.dto.ValidationProblemDetail;
import org.springframework.beans.TypeMismatchException;
import org.springframework.context.MessageSource;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.rest.core.RepositoryConstraintViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Global exception handler that converts exceptions to RFC 9457 Problem Detail responses.
 *
 * <p>This handler catches various exception types and returns standardized RFC 9457 Problem Detail
 * responses with appropriate HTTP status codes and problem type URIs.
 *
 * <p>All responses use Content-Type: application/problem+json
 *
 * @see <a href="https://www.rfc-editor.org/rfc/rfc9457.html">RFC 9457</a>
 */
@ControllerAdvice
public class DomainExceptionHandler extends ResponseEntityExceptionHandler {

  private final MessageSourceAccessor messageSourceAccessor;

  public DomainExceptionHandler(MessageSource messageSource) {
    Assert.notNull(messageSource, "MessageSource must not be null!");
    messageSourceAccessor = new MessageSourceAccessor(messageSource);
  }

  // ============================================================================
  // BUSINESS RULE VIOLATIONS
  // ============================================================================

  /**
   * Handles {@link BusinessRuleException} - domain business rules violations with specific problem
   * types.
   */
  @ExceptionHandler(BusinessRuleException.class)
  public ResponseEntity<ProblemDetail> handleBusinessRuleException(
      BusinessRuleException exception, HttpServletRequest request) {
    logger.info("Business rule violation: " + exception.getMessage(), exception);

    ProblemDetail problem =
        ProblemDetail.builder()
            .type(exception.getProblemType())
            .status(HttpStatus.BAD_REQUEST.value())
            .title(HttpStatus.BAD_REQUEST.getReasonPhrase())
            .detail(exception.getMessage())
            .instance(request.getRequestURI())
            .build();

    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problem);
  }

  /**
   * Handles generic {@link RequirementException} - fallback for requirement violations without
   * specific problem types.
   */
  @ExceptionHandler(RequirementException.class)
  public ResponseEntity<ProblemDetail> handleRequirementException(
      RequirementException exception, HttpServletRequest request) {
    logger.info("Requirement violation: " + exception.getMessage(), exception);

    ProblemDetail problem =
        ProblemDetail.builder()
            .type(ProblemTypes.BAD_REQUEST)
            .status(HttpStatus.BAD_REQUEST.value())
            .title(HttpStatus.BAD_REQUEST.getReasonPhrase())
            .detail(exception.getMessage())
            .instance(request.getRequestURI())
            .build();

    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problem);
  }

  // ============================================================================
  // VALIDATION ERRORS
  // ============================================================================

  /**
   * Handles {@link RepositoryConstraintViolationException} - Spring Data REST validation errors.
   */
  @ExceptionHandler(RepositoryConstraintViolationException.class)
  public ResponseEntity<ValidationProblemDetail> handleRepositoryConstraintViolationException(
      RepositoryConstraintViolationException exception, HttpServletRequest request) {
    logger.info("Repository constraint violation: " + exception.getMessage(), exception);

    List<ValidationProblemDetail.FieldError> fieldErrors = new ArrayList<>();
    exception
        .getErrors()
        .getFieldErrors()
        .forEach(
            error -> fieldErrors.add(
                ValidationProblemDetail.FieldError.builder()
                    .field(error.getField())
                    .rejectedValue(error.getRejectedValue())
                    .message(messageSourceAccessor.getMessage(error))
                    .build()));

    ValidationProblemDetail problem =
        ValidationProblemDetail.validationBuilder()
            .type(ProblemTypes.VALIDATION_ERROR)
            .status(HttpStatus.BAD_REQUEST.value())
            .title("Validation Failed")
            .detail("Request validation failed. Please check the errors field for details.")
            .instance(request.getRequestURI())
            .errors(fieldErrors)
            .build();

    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problem);
  }

  // ============================================================================
  // JPA/HIBERNATE PERSISTENCE EXCEPTIONS
  // ============================================================================

  /** Handles {@link EntityNotFoundException} - requested entity not found. */
  @ExceptionHandler(EntityNotFoundException.class)
  public ResponseEntity<ProblemDetail> handleEntityNotFoundException(
      EntityNotFoundException exception, HttpServletRequest request) {
    logger.info("Entity not found: " + exception.getMessage(), exception);

    ProblemDetail problem =
        ProblemDetail.builder()
            .type(ProblemTypes.ENTITY_NOT_FOUND)
            .status(HttpStatus.NOT_FOUND.value())
            .title("Entity Not Found")
            .detail(exception.getMessage())
            .instance(request.getRequestURI())
            .build();

    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problem);
  }

  /** Handles {@link OptimisticLockException} - concurrent modification detected. */
  @ExceptionHandler(OptimisticLockException.class)
  public ResponseEntity<ProblemDetail> handleOptimisticLockException(
      OptimisticLockException exception, HttpServletRequest request) {
    logger.warn("Optimistic lock failure: " + exception.getMessage(), exception);

    ProblemDetail problem =
        ProblemDetail.builder()
            .type(ProblemTypes.OPTIMISTIC_LOCK_FAILURE)
            .status(HttpStatus.CONFLICT.value())
            .title("Concurrent Modification Detected")
            .detail(
                "The resource has been modified by another user. Please refresh and try again.")
            .instance(request.getRequestURI())
            .build();

    return ResponseEntity.status(HttpStatus.CONFLICT)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problem);
  }

  /** Handles {@link PessimisticLockException} - resource is locked. */
  @ExceptionHandler(PessimisticLockException.class)
  public ResponseEntity<ProblemDetail> handlePessimisticLockException(
      PessimisticLockException exception, HttpServletRequest request) {
    logger.warn("Pessimistic lock failure: " + exception.getMessage(), exception);

    ProblemDetail problem =
        ProblemDetail.builder()
            .type(ProblemTypes.PESSIMISTIC_LOCK_FAILURE)
            .status(HttpStatus.CONFLICT.value())
            .title("Resource Locked")
            .detail("The resource is currently locked by another operation. Please try again later.")
            .instance(request.getRequestURI())
            .build();

    return ResponseEntity.status(HttpStatus.CONFLICT)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problem);
  }

  /** Handles {@link DataIntegrityViolationException} - database constraint violations. */
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ProblemDetail> handleDataIntegrityViolationException(
      DataIntegrityViolationException exception, HttpServletRequest request) {
    logger.warn("Data integrity violation: " + exception.getMessage(), exception);

    // Check if it's a duplicate key/unique constraint violation (409 Conflict)
    // vs other integrity violations like foreign keys (422 Unprocessable Entity)
    boolean isDuplicateKey = isIsDuplicateKey(exception);

    HttpStatus status = isDuplicateKey ? HttpStatus.CONFLICT : HttpStatus.UNPROCESSABLE_ENTITY;
    String detail = isDuplicateKey 
        ? "A resource with this value already exists." 
        : "The operation violates database constraints (foreign key, etc.).";

    ProblemDetail problem =
        ProblemDetail.builder()
            .type(ProblemTypes.DATA_INTEGRITY_VIOLATION)
            .status(status.value())
            .title(isDuplicateKey ? "Resource Already Exists" : "Data Integrity Violation")
            .detail(detail)
            .instance(request.getRequestURI())
            .build();

    return ResponseEntity.status(status)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problem);
  }

  private static boolean isIsDuplicateKey(DataIntegrityViolationException exception) {
    String message = exception.getMessage();
    boolean isDuplicateKey = false;
    if (message != null) {
      String lowerMessage = message.toLowerCase();
      isDuplicateKey = lowerMessage.contains("unique constraint")
          || lowerMessage.contains("duplicate key")
          || lowerMessage.contains("duplicate entry")
          || lowerMessage.contains("uniqueconstraint")
          || (exception.getCause() != null
              && exception.getCause() instanceof org.hibernate.exception.ConstraintViolationException
              && message.contains("constraint"));
    }
    return isDuplicateKey;
  }

  /** Handles Hibernate {@link jakarta.validation.ConstraintViolationException}. */
  @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
  public ResponseEntity<ProblemDetail> handleConstraintViolationException(
      jakarta.validation.ConstraintViolationException exception, HttpServletRequest request) {
    logger.info("Constraint violation: " + exception.getMessage(), exception);

    ProblemDetail problem =
        ProblemDetail.builder()
            .type(ProblemTypes.CONSTRAINT_VIOLATION)
            .status(HttpStatus.UNPROCESSABLE_ENTITY.value())
            .title("Constraint Violation")
            .detail(exception.getMessage())
            .instance(request.getRequestURI())
            .build();

    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problem);
  }

  /** Handles {@link NonUniqueResultException} - query returned multiple results. */
  @ExceptionHandler(NonUniqueResultException.class)
  public ResponseEntity<ProblemDetail> handleNonUniqueResultException(
      NonUniqueResultException exception, HttpServletRequest request) {
    logger.error("Non-unique result: " + exception.getMessage(), exception);

    ProblemDetail problem =
        ProblemDetail.builder()
            .type(ProblemTypes.NON_UNIQUE_RESULT)
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .title("Non-Unique Result")
            .detail("Query returned multiple results when only one was expected.")
            .instance(request.getRequestURI())
            .build();

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problem);
  }

  /** Handles {@link TransactionRequiredException} - no transaction context. */
  @ExceptionHandler(TransactionRequiredException.class)
  public ResponseEntity<ProblemDetail> handleTransactionRequiredException(
      TransactionRequiredException exception, HttpServletRequest request) {
    logger.error("Transaction required: " + exception.getMessage(), exception);

    ProblemDetail problem =
        ProblemDetail.builder()
            .type(ProblemTypes.TRANSACTION_REQUIRED)
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .title("Transaction Required")
            .detail("This operation requires an active transaction context.")
            .instance(request.getRequestURI())
            .build();

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problem);
  }

  // ============================================================================
  // DOMAIN-SPECIFIC EXCEPTIONS
  // ============================================================================

  /** Handles {@link MailNotImplementedException} - mail service not available. */
  @ExceptionHandler(MailNotImplementedException.class)
  public ResponseEntity<ProblemDetail> handleMailNotImplementedException(
      MailNotImplementedException exception, HttpServletRequest request) {
    logger.error("Mail service not available: " + exception.getMessage(), exception);

    ProblemDetail problem =
        ProblemDetail.builder()
            .type(ProblemTypes.MAIL_NOT_AVAILABLE)
            .status(HttpStatus.SERVICE_UNAVAILABLE.value())
            .title("Mail Service Not Available")
            .detail(exception.getMessage())
            .instance(request.getRequestURI())
            .build();

    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problem);
  }

  /** Handles {@link EmailTemplateException} - email template rendering error. */
  @ExceptionHandler(EmailTemplateException.class)
  public ResponseEntity<ProblemDetail> handleEmailTemplateException(
      EmailTemplateException exception, HttpServletRequest request) {
    logger.error("Email template error: " + exception.getMessage(), exception);

    ProblemDetail problem =
        ProblemDetail.builder()
            .type(ProblemTypes.EMAIL_TEMPLATE_ERROR)
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .title("Email Template Error")
            .detail("Error processing email template.")
            .instance(request.getRequestURI())
            .build();

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problem);
  }

  /** Handles {@link ImmutableSystemCodeListValueException} - cannot modify a system code list. */
  @ExceptionHandler(ImmutableSystemCodeListValueException.class)
  public ResponseEntity<ProblemDetail> handleImmutableSystemCodeListValueException(
      ImmutableSystemCodeListValueException exception, HttpServletRequest request) {
    logger.warn("Attempt to modify immutable code list: " + exception.getMessage(), exception);

    ProblemDetail problem =
        ProblemDetail.builder()
            .type(ProblemTypes.IMMUTABLE_CODELIST_VALUE)
            .status(HttpStatus.BAD_REQUEST.value())
            .title("Cannot Modify System Code List")
            .detail(exception.getMessage())
            .instance(request.getRequestURI())
            .build();

    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problem);
  }

  // ============================================================================
  // SPRING FRAMEWORK EXCEPTIONS - Override parent class methods
  // ============================================================================

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      @NonNull MethodArgumentNotValidException ex,
      @NonNull HttpHeaders headers,
      @NonNull HttpStatusCode status,
      @NonNull WebRequest request) {

    List<ValidationProblemDetail.FieldError> fieldErrors = new ArrayList<>();
    for (FieldError error : ex.getBindingResult().getFieldErrors()) {
      fieldErrors.add(
          ValidationProblemDetail.FieldError.builder()
              .field(error.getField())
              .rejectedValue(error.getRejectedValue())
              .expectedType(error.getCode())
              .message(error.getDefaultMessage())
              .build());
    }

    ValidationProblemDetail problem =
        ValidationProblemDetail.validationBuilder()
            .type(ProblemTypes.METHOD_ARGUMENT_NOT_VALID)
            .status(HttpStatus.BAD_REQUEST.value())
            .title("Method Argument Validation Failed")
            .detail("One or more request parameters failed validation.")
            .instance(getRequestURI(request))
            .errors(fieldErrors)
            .build();

    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problem);
  }

  @Override
  protected ResponseEntity<Object> handleTypeMismatch(
      @NonNull TypeMismatchException ex,
      @NonNull HttpHeaders headers,
      @NonNull HttpStatusCode status,
      @NonNull WebRequest request) {

    ProblemDetail problem =
        ProblemDetail.builder()
            .type(ProblemTypes.TYPE_MISMATCH)
            .status(HttpStatus.BAD_REQUEST.value())
            .title("Type Mismatch")
            .detail(
                String.format(
                    "Failed to convert value '%s' to type '%s'",
                    ex.getValue(), ex.getRequiredType()))
            .instance(getRequestURI(request))
            .build();

    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problem);
  }

  @Override
  protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(
      @NonNull HttpRequestMethodNotSupportedException ex,
      @NonNull HttpHeaders headers,
      @NonNull HttpStatusCode status,
      @NonNull WebRequest request) {

    ProblemDetail problem =
        ProblemDetail.builder()
            .type(ProblemTypes.METHOD_NOT_SUPPORTED)
            .status(HttpStatus.METHOD_NOT_ALLOWED.value())
            .title("Method Not Supported")
            .detail(String.format("HTTP method '%s' is not supported for this endpoint.", ex.getMethod()))
            .instance(getRequestURI(request))
            .build();

    return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problem);
  }

  @Override
  protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(
      @NonNull HttpMediaTypeNotSupportedException ex,
      @NonNull HttpHeaders headers,
      @NonNull HttpStatusCode status,
      @NonNull WebRequest request) {

    ProblemDetail problem =
        ProblemDetail.builder()
            .type(ProblemTypes.MEDIA_TYPE_NOT_SUPPORTED)
            .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value())
            .title("Media Type Not Supported")
            .detail(String.format("Content-Type '%s' is not supported.", ex.getContentType()))
            .instance(getRequestURI(request))
            .build();

    return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problem);
  }

  @Override
  protected ResponseEntity<Object> handleHttpMediaTypeNotAcceptable(
      @NonNull HttpMediaTypeNotAcceptableException ex,
      @NonNull HttpHeaders headers,
      @NonNull HttpStatusCode status,
      @NonNull WebRequest request) {

    ProblemDetail problem =
        ProblemDetail.builder()
            .type(ProblemTypes.MEDIA_TYPE_NOT_ACCEPTABLE)
            .status(HttpStatus.NOT_ACCEPTABLE.value())
            .title("Media Type Not Acceptable")
            .detail("The requested media type cannot be produced.")
            .instance(getRequestURI(request))
            .build();

    return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problem);
  }

  @Override
  protected ResponseEntity<Object> handleMissingPathVariable(
      @NonNull MissingPathVariableException ex,
      @NonNull HttpHeaders headers,
      @NonNull HttpStatusCode status,
      @NonNull WebRequest request) {

    ProblemDetail problem =
        ProblemDetail.builder()
            .type(ProblemTypes.MISSING_PATH_VARIABLE)
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .title("Missing Path Variable")
            .detail(String.format("Required path variable '%s' is missing.", ex.getVariableName()))
            .instance(getRequestURI(request))
            .build();

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problem);
  }

  @Override
  protected ResponseEntity<Object> handleMissingServletRequestParameter(
      @NonNull MissingServletRequestParameterException ex,
      @NonNull HttpHeaders headers,
      @NonNull HttpStatusCode status,
      @NonNull WebRequest request) {

    ProblemDetail problem =
        ProblemDetail.builder()
            .type(ProblemTypes.MISSING_REQUEST_PARAMETER)
            .status(HttpStatus.BAD_REQUEST.value())
            .title("Missing Request Parameter")
            .detail(
                String.format(
                    "Required parameter '%s' of type '%s' is missing.",
                    ex.getParameterName(), ex.getParameterType()))
            .instance(getRequestURI(request))
            .build();

    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problem);
  }

  @Override
  protected ResponseEntity<Object> handleNoHandlerFoundException(
      @NonNull NoHandlerFoundException ex,
      @NonNull HttpHeaders headers,
      @NonNull HttpStatusCode status,
      @NonNull WebRequest request) {

    ProblemDetail problem =
        ProblemDetail.builder()
            .type(ProblemTypes.NO_HANDLER_FOUND)
            .status(HttpStatus.NOT_FOUND.value())
            .title("No Handler Found")
            .detail(String.format("No handler found for %s %s", ex.getHttpMethod(), ex.getRequestURL()))
            .instance(getRequestURI(request))
            .build();

    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problem);
  }

  @Override
  protected ResponseEntity<Object> handleAsyncRequestTimeoutException(
      @NonNull AsyncRequestTimeoutException ex,
      @NonNull HttpHeaders headers,
      @NonNull HttpStatusCode status,
      @NonNull WebRequest request) {

    ProblemDetail problem =
        ProblemDetail.builder()
            .type(ProblemTypes.ASYNC_REQUEST_TIMEOUT)
            .status(HttpStatus.SERVICE_UNAVAILABLE.value())
            .title("Request Timeout")
            .detail("The request timed out. Please try again.")
            .instance(getRequestURI(request))
            .build();

    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problem);
  }

  // ============================================================================
  // SPRING SECURITY EXCEPTIONS
  // ============================================================================

  /**
   * Handles {@link org.springframework.security.access.AccessDeniedException} - Spring Security
   * access denied. Returns 401 if a user is not authenticated (anonymous), 403 if authenticated but
   * not authorized.
   */
  @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
  public ResponseEntity<ProblemDetail> handleAccessDeniedException(
      org.springframework.security.access.AccessDeniedException exception,
      HttpServletRequest request) {
    logger.warn("Access denied: " + exception.getMessage(), exception);

    // Check if a user is authenticated (not anonymous)
    org.springframework.security.core.Authentication authentication =
        org.springframework.security.core.context.SecurityContextHolder.getContext()
            .getAuthentication();
    boolean isAnonymous =
        authentication == null
            || !authentication.isAuthenticated()
            || authentication instanceof
                org.springframework.security.authentication.AnonymousAuthenticationToken;

    // For anonymous users, return 401 Unauthorized (need to authenticate)
    // For authenticated users, return 403 Forbidden (not authorized for this resource)
    HttpStatus status = isAnonymous ? HttpStatus.UNAUTHORIZED : HttpStatus.FORBIDDEN;
    String problemType = isAnonymous ? ProblemTypes.UNAUTHORIZED : ProblemTypes.FORBIDDEN;
    String title = isAnonymous ? "Unauthorized" : "Access Denied";

    ProblemDetail problem =
        ProblemDetail.builder()
            .type(problemType)
            .status(status.value())
            .title(title)
            .detail(exception.getMessage())
            .instance(request.getRequestURI())
            .build();

    return ResponseEntity.status(status)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problem);
  }

  // ============================================================================
  // SPRING DATA REST EXCEPTIONS
  // ============================================================================

  /**
   * Handles {@link org.springframework.data.rest.webmvc.ResourceNotFoundException} - Spring Data
   * REST resource not found.
   */
  @ExceptionHandler(org.springframework.data.rest.webmvc.ResourceNotFoundException.class)
  public ResponseEntity<ProblemDetail> handleResourceNotFoundException(
      org.springframework.data.rest.webmvc.ResourceNotFoundException exception,
      HttpServletRequest request) {
    logger.info("Resource not found: " + exception.getMessage(), exception);

    ProblemDetail problem =
        ProblemDetail.builder()
            .type(ProblemTypes.NOT_FOUND)
            .status(HttpStatus.NOT_FOUND.value())
            .title("Resource Not Found")
            .detail("The requested resource was not found.")
            .instance(request.getRequestURI())
            .build();

    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problem);
  }

  // ============================================================================
  // GENERIC EXCEPTION HANDLER (Fallback)
  // ============================================================================

  /** Fallback handler for any unhandled exceptions. */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ProblemDetail> handleAnyException(
      Exception exception, HttpServletRequest request) {
    logger.error("Unhandled exception", exception);

    ProblemDetail problem =
        ProblemDetail.builder()
            .type(ProblemTypes.INTERNAL_SERVER_ERROR)
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .title("Internal Server Error")
            .detail("An unexpected error occurred. Please contact support if the problem persists.")
            .instance(request.getRequestURI())
            .build();

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problem);
  }

  // ============================================================================
  // HELPER METHODS
  // ============================================================================

  private String getRequestURI(WebRequest request) {
    if (request instanceof ServletWebRequest servletRequest) {
      return servletRequest.getRequest().getRequestURI();
    }
    return null;
  }
}
