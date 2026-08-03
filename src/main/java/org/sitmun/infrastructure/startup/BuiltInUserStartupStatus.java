package org.sitmun.infrastructure.startup;

import java.util.List;
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

  /** Stable warning: built-in admin still has UserPosition rows (not auto-deleted). */
  public static final String WARNING_ADMIN_HAS_POSITIONS = "admin-has-positions";

  private volatile State state = State.INITIALIZING;
  private volatile String reason;
  private volatile List<String> warnings = List.of();

  public State getState() {
    return state;
  }

  public String getReason() {
    return reason;
  }

  public List<String> getWarnings() {
    return warnings;
  }

  public void markReady() {
    markReady(List.of());
  }

  public void markReady(List<String> warnings) {
    state = State.READY;
    reason = null;
    this.warnings = warnings == null ? List.of() : List.copyOf(warnings);
  }

  public void markInitializing() {
    state = State.INITIALIZING;
    reason = null;
    warnings = List.of();
  }

  public void markBlocked(String reason) {
    markBlocked(reason, List.of());
  }

  public void markBlocked(String reason, List<String> warnings) {
    state = State.BLOCKED;
    this.reason = reason;
    this.warnings = warnings == null ? List.of() : List.copyOf(warnings);
  }
}
