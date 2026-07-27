package org.sitmun.domain.application;

import org.apache.commons.lang3.StringUtils;
import org.sitmun.domain.user.User;
import org.sitmun.infrastructure.security.core.SecurityConstants;

/** Pure utility: PoC eligibility and publishable-email rules. */
public final class ApplicationPointOfContactPolicy {

  private ApplicationPointOfContactPolicy() {}

  /**
   * A creator is eligible as PoC when they are not a built-in principal and not blocked. Email is
   * not required for eligibility.
   */
  public static boolean isEligible(User user) {
    if (user == null) {
      return false;
    }
    if (SecurityConstants.isBuiltInPrincipal(user.getUsername())) {
      return false;
    }
    return !Boolean.TRUE.equals(user.getBlocked());
  }

  /** True when the user is eligible and has a non-blank email address. */
  public static boolean hasPublishableEmail(User user) {
    return isEligible(user) && !StringUtils.isBlank(user.getEmail());
  }
}
