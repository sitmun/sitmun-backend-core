package org.sitmun.domain.service.check;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.sitmun.domain.service.Service;
import org.sitmun.domain.service.ServiceRepository;
import org.sitmun.infrastructure.persistence.type.i18n.I18nTestConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.liquibase.autoconfigure.LiquibaseAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

/**
 * Reloads {@code SEC_OBSERVED} and {@code SCS_OBSERVED} on Oracle.
 *
 * <p>ojdbc throws ORA-18716 when {@code OracleDialect} reads an {@link Instant} from plain {@code
 * TIMESTAMP(6)}. {@code 32_service_check_observed_timestamptz.yaml} widens both columns to {@code
 * TIMESTAMP(6) WITH TIME ZONE}.
 *
 * <p>Run: {@code ./gradlew testOracle --tests
 * org.sitmun.domain.service.check.ServiceCheckOracleObservedInstantIT}
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(LiquibaseAutoConfiguration.class)
@Import(I18nTestConfiguration.class)
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=none")
@EnabledIfSystemProperty(named = "spring.profiles.active", matches = ".*oracle.*")
@DisplayName("Oracle service-check observed instant round-trip")
class ServiceCheckOracleObservedInstantIT {

  private static final Instant CHECK_OBSERVED = Instant.parse("2026-10-05T12:34:56.123456Z");
  private static final Instant SAMPLE_OBSERVED = Instant.parse("2026-10-05T12:34:56.654321Z");

  @Autowired private EntityManager entityManager;
  @Autowired private ServiceRepository serviceRepository;
  @Autowired private ServiceCheckRepository checkRepository;
  @Autowired private ServiceCheckSampleRepository sampleRepository;

  @Test
  @DisplayName("Reload reads both observed instants through OracleDialect")
  void reloadsObservedInstants() {
    Service service =
        serviceRepository.save(
            Service.builder()
                .name("Observed instant WMS")
                .type("WMS")
                .serviceURL("http://example.com/wms")
                .blocked(false)
                .isProxied(false)
                .build());

    ServiceCheck check =
        checkRepository.save(
            ServiceCheck.builder()
                .service(service)
                .observer(ServiceCheckStore.BACKEND)
                .status("up")
                .statusRank(0)
                .elapsedMs(12L)
                .detail("ok")
                .observedAt(CHECK_OBSERVED)
                .build());
    ServiceCheckSample sample =
        sampleRepository.save(
            ServiceCheckSample.builder()
                .service(service)
                .observer(ServiceCheckStore.BACKEND)
                .status("up")
                .statusRank(0)
                .elapsedMs(12L)
                .observedAt(SAMPLE_OBSERVED)
                .build());

    entityManager.flush();
    entityManager.clear();

    assertThat(checkRepository.findById(check.getId()).orElseThrow().getObservedAt())
        .isEqualTo(CHECK_OBSERVED);
    assertThat(sampleRepository.findById(sample.getId()).orElseThrow().getObservedAt())
        .isEqualTo(SAMPLE_OBSERVED);
  }
}
