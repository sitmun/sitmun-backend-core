package org.sitmun.infrastructure.startup;

import org.springframework.stereotype.Component;

/** Thread-safe readiness for built-in user startup repair. */
@Component
public class BuiltInUserStartupStatus {

  public enum State {
    INITIALIZING,
    READY,
    BLOCKED
  }

  public static final String REASON_ADMIN_MISSING_BOOTSTRAP_PASSWORD =
      "admin-missing-bootstrap-password";
  public static final String REASON_REPAIR_FAILED = "repair-failed";

  private volatile State state = State.INITIALIZING;
  private volatile String reason;

  public State getState() {
    return state;
  }

  public String getReason() {
    return reason;
  }

  public void markReady() {
    state = State.READY;
    reason = null;
  }

  public void markInitializing() {
    state = State.INITIALIZING;
    reason = null;
  }

  public void markBlocked(String reason) {
    state = State.BLOCKED;
    this.reason = reason;
  }
}
