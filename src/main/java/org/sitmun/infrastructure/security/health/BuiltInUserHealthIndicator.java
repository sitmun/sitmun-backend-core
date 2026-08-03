package org.sitmun.infrastructure.security.health;

import org.sitmun.infrastructure.startup.BuiltInUserStartupStatus;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Reports DOWN until built-in user startup repair finishes successfully. Exposes only stable,
 * non-sensitive reason codes when blocked.
 */
@Component
public class BuiltInUserHealthIndicator implements HealthIndicator {

  private final BuiltInUserStartupStatus status;

  public BuiltInUserHealthIndicator(BuiltInUserStartupStatus status) {
    this.status = status;
  }

  @Override
  public Health health() {
    return switch (status.getState()) {
      case READY -> {
        Health.Builder builder = Health.up().withDetail("builtInUsers", "ready");
        if (!status.getWarnings().isEmpty()) {
          builder.withDetail("warnings", status.getWarnings());
        }
        yield builder.build();
      }
      case BLOCKED -> {
        Health.Builder builder = Health.down().withDetail("builtInUsers", "blocked");
        if (status.getReason() != null) {
          builder.withDetail("reason", status.getReason());
        }
        if (!status.getWarnings().isEmpty()) {
          builder.withDetail("warnings", status.getWarnings());
        }
        yield builder.build();
      }
      case INITIALIZING -> Health.down().withDetail("builtInUsers", "initializing").build();
    };
  }
}
