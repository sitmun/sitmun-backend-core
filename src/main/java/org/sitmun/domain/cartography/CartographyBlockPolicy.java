package org.sitmun.domain.cartography;

/**
 * Policy for determining if a cartography should be accessible in client profiles.
 *
 * <p>Cartographies may be blocked at two levels:
 *
 * <ul>
 *   <li>Direct blocking: the cartography itself has {@code blocked = true}
 *   <li>Cascade blocking: the cartography's service is blocked or missing
 * </ul>
 *
 * <p>Both conditions make the cartography inaccessible to the viewer.
 */
public final class CartographyBlockPolicy {

  private CartographyBlockPolicy() {
    // Utility class
  }

  /**
   * Check if a cartography is directly accessible (not null and not blocked at the cartography
   * level).
   *
   * @param cartography the cartography to check
   * @return true if the cartography exists and is not marked as blocked
   */
  public static boolean isNotDirectlyBlocked(Cartography cartography) {
    return cartography != null && !Boolean.TRUE.equals(cartography.getBlocked());
  }
}
