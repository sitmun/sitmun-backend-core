package org.sitmun.authorization.client.dto;

import org.sitmun.domain.application.Application;
import org.sitmun.domain.service.Service;
import org.sitmun.domain.task.Task;
import org.sitmun.domain.territory.Territory;

/**
 * Utility class for building consistent proxy URLs across the application. Centralizes the proxy
 * URL construction logic to ensure consistency and maintainability.
 */
public class ProxyUrlBuilder {

  private static final String PROXY_PATH = "/proxy/";
  private static final String SEPARATOR = "/";

  private ProxyUrlBuilder() {
    // Utility class - prevent instantiation
  }

  /**
   * Builds a proxy URL for SQL-based tasks. Pattern:
   * {baseUrl}/proxy/{applicationId}/{territoryId}/SQL/{taskId}
   *
   * @param baseUrl Base proxy URL
   * @param application Application context
   * @param territory Territory context
   * @param task Task to proxy
   * @return Formatted proxy URL for SQL task
   */
  public static String forSqlTask(
      String baseUrl, Application application, Territory territory, Task task) {
    return baseUrl
        + PROXY_PATH
        + application.getId()
        + SEPARATOR
        + territory.getId()
        + SEPARATOR
        + "SQL"
        + SEPARATOR
        + task.getId();
  }

  /**
   * Builds a proxy URL for cartography service-based tasks (WMS/WFS). Pattern:
   * {baseUrl}/proxy/{applicationId}/{territoryId}/{serviceType}/{serviceId}
   *
   * @param baseUrl Base proxy URL
   * @param application Application context
   * @param territory Territory context
   * @param service Service to proxy
   * @return Formatted proxy URL for cartography service
   */
  public static String forCartographyService(
      String baseUrl, Application application, Territory territory, Service service) {
    return baseUrl
        + PROXY_PATH
        + application.getId()
        + SEPARATOR
        + territory.getId()
        + SEPARATOR
        + service.getType()
        + SEPARATOR
        + service.getId();
  }

  /**
   * Builds a proxy URL for Web API tasks. Pattern:
   * {baseUrl}/proxy/{applicationId}/{territoryId}/API/{taskId}
   *
   * @param baseUrl Base proxy URL
   * @param application Application context
   * @param territory Territory context
   * @param task Task to proxy
   * @return Formatted proxy URL for Web API task
   */
  public static String forWebApiTask(
      String baseUrl, Application application, Territory territory, Task task) {
    return baseUrl
        + PROXY_PATH
        + application.getId()
        + SEPARATOR
        + territory.getId()
        + SEPARATOR
        + "API"
        + SEPARATOR
        + task.getId();
  }

  /**
   * Builds a proxy URL with a custom scope and resource ID. Pattern:
   * {baseUrl}/proxy/{applicationId}/{territoryId}/{scope}/{resourceId} Used by tasks with variable
   * scopes (SQL, URL, API, etc.)
   *
   * @param baseUrl Base proxy URL
   * @param application Application context
   * @param territory Territory context
   * @param scope Scope identifier (e.g., "SQL", "API", "URL")
   * @param resourceId Resource identifier
   * @return Formatted proxy URL with custom scope
   */
  public static String forScopedResource(
      String baseUrl,
      Application application,
      Territory territory,
      String scope,
      String resourceId) {
    return baseUrl
        + PROXY_PATH
        + application.getId()
        + SEPARATOR
        + territory.getId()
        + SEPARATOR
        + scope
        + SEPARATOR
        + resourceId;
  }
}
