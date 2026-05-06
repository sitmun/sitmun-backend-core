package org.sitmun.domain.service;

/**
 * Policy for determining if a service should be accessible in client profiles and proxy requests.
 *
 * <p>Services marked as blocked should not appear in client configuration profiles and should be
 * denied at the proxy middleware layer.
 */
public final class ServiceBlockPolicy {

  private ServiceBlockPolicy() {
    // Utility class
  }

  /**
   * Check if a service is accessible (not blocked) for use in client profiles.
   *
   * @param service the service to check
   * @return true if the service is accessible (not null and not blocked), false otherwise
   */
  public static boolean isAccessibleInClientProfile(Service service) {
    return service != null && !Boolean.TRUE.equals(service.getBlocked());
  }

  /**
   * Check if a service is accessible or null (no service required). Used for tasks where having no
   * service is valid (e.g., UI control tasks).
   *
   * @param service the service to check (may be null)
   * @return true if the service is null or accessible (not blocked), false otherwise
   */
  public static boolean isAccessibleInClientProfileOrNull(Service service) {
    return service == null || !Boolean.TRUE.equals(service.getBlocked());
  }
}
