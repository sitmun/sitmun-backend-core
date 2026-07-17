package org.sitmun.domain.user;

import lombok.extern.slf4j.Slf4j;
import org.sitmun.infrastructure.startup.BuiltInUserRepairResult;
import org.sitmun.infrastructure.startup.BuiltInUserStartupStatus;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Soft-repairs built-in admin/public users at startup. Never aborts the process; reports unrepaired
 * conditions through {@link BuiltInUserStartupStatus}.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
@Slf4j
public class BuiltInUserStartupRepairer implements ApplicationRunner {

  private final BuiltInUserRepairService repairService;
  private final BuiltInUserStartupStatus status;

  public BuiltInUserStartupRepairer(
      BuiltInUserRepairService repairService, BuiltInUserStartupStatus status) {
    this.repairService = repairService;
    this.status = status;
  }

  @Override
  public void run(ApplicationArguments args) {
    try {
      BuiltInUserRepairResult result = repairService.repair();
      if (result.success()) {
        status.markReady();
        log.info("Built-in user startup repair completed");
      } else {
        status.markBlocked(result.blockedReason());
        log.error("Built-in user startup repair blocked: {}", result.blockedReason());
      }
    } catch (RuntimeException ex) {
      status.markBlocked(BuiltInUserStartupStatus.REASON_REPAIR_FAILED);
      log.error("Built-in user startup repair failed", ex);
    }
  }
}
