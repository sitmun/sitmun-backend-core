package org.sitmun.infrastructure.security.health;

import org.sitmun.infrastructure.startup.StartupReady;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Reports DOWN until startup tasks (e.g. CodeListValueSetup) have finished. Ensures healthcheck
 * only passes when the backend is ready to serve traffic.
 */
@Component
public class StartupReadyHealthIndicator implements HealthIndicator {

  private final StartupReady startupReady;

  public StartupReadyHealthIndicator(StartupReady startupReady) {
    this.startupReady = startupReady;
  }

  @Override
  public Health health() {
    if (startupReady.isReady()) {
      return Health.up().withDetail("startup", "ready").build();
    }
    return Health.down().withDetail("startup", "initializing").build();
  }
}
