package org.sitmun.infrastructure.web.dto;

/**
 * Registry of RFC 9457 Problem Detail type URIs used across the SITMUN application.
 *
 * <p>Each constant represents a specific problem type that can occur in the API. The type URI
 * serves as a stable identifier that clients can use for:
 * <ul>
 * <li>Determining the type of error that occurred</li>
 * <li>Looking up localized error messages</li>
 * <li>Implementing specific error handling logic</li>
 * </ul>
 *
 * <p>These URIs follow the pattern: https://sitmun.org/problems/{problem-type-slug}
 *
 * <p>Clients (frontend applications) should extract the last segment of the URI and use it as an
 * i18n translation key. For example:
 * <ul>
 * <li> https://sitmun.org/problems/unauthorized → "error.unauthorized"</li>
 * <li> https://sitmun.org/problems/validation-error → "error.validation-error"</li>
 * </ul>
 */
public final class ProblemTypes {

  /** Base URI for all SITMUN problem types. */
  private static final String BASE_URI = "https://sitmun.org/problems/";

  // ============================================================================
  // AUTHENTICATION & AUTHORIZATION
  // ============================================================================

  /** Authentication is required but was not provided. HTTP 401. */
  public static final String UNAUTHORIZED = BASE_URI + "unauthorized";

  /** The authenticated user does not have permission for this resource. HTTP 403. */
  public static final String FORBIDDEN = BASE_URI + "forbidden";

  // ============================================================================
  // VALIDATION & INPUT ERRORS
  // ============================================================================

  /** Request validation failed (Bean Validation, method argument validation). HTTP 400. */
  public static final String VALIDATION_ERROR = BASE_URI + "validation-error";

  /** Request body could not be deserialized or parsed. HTTP 422. */
  public static final String DESERIALIZATION_ERROR = BASE_URI + "deserialization-error";

  /** Malformed or invalid request data. HTTP 400. */
  public static final String BAD_REQUEST = BASE_URI + "bad-request";

  /** Request parameter binding failed. HTTP 400. */
  public static final String BIND_ERROR = BASE_URI + "bind-error";

  /** Method argument validation failed (@Valid annotation). HTTP 400. */
  public static final String METHOD_ARGUMENT_NOT_VALID = BASE_URI + "method-argument-not-valid";

  /** Request parameter type mismatch. HTTP 400. */
  public static final String TYPE_MISMATCH = BASE_URI + "type-mismatch";

  /** Required request parameter is missing. HTTP 400. */
  public static final String MISSING_REQUEST_PARAMETER = BASE_URI + "missing-request-parameter";

  /** Required path variable is missing. HTTP 500. */
  public static final String MISSING_PATH_VARIABLE = BASE_URI + "missing-path-variable";

  // ============================================================================
  // BUSINESS RULE VIOLATIONS
  // ============================================================================

  /**
   * Touristic tree can only be linked with 0 or 1 tourist application. HTTP 400.
   *
   * @see org.sitmun.domain.tree.TreeEventHandler#validateTouristicTree
   */
  public static final String TOURISTIC_TREE_CONSTRAINT = BASE_URI + "touristic-tree-constraint";

  /**
   * Non-touristic tree constraints violated. HTTP 400.
   *
   * @see org.sitmun.domain.tree.TreeEventHandler#validateNoTouristicTree
   */
  public static final String NON_TOURISTIC_TREE_CONSTRAINT =
      BASE_URI + "non-touristic-tree-constraint";

  /**
   * Tree type change validation failed. HTTP 422.
   *
   * <p>Used when validating a tree type change against candidate applications before
   * committing the change. This validation is necessary because Spring Data REST requires
   * two separate PUT operations (one for tree properties, one for applications), and we
   * need to validate the final state before any changes are made.
   *
   * @see org.sitmun.domain.tree.TreeController#validateTreeTypeChange
   */
  public static final String TREE_TYPE_CHANGE_CONSTRAINT =
      BASE_URI + "tree-type-change-constraint";

  /**
   * Tree node style not found in the cartography's styles. HTTP 400.
   *
   * @see org.sitmun.domain.tree.node.TreeNodeEventHandler
   */
  public static final String TREE_NODE_STYLE_NOT_FOUND = BASE_URI + "tree-node-style-not-found";

  /**
   * Tree node style requires a cartography to be set. HTTP 400.
   *
   * @see org.sitmun.domain.tree.node.TreeNodeEventHandler
   */
  public static final String TREE_NODE_STYLE_REQUIRES_CARTOGRAPHY =
      BASE_URI + "tree-node-style-requires-cartography";

  // ============================================================================
  // JPA/HIBERNATE PERSISTENCE ERRORS
  // ============================================================================

  /** Requested entity was not found in the database. HTTP 404. */
  public static final String ENTITY_NOT_FOUND = BASE_URI + "entity-not-found";

  /**
   * Optimistic locking failure - entity was modified by another user. HTTP 409 Conflict. Client
   * should refresh and retry.
   */
  public static final String OPTIMISTIC_LOCK_FAILURE = BASE_URI + "optimistic-lock-failure";

  /** Pessimistic locking failure - resource is locked by another transaction. HTTP 409 Conflict. */
  public static final String PESSIMISTIC_LOCK_FAILURE = BASE_URI + "pessimistic-lock-failure";

  /**
   * Data integrity constraint violation (foreign key, unique constraint). HTTP 422 Unprocessable
   * Entity.
   */
  public static final String DATA_INTEGRITY_VIOLATION = BASE_URI + "data-integrity-violation";

  /** Database constraint violation. HTTP 422 Unprocessable Entity. */
  public static final String CONSTRAINT_VIOLATION = BASE_URI + "constraint-violation";

  /** Query returned multiple results when only one was expected. HTTP 500. */
  public static final String NON_UNIQUE_RESULT = BASE_URI + "non-unique-result";

  /** Operation requires an active transaction but none is present. HTTP 500. */
  public static final String TRANSACTION_REQUIRED = BASE_URI + "transaction-required";

  // ============================================================================
  // DOMAIN-SPECIFIC ERRORS
  // ============================================================================

  /** Database connection error. HTTP 500. */
  public static final String DATABASE_CONNECTION_ERROR = BASE_URI + "database-connection-error";

  /** Database operation failed. HTTP 500. */
  public static final String DATABASE_ERROR = BASE_URI + "database-error";

  /**
   * Mail service is not available (mail profile not active). HTTP 503 Service Unavailable.
   *
   * @see org.sitmun.infrastructure.security.password.exception.MailNotImplementedException
   */
  public static final String MAIL_NOT_AVAILABLE = BASE_URI + "mail-not-available";

  /** Error rendering email template. HTTP 500. */
  public static final String EMAIL_TEMPLATE_ERROR = BASE_URI + "email-template-error";

  /** Invalid image format or size. HTTP 400. */
  public static final String ILLEGAL_IMAGE = BASE_URI + "illegal-image";

  /** Cannot modify system code list values. HTTP 400. */
  public static final String IMMUTABLE_CODELIST_VALUE = BASE_URI + "immutable-codelist-value";

  // ============================================================================
  // HTTP/SPRING FRAMEWORK ERRORS
  // ============================================================================

  /** Requested resource not found. HTTP 404. */
  public static final String NOT_FOUND = BASE_URI + "not-found";

  /** HTTP method not supported for this endpoint. HTTP 405 Method Not Allowed. */
  public static final String METHOD_NOT_SUPPORTED = BASE_URI + "method-not-supported";

  /** Content-Type header not supported. HTTP 415 Unsupported Media Type. */
  public static final String MEDIA_TYPE_NOT_SUPPORTED = BASE_URI + "media-type-not-supported";

  /** Requested media type cannot be produced (Accept header). HTTP 406 Not Acceptable. */
  public static final String MEDIA_TYPE_NOT_ACCEPTABLE = BASE_URI + "media-type-not-acceptable";

  /** Response could not be written. HTTP 500. */
  public static final String MESSAGE_NOT_WRITABLE = BASE_URI + "message-not-writable";

  /** No handler found for the request. HTTP 404. */
  public static final String NO_HANDLER_FOUND = BASE_URI + "no-handler-found";

  /** Asynchronous request timed out. HTTP 503 Service Unavailable. */
  public static final String ASYNC_REQUEST_TIMEOUT = BASE_URI + "async-request-timeout";

  // ============================================================================
  // GENERIC/FALLBACK
  // ============================================================================

  /** Internal server error. HTTP 500. */
  public static final String INTERNAL_SERVER_ERROR = BASE_URI + "internal-server-error";

  private ProblemTypes() {
    // Prevent instantiation
  }
}

