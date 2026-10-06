package org.sitmun.domain.service.usage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.application.ApplicationRepository;
import org.sitmun.domain.service.Service;
import org.sitmun.domain.service.ServiceRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("Service usage queries")
class ServiceUsageQueriesTest {

  @Mock private ServiceUsageRepository usage;
  @Mock private ServiceRepository services;
  @Mock private ApplicationRepository applications;

  @Test
  @DisplayName("The usage view lists every application")
  void listsEveryApplication() {
    Service service = Service.builder().id(7).name("WMS").type("WMS").isProxied(true).build();
    when(services.findById(7)).thenReturn(Optional.of(service));
    LocalDate today = LocalDate.of(2026, 10, 6);
    List<ServiceUsage> rows = new ArrayList<>();
    List<Application> apps = new ArrayList<>();
    for (int id = 1; id <= 11; id++) {
      ServiceUsage row = new ServiceUsage();
      row.setId(new ServiceUsageKey(7, id, today, UsageOperation.GET_MAP));
      row.setRequests(12L - id);
      row.setFailed(0);
      rows.add(row);
      apps.add(Application.builder().id(id).name("App " + id).type("I").build());
    }
    when(usage.findByIdServiceIdAndIdUsageDayGreaterThanEqualAndIdUsageDayLessThan(
            any(), any(), any()))
        .thenReturn(rows);
    when(applications.findAllById(any())).thenReturn(apps);

    ServiceUsageView view =
        new ServiceUsageQueries(usage, services, applications, false, ZoneId.of("UTC"))
            .view(7, "30d", Instant.parse("2026-10-06T12:00:00Z"));

    assertThat(view.applications()).extracting(UsageApplicationTotal::applicationId).hasSize(11);
    assertThat(view.applications()).extracting(UsageApplicationTotal::name).contains("App 11");
  }
}
