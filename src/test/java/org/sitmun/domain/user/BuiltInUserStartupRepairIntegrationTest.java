package org.sitmun.domain.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.sitmun.infrastructure.security.core.SecurityConstants;
import org.sitmun.infrastructure.security.health.BuiltInUserHealthIndicator;
import org.sitmun.infrastructure.startup.BuiltInUserRepairResult;
import org.sitmun.infrastructure.startup.BuiltInUserStartupStatus;
import org.sitmun.infrastructure.startup.BuiltInUsersStartupProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("BuiltInUserStartupRepair integration probe")
class BuiltInUserStartupRepairIntegrationTest {

  @Autowired private BuiltInUserRepairService repairService;
  @Autowired private BuiltInUserStartupStatus status;
  @Autowired private BuiltInUserHealthIndicator healthIndicator;
  @Autowired private BuiltInUsersStartupProperties properties;
  @Autowired private UserRepository userRepository;

  @AfterEach
  void restoreSingletonState() {
    properties.setAdminPassword(null);
    status.markReady();
  }

  @Test
  @Order(1)
  @Transactional
  @DisplayName("passwordless admin without bootstrap secret keeps process usable and health DOWN")
  void passwordlessAdminWithoutSecretBlocksHealth() {
    properties.setAdminPassword(null);
    User admin =
        userRepository.findByUsername(SecurityConstants.BUILT_IN_ADMIN_PRINCIPAL).orElseThrow();
    admin.setPassword(null);
    userRepository.saveAndFlush(admin);

    BuiltInUserRepairResult result = repairService.repair();
    assertThat(result.success()).isFalse();
    assertThat(result.blockedReason())
        .isEqualTo(BuiltInUserStartupStatus.REASON_ADMIN_MISSING_BOOTSTRAP_PASSWORD);

    status.markBlocked(result.blockedReason());
    var health = healthIndicator.health();
    assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    assertThat(health.getDetails())
        .containsEntry("reason", BuiltInUserStartupStatus.REASON_ADMIN_MISSING_BOOTSTRAP_PASSWORD);
    assertThat(health.getDetails().toString()).doesNotContain("$2a$");
  }

  @Test
  @Order(2)
  @Transactional
  @DisplayName(
      "passwordless admin with bootstrap secret restores encoded password and READY health")
  void passwordlessAdminWithSecretBecomesReady() {
    properties.setAdminPassword("probe-bootstrap-password");
    User admin =
        userRepository.findByUsername(SecurityConstants.BUILT_IN_ADMIN_PRINCIPAL).orElseThrow();
    admin.setPassword(null);
    userRepository.saveAndFlush(admin);

    BuiltInUserRepairResult result = repairService.repair();
    assertThat(result.success()).isTrue();

    User repaired =
        userRepository.findByUsername(SecurityConstants.BUILT_IN_ADMIN_PRINCIPAL).orElseThrow();
    assertThat(repaired.getPassword()).isNotBlank();
    assertThat(repaired.getPassword()).isNotEqualTo("probe-bootstrap-password");
    assertThat(repaired.getPassword()).startsWith("$2a$");

    status.markReady();
    assertThat(healthIndicator.health().getStatus()).isEqualTo(Status.UP);
  }

  @Test
  @Order(3)
  @DisplayName("after bootstrap case, singleton state is clean for subsequent tests")
  void subsequentTestSeesCleanSingletonState() {
    assertThat(properties.normalizedAdminPassword()).isEmpty();
    assertThat(status.getState()).isEqualTo(BuiltInUserStartupStatus.State.READY);
    assertThat(status.getReason()).isNull();
    assertThat(healthIndicator.health().getStatus()).isEqualTo(Status.UP);
  }
}
