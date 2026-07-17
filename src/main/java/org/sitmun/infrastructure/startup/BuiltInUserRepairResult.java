package org.sitmun.infrastructure.startup;

/** Outcome of one built-in user repair attempt. */
public record BuiltInUserRepairResult(boolean success, String blockedReason) {

  public static BuiltInUserRepairResult succeeded() {
    return new BuiltInUserRepairResult(true, null);
  }

  public static BuiltInUserRepairResult blocked(String reason) {
    return new BuiltInUserRepairResult(false, reason);
  }
}
