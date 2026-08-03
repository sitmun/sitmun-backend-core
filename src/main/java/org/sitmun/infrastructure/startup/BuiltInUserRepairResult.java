package org.sitmun.infrastructure.startup;

import java.util.List;

/** Outcome of one built-in user repair attempt. */
public record BuiltInUserRepairResult(
    boolean success, String blockedReason, List<String> warnings) {

  public BuiltInUserRepairResult {
    warnings = warnings == null ? List.of() : List.copyOf(warnings);
  }

  public static BuiltInUserRepairResult succeeded() {
    return succeeded(List.of());
  }

  public static BuiltInUserRepairResult succeeded(List<String> warnings) {
    return new BuiltInUserRepairResult(true, null, warnings);
  }

  public static BuiltInUserRepairResult blocked(String reason) {
    return blocked(reason, List.of());
  }

  public static BuiltInUserRepairResult blocked(String reason, List<String> warnings) {
    return new BuiltInUserRepairResult(false, reason, warnings);
  }
}
