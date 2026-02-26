package org.sitmun.infrastructure.util;

import java.util.Map;
import org.sitmun.domain.DomainConstants;

/**
 * Utility methods for working with task parameters. Provides backward-compatible access to
 * parameter properties during the migration from "name" to "variable" naming convention.
 */
public class TaskParameterUtil {

  private TaskParameterUtil() {
    // Utility class
  }

  /**
   * Gets the variable name from a parameter map, supporting both old and new naming.
   *
   * <p>Migration path: During transition from "name" to "variable":
   *
   * <ul>
   *   <li>Query/Edit/MoreInfo tasks: Migrate from "name" to "variable"
   *   <li>Basic tasks: Continue using "name"
   * </ul>
   *
   * <p>This method provides backward compatibility by checking both keys:
   *
   * <ol>
   *   <li>Tries "variable" first (new standard for query/edit/more-info)
   *   <li>Falls back to "name" (old standard, still used by basic tasks)
   * </ol>
   *
   * @param param Parameter map from task properties JSON
   * @return The variable/name value, or null if neither key exists
   */
  public static String getParameterVariable(Map<String, Object> param) {
    if (param == null) {
      return null;
    }

    // Try new key first (priority)
    Object variable = param.get(DomainConstants.Tasks.PARAMETERS_VARIABLE);
    if (variable != null) {
      return String.valueOf(variable);
    }

    // Fall back to old key (backward compatibility)
    Object name = param.get(DomainConstants.Tasks.PARAMETERS_NAME);
    if (name != null) {
      return String.valueOf(name);
    }

    return null;
  }

  /**
   * Checks if a parameter has a variable/name defined.
   *
   * @param param Parameter map
   * @return true if either "variable" or "name" key exists
   */
  public static boolean hasParameterVariable(Map<String, Object> param) {
    return param != null
        && (param.containsKey(DomainConstants.Tasks.PARAMETERS_VARIABLE)
            || param.containsKey(DomainConstants.Tasks.PARAMETERS_NAME));
  }
}
