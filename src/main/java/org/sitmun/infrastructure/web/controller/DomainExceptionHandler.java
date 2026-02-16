package org.sitmun.infrastructure.web.controller;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.NonUniqueResultException;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PessimisticLockException;
import jakarta.persistence.TransactionRequiredException;
import jakarta.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.hibernate.LazyInitializationException;
import org.hibernate.exception.ConstraintViolationException;
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

    HttpStatus status =
        ProblemTypes.DATA_INTEGRITY_VIOLATION.equals(exception.getProblemType())
            ? HttpStatus.UNPROCESSABLE_ENTITY
            : HttpStatus.BAD_REQUEST;

    ProblemDetail problem =
        ProblemDetail.builder()
            .type(exception.getProblemType())
            .status(status.value())
            .title(status.getReasonPhrase())
            .detail(exception.getMessage())
            .instance(request.getRequestURI())
            .build();

    if (ProblemTypes.DATA_INTEGRITY_VIOLATION.equals(exception.getProblemType())) {
      problem.addProperty("referencingEntityTranslationKey", "entity.tree-node.plural");
    }

    return ResponseEntity.status(status)
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
            error ->
                fieldErrors.add(
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
            .detail("The resource has been modified by another user. Please refresh and try again.")
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
            .detail(
                "The resource is currently locked by another operation. Please try again later.")
            .instance(request.getRequestURI())
            .build();

    return ResponseEntity.status(HttpStatus.CONFLICT)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problem);
  }

  /**
   * Handles {@link DataIntegrityViolationException} - database constraint violations.
   *
   * <p>Returns 409 Conflict for unique constraint violations (duplicate key), or 422 Unprocessable
   * Entity for foreign key violations and other integrity issues.
   *
   * <p>All exception property access is wrapped in try-catch blocks to guard against {@link
   * LazyInitializationException} which can occur when Spring Data REST's internal processing has
   * closed the Hibernate session.
   */
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ProblemDetail> handleDataIntegrityViolationException(
      DataIntegrityViolationException exception, HttpServletRequest request) {

    // All processing is wrapped to catch any LazyInitializationException
    boolean isDuplicateKey = false;
    ConstraintInfo constraintInfo = null;

    try {
      isDuplicateKey = isDuplicateKey(exception);
      constraintInfo = extractConstraintInfo(exception);
    } catch (LazyInitializationException e) {
      // Log WARN with stack trace to identify where lazy access occurs during constraint extraction
      logger.warn(
          "Lazy initialization exception during constraint extraction. method="
              + request.getMethod()
              + " uri="
              + request.getRequestURI(),
          e);
    } catch (Exception e) {
      logger.debug("Could not extract constraint info, using defaults");
    }

    HttpStatus status = isDuplicateKey ? HttpStatus.CONFLICT : HttpStatus.UNPROCESSABLE_ENTITY;

    // Context-aware error messages based on HTTP method
    String detail;
    if (isDuplicateKey) {
      detail = "A resource with this value already exists.";
    } else {
      String method = request.getMethod();
      if ("DELETE".equals(method)) {
        detail = "Cannot delete this resource because it is being used by other resources";
      } else if ("POST".equals(method)) {
        detail =
            "Cannot create this resource. A referenced resource does not exist or constraints are violated";
      } else if ("PUT".equals(method) || "PATCH".equals(method)) {
        detail =
            "Cannot update this resource. A referenced resource does not exist or constraints are violated";
      } else {
        detail = "This operation violates database constraints";
      }
    }

    ProblemDetail problem =
        ProblemDetail.builder()
            .type(ProblemTypes.DATA_INTEGRITY_VIOLATION)
            .status(status.value())
            .title(isDuplicateKey ? "Resource Already Exists" : "Data Integrity Violation")
            .detail(detail)
            .instance(request.getRequestURI())
            .build();

    // Add entity translation key for foreign key violations
    if (!isDuplicateKey
        && constraintInfo != null
        && constraintInfo.referencingEntityTranslationKey() != null) {
      problem.addProperty(
          "referencingEntityTranslationKey", constraintInfo.referencingEntityTranslationKey());
    }

    return ResponseEntity.status(status)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problem);
  }

  /**
   * Handles {@link LazyInitializationException} - Hibernate lazy loading failures.
   *
   * <p>This exception typically occurs when trying to access lazy-loaded properties after the
   * Hibernate session has closed. This can happen during exception handling when the logging
   * framework or exception processing tries to access exception properties that contain lazy-loaded
   * collections.
   *
   * <p>This handler treats it as a data integrity violation and returns a generic error response
   * without attempting to access any potentially lazy-loaded properties.
   */
  @ExceptionHandler(LazyInitializationException.class)
  public ResponseEntity<ProblemDetail> handleLazyInitializationException(
      LazyInitializationException exception, HttpServletRequest request) {
    // This is an internal server error. It can happen if some code tries to access a lazy
    // association after the Hibernate session has closed (often during error
    // handling/serialization).
    //
    // Log the stack trace so we can identify the real access point (the generic warning alone
    // is not actionable).
    logger.warn(
        "Lazy initialization exception occurred. method="
            + request.getMethod()
            + " uri="
            + request.getRequestURI(),
        exception);

    ProblemDetail problem =
        ProblemDetail.builder()
            .type(ProblemTypes.INTERNAL_SERVER_ERROR)
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .title("Internal Server Error")
            .detail("An internal error occurred while processing the request.")
            .instance(request.getRequestURI())
            .build();

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problem);
  }

  /**
   * Data extracted from a DataIntegrityViolationException for constraint analysis.
   *
   * @param constraintViolation the Hibernate ConstraintViolationException, or null if not available
   * @param sqlException the underlying SQLException, or null if not available
   * @param sqlState the SQL state code (e.g., "23503"), or null if not available
   * @param constraintName the constraint name (e.g., "STM_TAS_FK_SER"), or null if not available
   */
  private record ConstraintViolationData(
      ConstraintViolationException constraintViolation,
      SQLException sqlException,
      String sqlState,
      String constraintName) {}

  /**
   * Extracts constraint violation details from a DataIntegrityViolationException.
   *
   * <p>This helper method centralizes the extraction of constraint information from the exception
   * chain, reducing code duplication between {@link #isDuplicateKey} and {@link
   * #extractConstraintInfo}.
   *
   * @param exception the exception to analyze
   * @return extracted constraint data, with null fields if information is unavailable
   */
  private static ConstraintViolationData extractConstraintViolationData(
      DataIntegrityViolationException exception) {
    if (exception == null) {
      return new ConstraintViolationData(null, null, null, null);
    }

    try {
      // Safely get cause - may trigger lazy loading
      Throwable cause = null;
      try {
        cause = exception.getCause();
      } catch (Exception e) {
        // getCause() may trigger lazy loading - return empty data
        return new ConstraintViolationData(null, null, null, null);
      }

      if (cause == null || !(cause instanceof ConstraintViolationException constraintViolation)) {
        return new ConstraintViolationData(null, null, null, null);
      }

      SQLException sqlException = null;
      String sqlState = null;
      String constraintName = null;

      try {
        sqlException = constraintViolation.getSQLException();
        sqlState = sqlException != null ? sqlException.getSQLState() : null;
      } catch (Exception e) {
        // Ignore - sqlException or sqlState may be unavailable or trigger lazy loading
        // Cannot log here as this is a static method
      }

      try {
        constraintName = constraintViolation.getConstraintName();
      } catch (Exception e) {
        // Ignore - constraint name may be unavailable or trigger lazy loading
        // Cannot log here as this is a static method
      }

      return new ConstraintViolationData(
          constraintViolation, sqlException, sqlState, constraintName);
    } catch (Exception e) {
      // If any exception occurs during extraction (including LazyInitializationException),
      // return empty data to prevent cascading failures
      // Cannot log here as this is a static method
      return new ConstraintViolationData(null, null, null, null);
    }
  }

  /**
   * Checks if a DataIntegrityViolationException is due to a duplicate key/unique constraint
   * violation.
   *
   * <p>Examines the exception structure to distinguish between:
   *
   * <ul>
   *   <li>Duplicate key violations (409 Conflict) - unique constraint violations
   *   <li>Foreign key violations (422 Unprocessable Entity) - foreign key constraint violations
   * </ul>
   *
   * <p>Uses SQL state codes for reliable detection (PostgreSQL: 23505 for unique, 23503 for FK;
   * Oracle: 23001 for unique), with constraint name pattern matching as fallback.
   *
   * @param exception the DataIntegrityViolationException to examine
   * @return true if it's a duplicate key violation, false otherwise
   */
  private static boolean isDuplicateKey(DataIntegrityViolationException exception) {
    ConstraintViolationData data = extractConstraintViolationData(exception);

    // Check SQL state code first (most reliable)
    if (data.sqlState() != null) {
      // 23505 = unique constraint violation (PostgreSQL)
      // 23001 = restrict violation (Oracle unique constraint)
      if ("23505".equals(data.sqlState()) || "23001".equals(data.sqlState())) {
        return true;
      }
      // 23503 = foreign key constraint violation (PostgreSQL) - explicitly exclude
      if ("23503".equals(data.sqlState())) {
        return false;
      }
    }

    // Check constraint name patterns
    if (data.constraintName() != null) {
      String upperConstraintName = data.constraintName().toUpperCase();
      // Foreign key constraints typically contain _FK_ or FK_
      if (upperConstraintName.contains("_FK_")
          || upperConstraintName.contains("FK_")
          || upperConstraintName.contains("FOREIGN")) {
        return false;
      }
      // Unique constraints typically contain _UK, UK_, UNIQUE
      if (upperConstraintName.contains("_UK")
          || upperConstraintName.contains("UK_")
          || upperConstraintName.contains("UNIQUE")) {
        return true;
      }
    }

    // No fallback to exception.getMessage() - it can trigger LazyInitializationException
    // if the exception contains lazy-loaded entity references. We rely on SQL state
    // codes and constraint name patterns which are safe to access.
    return false;
  }

  /**
   * Information about a database constraint violation.
   *
   * @param constraintName the name of the constraint
   * @param referencingEntityTranslationKey translation key for the entity that references this
   *     resource (e.g., "entity.task.plural")
   * @param isForeignKey whether this is a foreign key constraint
   */
  private record ConstraintInfo(
      String constraintName, String referencingEntityTranslationKey, boolean isForeignKey) {}

  /**
   * Extracts constraint information from a DataIntegrityViolationException.
   *
   * <p>Maps constraint names to entity translation keys for user-friendly error messages. Uses SQL
   * state codes for reliable foreign key detection, with constraint name pattern matching as
   * fallback.
   *
   * @param exception the DataIntegrityViolationException to examine
   * @return ConstraintInfo with constraint details, or null if information cannot be extracted
   */
  private static ConstraintInfo extractConstraintInfo(DataIntegrityViolationException exception) {
    ConstraintViolationData data = extractConstraintViolationData(exception);

    if (data.constraintViolation() == null || data.constraintName() == null) {
      return null;
    }

    // Check SQL state code first for foreign key detection (most reliable)
    boolean isForeignKey = false;
    if (data.sqlState() != null) {
      // 23503 = foreign key constraint violation (PostgreSQL)
      isForeignKey = "23503".equals(data.sqlState());
    }

    // Fallback to constraint name pattern matching
    if (!isForeignKey && data.constraintName() != null) {
      String upperConstraintName = data.constraintName().toUpperCase();
      isForeignKey =
          upperConstraintName.contains("_FK_")
              || upperConstraintName.contains("FK_")
              || upperConstraintName.contains("FOREIGN");
    }

    if (!isForeignKey) {
      return new ConstraintInfo(data.constraintName(), null, false);
    }

    // Map constraint names to entity translation keys
    String entityTranslationKey = mapConstraintToEntityTranslationKey(data.constraintName());

    return new ConstraintInfo(data.constraintName(), entityTranslationKey, true);
  }

  /**
   * Maps constraint names to entity translation keys.
   *
   * <p>Uses pattern matching to identify which entity type is referencing the resource based on the
   * constraint name. Patterns are checked in order of specificity (most specific first) to avoid
   * false matches.
   *
   * <p>Pattern matching priority:
   *
   * <ol>
   *   <li>Priority 1: Most specific patterns (multi-part with FK, e.g., "TNO_FK_TAS")
   *   <li>Priority 2: Specific FK patterns (e.g., "TAS_FK_SER")
   *   <li>Priority 3: General entity prefixes (e.g., "TAS_")
   *   <li>Priority 4: Least specific patterns
   * </ol>
   *
   * @param constraintName the database constraint name (e.g., "STM_TAS_FK_SER")
   * @return the entity translation key (e.g., "entity.task.plural"), or null if not found
   */
  private static String mapConstraintToEntityTranslationKey(String constraintName) {
    if (constraintName == null) {
      return null;
    }

    String upperConstraintName = constraintName.toUpperCase();

    // LinkedHashMap preserves insertion order for guaranteed specificity
    // Note: Translation keys should match existing frontend keys (entity.{entity}.plural)
    Map<String, String> constraintPatterns = new LinkedHashMap<>();

    // Priority 1: Most specific patterns (multi-part with FK)
    // Format: {REFERENCING_ENTITY}_FK_{REFERENCED_ENTITY}
    // When deleting the referenced entity, the error should mention the referencing entity
    constraintPatterns.put("TNO_FK_TAS", "entity.tree-node.plural"); // Tree nodes referencing Task
    constraintPatterns.put(
        "TNO_FK_GEO", "entity.tree-node.plural"); // Tree nodes referencing Cartography
    constraintPatterns.put("TNO_FK_TRE", "entity.tree-node.plural"); // Tree nodes referencing Tree
    constraintPatterns.put(
        "TNO_FK_TNO", "entity.tree-node.plural"); // Tree nodes referencing Tree Node (parent)

    // Priority 2: Specific FK patterns
    // Format: {REFERENCING_ENTITY}_FK_{REFERENCED_ENTITY}
    constraintPatterns.put("TAS_FK_GEO", "entity.task.plural"); // Tasks referencing Cartography
    constraintPatterns.put("TAS_FK_SER", "entity.task.plural"); // Tasks referencing Service
    constraintPatterns.put("TAS_FK_CON", "entity.task.plural"); // Tasks referencing Connection
    constraintPatterns.put(
        "GEO_FK_SER", "entity.cartography.plural"); // Cartographies referencing Service
    constraintPatterns.put(
        "GEO_FK_CON", "entity.cartography.plural"); // Cartographies referencing Connection
    constraintPatterns.put(
        "ABC_FK_APP",
        "entity.background.plural"); // Application backgrounds referencing Application
    constraintPatterns.put(
        "ABC_FK_FON", "entity.background.plural"); // Application backgrounds referencing Background
    constraintPatterns.put(
        "ARO_FK_APP", "entity.role.plural"); // Application roles referencing Application
    constraintPatterns.put(
        "ARO_FK_ROL", "entity.role.plural"); // Application roles referencing Role
    constraintPatterns.put(
        "ATR_FK_APP", "entity.tree.plural"); // Application trees referencing Application
    constraintPatterns.put(
        "ATR_FK_TRE", "entity.tree.plural"); // Application trees referencing Tree
    constraintPatterns.put(
        "RGG_FK_GGI",
        "entity.role.plural"); // Role cartography groups referencing Cartography Group
    constraintPatterns.put(
        "RGG_FK_ROL", "entity.role.plural"); // Role cartography groups referencing Role
    constraintPatterns.put("RTS_FK_ROL", "entity.role.plural"); // Role tasks referencing Role
    constraintPatterns.put("RTS_FK_TAS", "entity.role.plural"); // Role tasks referencing Task
    constraintPatterns.put("TRO_FK_ROL", "entity.role.plural"); // Tree roles referencing Role
    constraintPatterns.put("TRO_FK_TRE", "entity.role.plural"); // Tree roles referencing Tree
    constraintPatterns.put(
        "UCO_FK_USU", "entity.user.plural"); // User configurations referencing User
    constraintPatterns.put(
        "UCO_FK_TER", "entity.user.plural"); // User configurations referencing Territory
    constraintPatterns.put(
        "UCO_FK_ROL", "entity.user.plural"); // User configurations referencing Role
    constraintPatterns.put("POS_FK_USE", "entity.user.plural"); // User positions referencing User
    constraintPatterns.put(
        "POS_FK_TER", "entity.user.plural"); // User positions referencing Territory
    constraintPatterns.put("COM_FK_APP", "entity.user.plural"); // Comments referencing Application
    constraintPatterns.put("COM_FK_USE", "entity.user.plural"); // Comments referencing User
    constraintPatterns.put(
        "AGI_FK_GEO",
        "entity.territory.plural"); // Cartography availabilities referencing Cartography
    constraintPatterns.put(
        "AGI_FK_TER",
        "entity.territory.plural"); // Cartography availabilities referencing Territory
    constraintPatterns.put(
        "ATS_FK_TAS", "entity.territory.plural"); // Task availabilities referencing Task
    constraintPatterns.put(
        "ATS_FK_TER", "entity.territory.plural"); // Task availabilities referencing Territory
    // Note: SGI_FK_GEO, PGI_FK_GEO, PSG_FK_GEO, GGG_FK_GEO have CASCADE DELETE, so they won't block
    // deletion
    // But we include them in case CASCADE is not working or for other scenarios
    constraintPatterns.put(
        "SGI_FK_GEO",
        "entity.cartography.plural"); // Styles referencing Cartography (deprecated defaultStyle)
    // PGI, PSG, GGG have CASCADE, so they typically won't block, but include for completeness
    constraintPatterns.put(
        "FGI_FK_GEO", "entity.cartography.plural"); // Filters referencing Cartography (CASCADE)
    constraintPatterns.put(
        "GGG_FK_GGI",
        "entity.cartography-group.plural"); // Cartography permissions referencing Cartography Group

    // Priority 3: General entity prefixes (less specific)
    constraintPatterns.put("TNO_", "entity.tree-node.plural");
    constraintPatterns.put("TAS_", "entity.task.plural");
    constraintPatterns.put("GEO_", "entity.cartography.plural");
    constraintPatterns.put("APP_", "entity.application.plural");
    constraintPatterns.put("ROL_", "entity.role.plural");
    constraintPatterns.put("TER_", "entity.territory.plural");
    constraintPatterns.put("USU_", "entity.user.plural");
    constraintPatterns.put("SER_", "entity.service.plural");
    constraintPatterns.put("CONN", "entity.connection.plural");

    // Priority 4: Least specific
    constraintPatterns.put("BAC_", "entity.background.plural");

    // LinkedHashMap iteration preserves insertion order
    for (Map.Entry<String, String> entry : constraintPatterns.entrySet()) {
      if (upperConstraintName.contains(entry.getKey())) {
        return entry.getValue();
      }
    }

    return null; // No mapping found - fallback to generic message
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
            .detail(
                String.format(
                    "HTTP method '%s' is not supported for this endpoint.", ex.getMethod()))
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
            .detail(
                String.format("No handler found for %s %s", ex.getHttpMethod(), ex.getRequestURL()))
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
            || authentication
                instanceof org.springframework.security.authentication.AnonymousAuthenticationToken;

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
    // Safely extract exception info to avoid LazyInitializationException when logging
    String exceptionInfo = exception.getClass().getName();
    try {
      String msg = exception.getMessage();
      if (msg != null) {
        exceptionInfo += ": " + msg;
      }
    } catch (Exception ignored) {
      // Message may be unavailable (e.g., lazy-loaded properties)
    }
    logger.error("Unhandled exception: " + exceptionInfo, exception);

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
