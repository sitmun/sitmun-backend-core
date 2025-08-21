package org.sitmun.authorization.client;

/**
 * Constants used for authorization-related operations in SITMUN. This class provides a collection
 * of constant values used throughout the authorization system.
 */
public class AuthorizationConstants {
  private AuthorizationConstants() {
    // Prevent instantiation
  }

  /**
   * Constants related to Task DTO operations. Contains parameter names and values used in
   * task-related operations.
   */
  public static class TaskDto {
    private TaskDto() {
      // Prevent instantiation
    }

    /** Parameter name for service identifier */
    public static final String PARAMETER_SERVICE = "service";

    /** Parameter name for WFS type name */
    public static final String PARAMETER_WFS_TYPENAME = "typename";

    /** Parameter name for layers */
    public static final String PARAMETER_LAYERS = "layers";

    /** Parameter name for type */
    public static final String PARAMETER_TYPE = "type";

    /** Parameter name for required flag */
    public static final String PARAMETER_REQUIRED = "required";

    /** Parameter name for value */
    public static final String PARAMETER_VALUE = "value";

    /** Simple task type identifier */
    public static final String SIMPLE = "simple";

    /** Query task type identifier */
    public static final String QUERY = "query";

    /** Edition task type identifier */
    public static final String EDITION = "edition";
  }
}
