package org.sitmun.infrastructure.startup;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

/**
 * Marks startup ready when CodeListValueSetup is disabled. When check-on-startup is true,
 * only CodeListValueSetup sets ready (after it finishes); this bean is not created.
 */
@Component
@ConditionalOnProperty(
    prefix = "sitmun.startup.code-lists",
    name = "check-on-startup",
    havingValue = "false")
public class StartupReadyMarker implements ApplicationRunner, Ordered {

  private final StartupReady startupReady;

  public StartupReadyMarker(StartupReady startupReady) {
    this.startupReady = startupReady;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (!startupReady.isReady()) {
      startupReady.setReady(true);
    }
  }

  @Override
  public int getOrder() {
    return Ordered.LOWEST_PRECEDENCE;
  }
}
