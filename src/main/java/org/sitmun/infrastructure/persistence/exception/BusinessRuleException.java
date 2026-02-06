package org.sitmun.infrastructure.persistence.exception;

/**
 * Exception thrown when a business rule is violated.
 *
 * <p>This exception extends {@link RequirementException} and adds a problem type field to allow
 * specific identification of which business rule was violated. This enables proper RFC 9457 Problem
 * Detail responses with specific type URIs.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * throw new BusinessRuleException(
 *     ProblemTypes.TOURISTIC_TREE_CONSTRAINT,
 *     "Touristic tree can only be linked with 0 or 1 tourist application"
 * );
 * }</pre>
 *
 * <p>The problem type will be used by exception handlers to generate appropriate RFC 9457 Problem
 * Detail responses.
 *
 * @see org.sitmun.infrastructure.web.dto.ProblemTypes
 * @see RequirementException
 */
public class BusinessRuleException extends RequirementException {

  /**
   * The RFC 9457 problem type URI that identifies which business rule was violated. Should be one
   * of the constants from {@link org.sitmun.infrastructure.web.dto.ProblemTypes}.
   */
  private final String problemType;

  /**
   * Constructs a new BusinessRuleException with the specified problem type and detail message.
   *
   * @param problemType the RFC 9457 problem type URI (e.g., ProblemTypes.TOURISTIC_TREE_CONSTRAINT)
   * @param message the detail message explaining why the business rule was violated
   */
  public BusinessRuleException(String problemType, String message) {
    super(message);
    this.problemType = problemType;
  }

  /**
   * Returns the RFC 9457 problem type URI for this business rule violation.
   *
   * @return the problem type URI
   */
  public String getProblemType() {
    return problemType;
  }
}
