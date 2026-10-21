package org.sitmun.domain.service.check;

import static org.mockito.Mockito.mock;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import org.sitmun.administration.service.access.ServiceAccessCheckExecutor;
import org.sitmun.administration.service.access.ServiceAccessSummaries;
import org.sitmun.administration.service.access.ServiceCheckDispatcher;
import org.sitmun.administration.service.access.ServiceCheckDueQuery;
import org.sitmun.administration.service.access.ServiceCheckInFlight;
import org.sitmun.administration.service.access.ServiceCheckProperties;
import org.sitmun.domain.service.AffectedApplications;
import org.sitmun.domain.service.ServiceHealthQuery;
import org.sitmun.domain.service.usage.ServiceUsageQueries;
import org.sitmun.domain.service.usage.ServiceUsageStore;
import org.sitmun.infrastructure.persistence.type.i18n.I18nTestConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@TestConfiguration
@EnableConfigurationProperties(ServiceCheckProperties.class)
@Import({
  I18nTestConfiguration.class,
  ServiceCheckStore.class,
  ServiceHealthQuery.class,
  AffectedApplications.class,
  ServiceAccessSummaries.class,
  ServiceUsageQueries.class,
  ServiceUsageStore.class,
  ServiceCheckDueQuery.class,
  ServiceCheckDispatcher.class
})
public class ServiceCheckSliceConfiguration {

  @Bean
  ServiceAccessCheckExecutor serviceAccessCheckExecutor() {
    return mock(ServiceAccessCheckExecutor.class);
  }

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

  public static final class ProbeExecutor implements Executor, AutoCloseable {

    private final ExecutorService pool =
        Executors.newCachedThreadPool(
            runnable -> {
              Thread thread = new Thread(runnable, "service-check-test");
              thread.setDaemon(true);
              return thread;
            });
    private final AtomicBoolean async = new AtomicBoolean();

    public void async(boolean enabled) {
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
