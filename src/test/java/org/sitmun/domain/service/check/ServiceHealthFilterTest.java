package org.sitmun.domain.service.check;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.domain.service.Service;
import org.sitmun.domain.service.ServiceHealthQuery;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(LiquibaseAutoConfiguration.class)
@DisplayName("Service health filter")
class ServiceHealthFilterTest {

  private String token;

  @BeforeEach
  void token() {
    token = "HealthFilter-" + java.util.UUID.randomUUID();
  }

  @Autowired private ServiceRepository serviceRepository;
  @Autowired private ServiceCheckStore store;
  @Autowired private ServiceHealthQuery health;

  @Test
  @DisplayName("failing, healthy, and unchecked partition the matching services")
  void filtersPartitionMatchingServices() {
    Service failing = save("timeout");
    Service healthy = save("up");
    Service reached = save("reached");
    Service unchecked = save("quiet");
    record(failing, "proxy", "timeout", 70, "2026-10-05T12:00:00Z");
    record(failing, ServiceCheckStore.BACKEND, "up", 0, "2026-10-05T11:00:00Z");
    record(healthy, ServiceCheckStore.BACKEND, "up", 0, "2026-10-05T12:00:00Z");
    record(reached, ServiceCheckStore.BACKEND, "reached", 10, "2026-10-05T12:00:00Z");

    assertThat(ids("failing"))
        .contains(failing.getId())
        .doesNotContain(healthy.getId(), reached.getId(), unchecked.getId());
    assertThat(ids("healthy"))
        .contains(healthy.getId(), reached.getId())
        .doesNotContain(failing.getId(), unchecked.getId());
    assertThat(ids("unchecked"))
        .contains(unchecked.getId())
        .doesNotContain(failing.getId(), healthy.getId());

    long matched = health.page(null, token, Pageable.unpaged()).getTotalElements();
    long parts =
        health.page("failing", token, Pageable.unpaged()).getTotalElements()
            + health.page("healthy", token, Pageable.unpaged()).getTotalElements()
            + health.page("unchecked", token, Pageable.unpaged()).getTotalElements();
    assertThat(parts).isEqualTo(matched);
  }

  @Test
  @DisplayName("accessRank desc puts the worst latest rank first")
  void sortsByLatestRank() {
    Service low = save("low");
    Service high = save("high");
    Service none = save("none");
    record(low, ServiceCheckStore.BACKEND, "up", 0, "2026-10-05T12:00:00Z");
    record(high, ServiceCheckStore.BACKEND, "up", 0, "2026-10-05T10:00:00Z");
    record(high, "proxy", "timeout", 70, "2026-10-05T12:00:00Z");

    List<Integer> ids =
        health
            .page(null, token, PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "accessRank")))
            .map(Service::getId)
            .getContent();

    assertThat(ids).containsSubsequence(high.getId(), low.getId(), none.getId());
  }

  @Test
  @DisplayName("health and text search both apply, and pages do not repeat ids")
  void searchAndPaging() {
    Service first = save("alpha down");
    Service second = save("beta down");
    Service other = save("gamma up");
    record(first, ServiceCheckStore.BACKEND, "server_error", 50, "2026-10-05T12:00:00Z");
    record(second, ServiceCheckStore.BACKEND, "timeout", 70, "2026-10-05T12:00:00Z");
    record(other, ServiceCheckStore.BACKEND, "up", 0, "2026-10-05T12:00:00Z");

    var page0 = health.page("failing", token, PageRequest.of(0, 1, Sort.by("id")));
    var page1 = health.page("failing", token, PageRequest.of(1, 1, Sort.by("id")));

    assertThat(page0.getTotalElements()).isEqualTo(2);
    assertThat(page0.getContent()).extracting(Service::getId).doesNotContain(other.getId());
    assertThat(page1.getContent()).extracting(Service::getId).doesNotContain(other.getId());
    assertThat(page0.getContent().get(0).getId()).isNotEqualTo(page1.getContent().get(0).getId());
  }

  @Test
  @DisplayName("An unknown health value is rejected")
  void unknownHealthIsBadRequest() {
    assertThatThrownBy(() -> health.page("bogus", null, PageRequest.of(0, 10)))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(error -> ((ResponseStatusException) error).getStatusCode())
        .isEqualTo(HttpStatus.BAD_REQUEST);
  }

  private List<Integer> ids(String filter) {
    return health.page(filter, token, Pageable.unpaged()).map(Service::getId).getContent();
  }

  private void record(Service service, String observer, String status, int rank, String at) {
    store.record(service, observer, new ServiceStatus(status, rank), 10L, "", Instant.parse(at));
  }

  private Service save(String name) {
    return serviceRepository.save(
        Service.builder()
            .name(token + " " + name)
            .type("WMS")
            .serviceURL("http://example.test/" + name)
            .blocked(false)
            .isProxied(false)
            .build());
  }

  @TestConfiguration
  @Import({I18nTestConfiguration.class, ServiceCheckStore.class, ServiceHealthQuery.class})
  static class Configuration {}
}
