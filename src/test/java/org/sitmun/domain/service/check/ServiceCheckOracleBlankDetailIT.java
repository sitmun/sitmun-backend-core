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
import org.sitmun.upstream.signal.ServiceStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.liquibase.autoconfigure.LiquibaseAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

/**
 * Oracle stores {@code ''} as NULL. A blank {@code SEC_DETAIL} must be written as null, or the
 * insert fails with ORA-01400 while the column is NOT NULL.
 *
 * <p>Run: {@code ./gradlew testOracle --tests
 * org.sitmun.domain.service.check.ServiceCheckOracleBlankDetailIT}
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(LiquibaseAutoConfiguration.class)
@Import({I18nTestConfiguration.class, ServiceCheckStore.class})
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=none")
@EnabledIfSystemProperty(named = "spring.profiles.active", matches = ".*oracle.*")
@DisplayName("Oracle service-check blank detail")
class ServiceCheckOracleBlankDetailIT {

  private static final Instant OBSERVED = Instant.parse("2026-10-05T12:34:56Z");

  @Autowired private EntityManager entityManager;
  @Autowired private ServiceRepository serviceRepository;
  @Autowired private ServiceCheckRepository checkRepository;
  @Autowired private ServiceCheckStore store;

  @Test
  @DisplayName("Blank detail is stored as null")
  void blankDetailIsStoredAsNull() {
    Service service =
        serviceRepository.save(
            Service.builder()
                .name("Blank detail WMS")
                .type("WMS")
                .serviceURL("http://example.com/wms")
                .blocked(false)
                .isProxied(false)
                .build());

    store.record(service, ServiceCheckStore.BACKEND, new ServiceStatus("up", 0), 4L, "", OBSERVED);
    store.record(service, ServiceCheckStore.PROXY, new ServiceStatus("up", 0), 5L, null, OBSERVED);
    entityManager.flush();
    entityManager.clear();

    assertThat(stored(service.getId(), ServiceCheckStore.BACKEND)).isNull();
    assertThat(stored(service.getId(), ServiceCheckStore.PROXY)).isNull();
  }

  private String stored(Integer serviceId, String observer) {
    return checkRepository
        .findByService_IdAndObserver(serviceId, observer)
        .orElseThrow()
        .getDetail();
  }
}
