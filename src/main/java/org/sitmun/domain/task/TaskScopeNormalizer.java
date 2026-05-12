package org.sitmun.domain.task;

import static org.sitmun.domain.DomainConstants.Tasks.*;

import java.util.Map;
import org.springframework.util.StringUtils;

/**
 * Normalizes admin task scope values to viewer-compatible canonical scopes. Maps kebab-case admin
 * storage scopes (e.g., sql-query) to uppercase viewer scopes (e.g., SQL) for TaskDto.scope.
 */
public class TaskScopeNormalizer {

  private TaskScopeNormalizer() {
    // Utility class
  }

  /**
   * Normalizes execution scope from task properties to viewer-compatible scope.
   *
   * <p>Mapping:
   *
   * <ul>
   *   <li>sql-query → SQL
   *   <li>web-api-query → API
   *   <li>web-api-query-no-proxy → RESOURCE (if mimeType present) or URL (if no mimeType)
   *   <li>external-link → URL
   *   <li>cartography-query → unchanged (existing cartography reference)
   * </ul>
   *
   * @param properties Task properties map
   * @return Normalized scope for viewer or null if no scope found
   */
  public static String normalizeExecutionScope(Map<String, Object> properties) {
    if (properties == null) {
      return null;
    }
    Object scopeObj = properties.get(PROPERTY_SCOPE);
    if (scopeObj == null) {
      return null;
    }
    String scope = scopeObj.toString();

    if (SCOPE_SQL_QUERY.equalsIgnoreCase(scope)) {
      return SCOPE_SQL;
    }
    if (SCOPE_WEB_API_QUERY.equalsIgnoreCase(scope)) {
      return SCOPE_API;
    }
    if (SCOPE_WEB_API_QUERY_NO_PROXY.equalsIgnoreCase(scope)) {
      // No-proxy with mimeType → RESOURCE (mimeType-driven rendering, direct fetch)
      // No-proxy without mimeType → URL (external redirect)
      Object mimeTypeObj = properties.get(PROPERTY_MIME_TYPE);
      boolean hasMimeType = mimeTypeObj != null && StringUtils.hasText(mimeTypeObj.toString());
      return hasMimeType ? SCOPE_RESOURCE : SCOPE_URL;
    }
    if (SCOPE_URL_QUERY.equalsIgnoreCase(scope)) {
      return SCOPE_URL;
    }
    // Return as-is for cartography-query and other unknown scopes
    return scope;
  }
}
