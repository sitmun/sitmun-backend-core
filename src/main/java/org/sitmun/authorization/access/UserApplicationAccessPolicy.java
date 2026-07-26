package org.sitmun.authorization.access;

import lombok.extern.slf4j.Slf4j;
import org.sitmun.domain.application.ApplicationRepository;
import org.sitmun.domain.user.UserRepository;
import org.sitmun.infrastructure.security.core.SecurityConstants;
import org.springframework.stereotype.Component;

/**
 * Shared authorization gates for client configuration and proxy access.
 *
 * <p>Account-level check ({@link #mayUseClientConfigEndpoints}): passes when the user is not
 * blocked. App-level check ({@link #mayAccessApplication}): passes when the public principal is not
 * accessing a private application. Callers must run both checks in sequence for app-scoped
 * endpoints.
 */
@Slf4j
@Component
public class UserApplicationAccessPolicy {

  private final UserRepository userRepository;
  private final ApplicationRepository applicationRepository;

  public UserApplicationAccessPolicy(
      UserRepository userRepository, ApplicationRepository applicationRepository) {
    this.userRepository = userRepository;
    this.applicationRepository = applicationRepository;
  }

  /** {@code true} when the persisted user exists and {@code User.blocked} is {@code true}. */
  public boolean isBlockedAccount(String username) {
    var user = userRepository.findByUsername(username);
    var blocked =
        user.filter(persistedUser -> Boolean.TRUE.equals(persistedUser.getBlocked())).isPresent();
    if (blocked) {
      log.warn("Access denied: user account is blocked - user={}", username);
    }
    return blocked;
  }

  /**
   * {@code true} when {@code username} is the public principal and the application is private
   * ({@code appPrivate}).
   */
  public boolean isPrivateAppDeniedForPublic(String username, Integer appId) {
    if (!SecurityConstants.isPublicPrincipal(username) || appId == null) {
      return false;
    }
    var denied =
        applicationRepository
            .findById(appId)
            .filter(app -> Boolean.TRUE.equals(app.getAppPrivate()))
            .isPresent();
    if (denied) {
      log.warn("Access denied: application is private - appId={}, user={}", appId, username);
    }
    return denied;
  }

  /** Account-level gate for list/dashboard client config endpoints (blocked users only). */
  public boolean mayUseClientConfigEndpoints(String username) {
    return !isBlockedAccount(username);
  }

  /**
   * App-level gate: passes when the public principal is not accessing a private application.
   * Assumes the account-level gate ({@link #mayUseClientConfigEndpoints}) has already passed. Does
   * not evaluate roles or territories.
   */
  public boolean mayAccessApplication(Integer appId, String username) {
    return !isPrivateAppDeniedForPublic(username, appId);
  }
}
