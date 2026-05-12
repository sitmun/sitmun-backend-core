package org.sitmun.domain.task.parameter;

/**
 * Classification of a task parameter based on its value source and mutability. Determines whether
 * the backend or client controls the parameter value and whether a default exists.
 *
 * <p>The four types capture two orthogonal facts:
 *
 * <ul>
 *   <li><b>Who controls the value:</b> backend ({@code LOCKED}, {@code PROVIDED}) vs client ({@code
 *       DECLARED_*})
 *   <li><b>Is there a default to fall back on:</b> yes ({@code LOCKED}, {@code PROVIDED}, {@code
 *       DECLARED_WITH_DEFAULT}) vs no ({@code DECLARED_WITHOUT_DEFAULT})
 * </ul>
 */
public enum TaskParameterType {
  /**
   * Parameter value contains {@code #{...}} system variable expressions. Backend always wins; value
   * is resolved at runtime. Takes priority over {@code PROVIDED} flag if both are present
   * (defensive).
   */
  LOCKED,

  /**
   * Parameter has {@code provided: true} flag (and is not {@code LOCKED}). Backend always wins;
   * value may be supplied by configuration or computed.
   */
  PROVIDED,

  /**
   * Parameter has a non-blank plain value without {@code #{...}} or {@code provided} flag. Client
   * may override this default.
   */
  DECLARED_WITH_DEFAULT,

  /**
   * Parameter has null or blank value. Client must supply a value; falls back to empty string if
   * not provided.
   */
  DECLARED_WITHOUT_DEFAULT;

  /**
   * Returns true if this parameter type is controlled exclusively by the backend (not visible to or
   * overridable by the client).
   */
  public boolean isBackendOnly() {
    return this == LOCKED || this == PROVIDED;
  }

  /**
   * Returns true if this parameter type allows client input (may be supplied or overridden by the
   * client).
   */
  public boolean isClientAllowed() {
    return this == DECLARED_WITH_DEFAULT || this == DECLARED_WITHOUT_DEFAULT;
  }
}
