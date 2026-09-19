package org.sitmun.administration.service.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MultiGauge;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.Collections;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GenericDashboardMetricsContributorTest {

  @Mock private EntityManager entityManager;

  @Mock private Query query;

  private SimpleMeterRegistry registry;

  private MultiGauge gauge;

  @BeforeEach
  void setUp() {
    registry = new SimpleMeterRegistry();
    gauge = MultiGauge.builder("sitmun.users-per-application").register(registry);
    when(entityManager.createQuery(anyString())).thenReturn(query);
    when(query.getParameters()).thenReturn(Set.of());
  }

  @Test
  void publishesApplicationNameAndUserCount() {
    when(query.getResultList())
        .thenReturn(Collections.singletonList(new Object[] {"IDE genèric Menorca", 6L}));

    contributor().run();

    Gauge published =
        registry
            .find("sitmun.users-per-application")
            .tag(DashboardInfoContributor.TAG, "IDE genèric Menorca")
            .gauge();
    assertThat(published).isNotNull();
    assertThat(published.value()).isEqualTo(6.0);
  }

  @Test
  void stillPublishesYearMonthCounts() {
    when(query.getResultList()).thenReturn(Collections.singletonList(new Object[] {2024, 3, 4L}));

    contributor().run();

    Gauge published =
        registry
            .find("sitmun.users-per-application")
            .tag(DashboardInfoContributor.TAG, "2024-03")
            .gauge();
    assertThat(published).isNotNull();
    assertThat(published.value()).isEqualTo(4.0);
  }

  private GenericDashboardMetricsContributor contributor() {
    DashboardProperties.MetricDefinition definition = new DashboardProperties.MetricDefinition();
    definition.setSize(30);
    definition.setQuery("unused");
    return new GenericDashboardMetricsContributor(gauge, entityManager, definition);
  }
}
