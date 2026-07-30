package org.sitmun.infrastructure.security.health;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.infrastructure.startup.BuiltInUserStartupStatus;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

@DisplayName("BuiltInUserHealthIndicator")
class BuiltInUserHealthIndicatorTest {

  private BuiltInUserStartupStatus status;
  private BuiltInUserHealthIndicator indicator;

  @BeforeEach
  void setUp() {
    status = new BuiltInUserStartupStatus();
    indicator = new BuiltInUserHealthIndicator(status);
  }

  @Test
  @DisplayName("fresh status is DOWN initializing")
  void initializingIsDown() {
    Health health = indicator.health();
    assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    assertThat(health.getDetails()).containsEntry("builtInUsers", "initializing");
    assertThat(health.getDetails()).doesNotContainKey("reason");
  }

  @Test
  @DisplayName("ready status is UP")
  void readyIsUp() {
    status.markReady();
    Health health = indicator.health();
    assertThat(health.getStatus()).isEqualTo(Status.UP);
    assertThat(health.getDetails()).containsEntry("builtInUsers", "ready");
    assertThat(health.getDetails()).doesNotContainKey("warnings");
  }

  @Test
  @DisplayName("ready with admin-has-positions warning stays UP and exposes stable warning code")
  void readyWithAdminPositionsWarningIsUp() {
    status.markReady(List.of(BuiltInUserStartupStatus.WARNING_ADMIN_HAS_POSITIONS));
    Health health = indicator.health();
    assertThat(health.getStatus()).isEqualTo(Status.UP);
    assertThat(health.getDetails()).containsEntry("builtInUsers", "ready");
    assertThat(health.getDetails())
        .containsEntry("warnings", List.of(BuiltInUserStartupStatus.WARNING_ADMIN_HAS_POSITIONS));
  }

  @Test
  @DisplayName("blocked status is DOWN with stable reason only")
  void blockedIsDownWithReason() {
    status.markBlocked(BuiltInUserStartupStatus.REASON_ADMIN_MISSING_BOOTSTRAP_PASSWORD);
    Health health = indicator.health();
    assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    assertThat(health.getDetails()).containsEntry("builtInUsers", "blocked");
    assertThat(health.getDetails())
        .containsEntry("reason", BuiltInUserStartupStatus.REASON_ADMIN_MISSING_BOOTSTRAP_PASSWORD);
    String details = health.getDetails().toString();
    assertThat(details).doesNotContain("bootstrap-secret");
    assertThat(details).doesNotContain("$2a$");
    assertThat(details).doesNotContain("admin@");
    assertThat(details).doesNotContain("plaintext");
  }
}
