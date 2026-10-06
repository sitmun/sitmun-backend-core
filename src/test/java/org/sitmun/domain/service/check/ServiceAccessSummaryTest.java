package org.sitmun.domain.service.check;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.administration.service.access.ServiceAccessHour;
import org.sitmun.administration.service.access.ServiceAccessSample;
import org.sitmun.administration.service.access.ServiceAccessSamples;
import org.sitmun.administration.service.access.ServiceAccessSummaries;
import org.sitmun.administration.service.access.ServiceAccessSummary;
import org.sitmun.domain.service.Service;
import org.sitmun.domain.service.ServiceRepository;
import org.sitmun.domain.service.usage.ServiceUsageQueries;
import org.sitmun.infrastructure.persistence.type.i18n.I18nTestConfiguration;
import org.sitmun.upstream.signal.ServiceStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(LiquibaseAutoConfiguration.class)
@DisplayName("Service access summaries")
class ServiceAccessSummaryTest {

  private static final Instant NOW = Instant.parse("2026-10-05T15:40:00Z");

  @Autowired private ServiceRepository serviceRepository;
  @Autowired private ServiceCheckStore store;
  @Autowired private ServiceAccessSummaries summaries;

  @Test
  @DisplayName("Keeps the worst status per hour from either observer and lists both")
  void worstStatusPerHourFromEitherObserver() {
    Service service = save("Checked WMS");
    Service unobserved = save("Unobserved WMS");

    store.record(
        service,
        ServiceCheckStore.BACKEND,
        new ServiceStatus("up", 0),
        10L,
        "",
        Instant.parse("2026-10-04T15:30:00Z"));
    store.record(
        service,
        ServiceCheckStore.BACKEND,
        new ServiceStatus("up", 0),
        11L,
        "",
        Instant.parse("2026-10-05T14:10:00Z"));
    store.record(
        service,
        ServiceCheckStore.BACKEND,
        new ServiceStatus("maintenance", 55),
        40L,
        "maintenance",
        Instant.parse("2026-10-05T14:40:00Z"));
    store.record(
        service,
        "proxy",
        new ServiceStatus("server_error", 50),
        20L,
        "proxy only",
        Instant.parse("2026-10-05T13:20:00Z"));
    store.record(
        service,
        "proxy",
        new ServiceStatus("timeout", 70),
        90L,
        "proxy in the backend hour",
        Instant.parse("2026-10-05T14:50:00Z"));
    store.record(
        service,
        "proxy",
        new ServiceStatus("timeout", 70),
        128L,
        "newer proxy row",
        Instant.parse("2026-10-05T15:10:00Z"));

    List<ServiceAccessSummary> listed = summaries.list(NOW);

    assertThat(listed)
        .extracting(ServiceAccessSummary::serviceId)
        .doesNotContain(unobserved.getId());
    ServiceAccessSummary summary =
        listed.stream()
            .filter(row -> row.serviceId().equals(service.getId()))
            .findFirst()
            .orElseThrow();
    assertThat(summary.status()).isEqualTo("timeout");
    assertThat(summary.statusRank()).isEqualTo(70);
    assertThat(summary.observer()).isEqualTo("proxy");
    assertThat(summary.elapsedMs()).isEqualTo(128L);
    assertThat(summary.observedAt()).isEqualTo(Instant.parse("2026-10-05T15:10:00Z"));
    assertThat(summary.detail()).isEqualTo("newer proxy row");
    assertThat(summary.hours()).hasSize(24);
    assertThat(summary.hours().get(0)).isNull();
    assertThat(summary.hours().get(21))
        .isEqualTo(new ServiceAccessHour("server_error", 50, List.of("proxy")));
    assertThat(summary.hours().get(22))
        .isEqualTo(new ServiceAccessHour("timeout", 70, List.of("backend", "proxy")));
    assertThat(summary.hours().get(23))
        .isEqualTo(new ServiceAccessHour("timeout", 70, List.of("proxy")));
  }

  @Test
  @DisplayName("A proxy timeout and a backend up in one hour paint timeout with both observers")
  void proxyTimeoutBeatsBackendUpInTheSameHour() {
    Service service = save("Merged hour WMS");
    store.record(
        service,
        ServiceCheckStore.BACKEND,
        new ServiceStatus("up", 0),
        12L,
        "",
        Instant.parse("2026-10-05T14:05:00Z"));
    store.record(
        service,
        "proxy",
        new ServiceStatus("timeout", 70),
        90L,
        "timed out",
        Instant.parse("2026-10-05T14:20:00Z"));

    ServiceAccessHour hour = summary(service).hours().get(22);

    assertThat(hour).isEqualTo(new ServiceAccessHour("timeout", 70, List.of("backend", "proxy")));
  }

  @Test
  @DisplayName("An equal rank keeps the later sample and still lists both observers")
  void equalRankKeepsTheLaterSample() {
    Service service = save("Tied hour WMS");
    store.record(
        service,
        ServiceCheckStore.BACKEND,
        new ServiceStatus("timeout", 70),
        40L,
        "earlier",
        Instant.parse("2026-10-05T14:05:00Z"));
    store.record(
        service,
        "proxy",
        new ServiceStatus("server_error", 70),
        80L,
        "later",
        Instant.parse("2026-10-05T14:20:00Z"));

    ServiceAccessHour hour = summary(service).hours().get(22);

    assertThat(hour.status()).isEqualTo("server_error");
    assertThat(hour.observers()).containsExactly("backend", "proxy");
  }

  @Test
  @DisplayName(
      "Samples stay inside the retention window, in observed order, with the check timeout")
  void samplesStayInsideRetention() {
    Service service = save("Sampled WMS");
    store.record(
        service,
        "proxy",
        new ServiceStatus("timeout", 70),
        90L,
        "",
        NOW.minus(Duration.ofHours(24)).minusSeconds(1));
    store.record(
        service,
        ServiceCheckStore.BACKEND,
        new ServiceStatus("up", 0),
        11L,
        "",
        NOW.minus(Duration.ofHours(2)));
    store.record(
        service,
        "proxy",
        new ServiceStatus("server_error", 50),
        40L,
        "",
        NOW.minus(Duration.ofMinutes(30)));
    store.record(
        service, ServiceCheckStore.BACKEND, new ServiceStatus("reached", 10), 15L, "", NOW);

    ServiceAccessSamples result =
        summaries.samples(service.getId(), NOW, Duration.ofSeconds(10), Duration.ofHours(24));

    assertThat(result.timeoutMs()).isEqualTo(10_000L);
    assertThat(result.samples())
        .extracting(ServiceAccessSample::observedAt)
        .containsExactly(NOW.minus(Duration.ofHours(2)), NOW.minus(Duration.ofMinutes(30)), NOW);
    assertThat(result.samples())
        .extracting(ServiceAccessSample::observer)
        .containsExactly("backend", "proxy", "backend");
  }

  @Test
  @DisplayName("A service with no checks returns an empty sample list and the timeout")
  void emptySamplesStillExposeTimeout() {
    Service service = save("Quiet WMS");

    ServiceAccessSamples result =
        summaries.samples(service.getId(), NOW, Duration.ofSeconds(10), Duration.ofHours(24));

    assertThat(result.timeoutMs()).isEqualTo(10_000L);
    assertThat(result.samples()).isEmpty();
  }

  @Test
  @DisplayName("Samples for a missing service are not found")
  void missingServiceSamplesAreNotFound() {
    assertThatThrownBy(
            () -> summaries.samples(9_999_999, NOW, Duration.ofSeconds(10), Duration.ofHours(24)))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(error -> ((ResponseStatusException) error).getStatusCode())
        .isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  @DisplayName("Collapses a 10-minute window to the worse status from either observer")
  void tenMinuteWindowKeepsWorstSample() {
    Service service = save("Ten minute WMS");

    store.record(
        service,
        ServiceCheckStore.BACKEND,
        new ServiceStatus("server_error", 50),
        15L,
        "",
        Instant.parse("2026-10-05T13:25:00Z"));
    store.record(
        service,
        "proxy",
        new ServiceStatus("timeout", 90),
        15L,
        "proxy only",
        Instant.parse("2026-10-05T13:05:00Z"));
    store.record(
        service,
        ServiceCheckStore.BACKEND,
        new ServiceStatus("timeout", 70),
        20L,
        "",
        Instant.parse("2026-10-05T14:02:00Z"));
    store.record(
        service,
        ServiceCheckStore.BACKEND,
        new ServiceStatus("up", 0),
        8L,
        "",
        Instant.parse("2026-10-05T14:09:00Z"));
    store.record(
        service,
        "proxy",
        new ServiceStatus("server_error", 80),
        8L,
        "proxy in the same window",
        Instant.parse("2026-10-05T14:05:00Z"));
    store.record(
        service,
        ServiceCheckStore.BACKEND,
        new ServiceStatus("up", 0),
        9L,
        "",
        Instant.parse("2026-10-05T14:11:00Z"));

    List<ServiceAccessHour> buckets = summaries.tenMinuteBuckets(service.getId(), NOW);
    ServiceAccessSummary summary =
        summaries.list(NOW).stream()
            .filter(item -> service.getId().equals(item.serviceId()))
            .findFirst()
            .orElseThrow();

    assertThat(summary.hours()).hasSize(24);
    assertThat(summary.hours().get(21))
        .isEqualTo(new ServiceAccessHour("timeout", 90, List.of("backend", "proxy")));
    assertThat(summary.hours().get(22))
        .isEqualTo(new ServiceAccessHour("server_error", 80, List.of("backend", "proxy")));

    assertThat(buckets).hasSize(144);
    assertThat(buckets.get(127)).isEqualTo(new ServiceAccessHour("timeout", 90, List.of("proxy")));
    assertThat(buckets.get(128)).isNull();
    assertThat(buckets.get(129))
        .isEqualTo(new ServiceAccessHour("server_error", 50, List.of("backend")));
    assertThat(buckets.get(130)).isNull();
    assertThat(buckets.get(131)).isNull();
    assertThat(buckets.get(132)).isNull();
    assertThat(buckets.get(133))
        .isEqualTo(new ServiceAccessHour("server_error", 80, List.of("backend", "proxy")));
    assertThat(buckets.get(134)).isEqualTo(new ServiceAccessHour("up", 0, List.of("backend")));
    assertThat(buckets)
        .filteredOn(hour -> hour != null)
        .containsExactly(
            new ServiceAccessHour("timeout", 90, List.of("proxy")),
            new ServiceAccessHour("server_error", 50, List.of("backend")),
            new ServiceAccessHour("server_error", 80, List.of("backend", "proxy")),
            new ServiceAccessHour("up", 0, List.of("backend")));
  }

  private ServiceAccessSummary summary(Service service) {
    return summaries.list(NOW).stream()
        .filter(item -> service.getId().equals(item.serviceId()))
        .findFirst()
        .orElseThrow();
  }

  private Service save(String name) {
    return serviceRepository.save(
        Service.builder()
            .name(name)
            .type("WMS")
            .serviceURL("http://example.test/wms")
            .blocked(false)
            .isProxied(false)
            .build());
  }

  @TestConfiguration
  @Import({
    I18nTestConfiguration.class,
    ServiceCheckStore.class,
    ServiceAccessSummaries.class,
    ServiceUsageQueries.class
  })
  static class Configuration {}
}
