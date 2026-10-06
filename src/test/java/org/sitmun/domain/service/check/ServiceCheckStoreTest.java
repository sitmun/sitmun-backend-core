package org.sitmun.domain.service.check;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.domain.service.Service;
import org.sitmun.domain.service.ServiceRepository;
import org.sitmun.infrastructure.persistence.type.i18n.I18nTestConfiguration;
import org.sitmun.upstream.signal.ServiceStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(LiquibaseAutoConfiguration.class)
@DisplayName("Service check store")
class ServiceCheckStoreTest {

  @Autowired private ServiceRepository serviceRepository;
  @Autowired private ServiceCheckStore store;
  @Autowired private ServiceCheckRepository checkRepository;
  @Autowired private ServiceCheckSampleRepository sampleRepository;

  @Test
  @DisplayName("Upsert replaces the latest row, appends a sample, and leaves SER_BLOCKED unchanged")
  void upsertReplacesLatestAndLeavesBlockedUnchanged() {
    Service service =
        serviceRepository.save(
            Service.builder()
                .name("Blocked WMS")
                .type("WMS")
                .serviceURL("http://example.test/wms")
                .blocked(true)
                .isProxied(false)
                .build());

    Instant firstSeen = Instant.parse("2026-10-05T12:00:00Z");
    ServiceCheck first =
        store.record(
            service,
            ServiceCheckStore.BACKEND,
            new ServiceStatus("maintenance", 55),
            40L,
            "GetCapabilities example.test/wms | maintenance",
            firstSeen);

    Instant secondSeen = Instant.parse("2026-10-05T12:05:00Z");
    ServiceCheck second =
        store.record(
            service, ServiceCheckStore.BACKEND, new ServiceStatus("up", 0), 12L, "", secondSeen);

    assertThat(second.getId()).isEqualTo(first.getId());
    ServiceCheck latest =
        checkRepository
            .findByService_IdAndObserver(service.getId(), ServiceCheckStore.BACKEND)
            .orElseThrow();
    assertThat(latest.getId()).isEqualTo(first.getId());
    assertThat(second.getStatus()).isEqualTo("up");
    assertThat(second.getStatusRank()).isEqualTo(0);
    assertThat(second.getObserver()).isEqualTo("backend");
    assertThat(second.getElapsedMs()).isEqualTo(12L);
    assertThat(second.getDetail()).isEmpty();
    assertThat(second.getObservedAt()).isEqualTo(secondSeen);

    List<ServiceCheckSample> samples =
        sampleRepository.findByService_IdAndObserverOrderByIdAsc(
            service.getId(), ServiceCheckStore.BACKEND);
    assertThat(samples).hasSize(2);
    assertThat(samples.get(0).getStatus()).isEqualTo("maintenance");
    assertThat(samples.get(0).getStatusRank()).isEqualTo(55);
    assertThat(samples.get(0).getElapsedMs()).isEqualTo(40L);
    assertThat(samples.get(0).getObservedAt()).isEqualTo(firstSeen);
    assertThat(samples.get(1).getStatus()).isEqualTo("up");
    assertThat(samples.get(1).getStatusRank()).isZero();

    Service reloaded = serviceRepository.findById(service.getId()).orElseThrow();
    assertThat(reloaded.getBlocked()).isTrue();
  }

  @TestConfiguration
  @Import({I18nTestConfiguration.class, ServiceCheckStore.class})
  static class Configuration {}
}
