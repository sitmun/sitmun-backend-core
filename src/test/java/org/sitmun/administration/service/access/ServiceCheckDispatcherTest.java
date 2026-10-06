package org.sitmun.administration.service.access;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.application.ApplicationRepository;
import org.sitmun.domain.service.Service;
import org.sitmun.domain.service.ServiceRepository;
import org.sitmun.domain.service.check.ServiceCheck;
import org.sitmun.domain.service.check.ServiceCheckRepository;
import org.sitmun.domain.service.check.ServiceCheckSample;
import org.sitmun.domain.service.check.ServiceCheckSampleRepository;
import org.sitmun.domain.service.check.ServiceCheckStore;
import org.sitmun.domain.service.usage.ServiceUsage;
import org.sitmun.domain.service.usage.ServiceUsageRepository;
import org.sitmun.domain.service.usage.ServiceUsageStore;
import org.sitmun.infrastructure.persistence.type.i18n.I18nTestConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(LiquibaseAutoConfiguration.class)
@TestPropertySource(properties = "sitmun.service-check.enabled=true")
@DisplayName("Service check dispatcher")
class ServiceCheckDispatcherTest {

  @Autowired private ServiceCheckDispatcher dispatcher;
  @Autowired private ServiceCheckProperties properties;
  @Autowired private ServiceCheckInFlight inFlight;
  @Autowired private Clock clock;
  @Autowired private ProbeExecutor workers;
  @Autowired private ServiceUsageStore usageStore;
  @Autowired private ServiceCheckStore store;
  @Autowired private ServiceRepository serviceRepository;
  @Autowired private ServiceCheckRepository checkRepository;
  @Autowired private ServiceCheckSampleRepository sampleRepository;
  @Autowired private ServiceUsageRepository usageRepository;
  @Autowired private ApplicationRepository applicationRepository;
  @MockitoBean private ServiceAccessCheckExecutor checks;

  private CountDownLatch releaseProbe;

  @BeforeEach
  void resetWorkers() {
    workers.async(false);
    releaseProbe = new CountDownLatch(1);
    Instant fresh = clock.instant();
    for (Service existing : serviceRepository.findAll()) {
      var row =
          checkRepository.findByService_IdAndObserver(existing.getId(), ServiceCheckStore.BACKEND);
      if (row.isEmpty()) {
        observe(existing, fresh);
      } else {
        row.get().setObservedAt(fresh);
        checkRepository.save(row.get());
      }
    }
  }

  @AfterEach
  void finishProbes() throws InterruptedException {
    releaseProbe.countDown();
    long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
    while (inFlight.availablePermits() < properties.maxInFlight() && System.nanoTime() < end) {
      Thread.sleep(10);
    }
  }

  @Test
  @DisplayName("enabled=false does not probe")
  void disabledDispatcherDoesNotProbe() {
    ServiceAccessCheckExecutor localChecks = mock(ServiceAccessCheckExecutor.class);
    ServiceCheckDueQuery localDue = mock(ServiceCheckDueQuery.class);
    when(localDue.first(any(), any()))
        .thenReturn(Optional.of(new DueService(4, "http://alpha.example/wms", null)));
    ServiceCheckInFlight permits = new ServiceCheckInFlight(properties.maxInFlight());
    Executor inline = Runnable::run;
    ServiceCheckDispatcher off = dispatcher(false, localChecks, localDue, permits, inline);
    off.dispatch();
    verifyNoInteractions(localChecks, localDue);

    ServiceCheckDispatcher on = dispatcher(true, localChecks, localDue, permits, inline);
    on.dispatch();
    verify(localChecks).check(4, false);
  }

  @Test
  @DisplayName("A blocked service with no backend row is due")
  void blockedServiceWithNoRowIsDue() {
    Service blocked = saveService("Blocked WMS", "http://blocked.example/wms", true);
    Service fresh = saveService("Fresh WMS", "http://fresh.example/wms", false);
    observe(fresh, clock.instant());

    dispatcher.dispatch();

    verify(checks).check(blocked.getId(), false);
    verify(checks, never()).check(eq(fresh.getId()), eq(false));
    assertThat(serviceRepository.findById(blocked.getId()).orElseThrow().getBlocked()).isTrue();
  }

  @Test
  @DisplayName("Never-probed is chosen before an old row")
  void neverProbedIsChosenBeforeAnOldRow() {
    Service old = saveService("Old WMS", "http://old.example/wms", false);
    Service never = saveService("Never WMS", "http://never.example/wms", false);
    observe(old, clock.instant().minus(properties.interval()).minusSeconds(1));

    dispatcher.dispatch();

    verify(checks).check(never.getId(), false);
    verify(checks, never()).check(eq(old.getId()), eq(false));
  }

  @Test
  @DisplayName("A service observed inside interval is not due")
  void serviceObservedInsideIntervalIsNotDue() {
    Service fresh = saveService("Fresh WMS", "http://fresh.example/wms", false);
    Service old = saveService("Old WMS", "http://old.example/wms", false);
    observe(fresh, clock.instant().minus(properties.interval()).plusSeconds(1));
    observe(old, clock.instant().minus(properties.interval()).minusSeconds(1));

    dispatcher.dispatch();

    verify(checks).check(old.getId(), false);
    verify(checks, never()).check(eq(fresh.getId()), eq(false));
  }

  @Test
  @DisplayName("No free permit: no probe starts, the service stays due")
  void noFreePermitLeavesTheServiceDue() throws InterruptedException {
    Service dueService = saveService("Due WMS", "http://due.example/wms", false);
    int held = 0;
    try {
      for (int i = 0; i < properties.maxInFlight(); i++) {
        inFlight.acquire();
        held++;
      }
      dispatcher.dispatch();
      verify(checks, never()).check(any(), eq(false));
    } finally {
      for (int i = 0; i < held; i++) {
        inFlight.release();
      }
    }

    dispatcher.dispatch();

    verify(checks).check(dueService.getId(), false);
  }

  @Test
  @DisplayName("Host already at max-per-host: that host is skipped and another host may start")
  void fullHostIsSkippedForAnotherHost() throws InterruptedException {
    Service alpha = saveService("Alpha WMS", "http://alpha.example/wms", false);
    Service sameHost = saveService("Alpha again", "http://ALPHA.example/wms", false);
    Service beta = saveService("Beta WMS", "http://beta.example/wms", false);
    workers.async(true);
    CountDownLatch entered = new CountDownLatch(1);
    CountDownLatch betaStarted = new CountDownLatch(1);
    when(checks.check(eq(alpha.getId()), eq(false)))
        .thenAnswer(
            invocation -> {
              entered.countDown();
              releaseProbe.await(5, TimeUnit.SECONDS);
              return Optional.empty();
            });
    when(checks.check(eq(beta.getId()), eq(false)))
        .thenAnswer(
            invocation -> {
              betaStarted.countDown();
              return Optional.empty();
            });

    dispatcher.dispatch();
    assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
    dispatcher.dispatch();
    assertThat(betaStarted.await(5, TimeUnit.SECONDS)).isTrue();
    verify(checks, never()).check(eq(sameHost.getId()), eq(false));
  }

  @Test
  @DisplayName(
      "Retention deletes an old sample and an old usage row and keeps the latest check row")
  void retentionDeletesOldSampleAndUsageAndKeepsLatestCheck() {
    Service service = saveService("Retained WMS", "http://retained.example/wms", false);
    Application application =
        applicationRepository.save(
            Application.builder()
                .name("retained-app")
                .type("I")
                .createdDate(Date.from(clock.instant()))
                .lastUpdate(Date.from(clock.instant()))
                .build());
    Instant now = clock.instant();
    Instant cutoff = now.minus(properties.sampleRetention());
    ServiceCheck latest =
        checkRepository.save(
            ServiceCheck.builder()
                .service(service)
                .observer(ServiceCheckStore.BACKEND)
                .status("up")
                .statusRank(0)
                .elapsedMs(4L)
                .detail("kept")
                .observedAt(cutoff.minusSeconds(1))
                .build());
    sampleRepository.save(sample(service, cutoff.minusSeconds(1), "timeout"));
    ServiceCheckSample fresh = sampleRepository.save(sample(service, now, "up"));
    LocalDate today = now.atZone(ZoneId.systemDefault()).toLocalDate();
    long days = properties.usageRetention().toDays();
    usageStore.add(service.getId(), application.getId(), today.minusDays(days + 1), "GetMap", 8, 1);
    usageStore.add(service.getId(), application.getId(), today.minusDays(days), "GetMap", 3, 0);

    dispatcher.retain();

    assertThat(checkRepository.findById(latest.getId()).orElseThrow().getDetail())
        .isEqualTo("kept");
    assertThat(
            sampleRepository.findByService_IdAndObserverOrderByIdAsc(
                service.getId(), ServiceCheckStore.BACKEND))
        .singleElement()
        .extracting(ServiceCheckSample::getId)
        .isEqualTo(fresh.getId());
    List<ServiceUsage> rows =
        usageRepository.findByIdServiceIdAndIdOperation(service.getId(), "GetMap");
    assertThat(rows).singleElement().extracting(ServiceUsage::getRequests).isEqualTo(3L);
    assertThat(rows.get(0).getId().getUsageDay()).isEqualTo(today.minusDays(days));
  }

  private ServiceCheckDispatcher dispatcher(
      boolean enabled,
      ServiceAccessCheckExecutor localChecks,
      ServiceCheckDueQuery localDue,
      ServiceCheckInFlight permits,
      Executor inline) {
    return new ServiceCheckDispatcher(
        withEnabled(enabled), permits, localChecks, localDue, clock, inline, usageStore, store);
  }

  private ServiceCheckProperties withEnabled(boolean enabled) {
    return new ServiceCheckProperties(
        enabled,
        properties.interval(),
        properties.dispatchInterval(),
        properties.maxInFlight(),
        properties.maxPerHost(),
        properties.timeout(),
        properties.connectTimeout(),
        properties.readTimeout(),
        properties.sampleRetention(),
        properties.usageRetention(),
        properties.retentionInterval(),
        properties.overdueWarnFactor(),
        properties.viewerFlushInterval(),
        properties.detailMaxLength(),
        properties.exceptionTextMaxLength(),
        properties.scanMaxBytes());
  }

  private Service saveService(String name, String url, boolean blocked) {
    return serviceRepository.save(
        Service.builder()
            .name(name)
            .type("WMS")
            .serviceURL(url)
            .blocked(blocked)
            .isProxied(false)
            .build());
  }

  private void observe(Service service, Instant observedAt) {
    checkRepository.save(
        ServiceCheck.builder()
            .service(service)
            .observer(ServiceCheckStore.BACKEND)
            .status("up")
            .statusRank(0)
            .elapsedMs(1L)
            .detail("")
            .observedAt(observedAt)
            .build());
  }

  private ServiceCheckSample sample(Service service, Instant observedAt, String status) {
    return ServiceCheckSample.builder()
        .service(service)
        .observer(ServiceCheckStore.BACKEND)
        .status(status)
        .statusRank(0)
        .elapsedMs(1L)
        .observedAt(observedAt)
        .build();
  }

  @TestConfiguration
  @EnableConfigurationProperties(ServiceCheckProperties.class)
  @Import({
    I18nTestConfiguration.class,
    ServiceCheckStore.class,
    ServiceUsageStore.class,
    ServiceCheckDueQuery.class,
    ServiceCheckDispatcher.class
  })
  static class Configuration {

    @Bean
    Clock serviceCheckClock() {
      return Clock.fixed(Instant.parse("2026-10-05T12:00:00Z"), ZoneOffset.UTC);
    }

    @Bean
    ServiceCheckInFlight serviceCheckInFlight(ServiceCheckProperties properties) {
      return new ServiceCheckInFlight(properties.maxInFlight());
    }

    @Bean(destroyMethod = "close")
    ProbeExecutor serviceCheckExecutor() {
      return new ProbeExecutor();
    }
  }

  static final class ProbeExecutor implements Executor, AutoCloseable {

    private final ExecutorService pool =
        Executors.newCachedThreadPool(
            runnable -> {
              Thread thread = new Thread(runnable, "service-check-test");
              thread.setDaemon(true);
              return thread;
            });
    private final AtomicBoolean async = new AtomicBoolean();

    void async(boolean enabled) {
      async.set(enabled);
    }

    @Override
    public void execute(Runnable command) {
      if (async.get()) {
        pool.execute(command);
      } else {
        command.run();
      }
    }

    @Override
    public void close() {
      pool.shutdownNow();
    }
  }
}
