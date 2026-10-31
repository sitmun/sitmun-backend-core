package org.sitmun.domain.service.usage;

import jakarta.annotation.PreDestroy;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.sitmun.administration.service.access.ServiceCheckProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ViewerUsageScheduler {

  private static final Logger log = LoggerFactory.getLogger(ViewerUsageScheduler.class);

  private final ViewerUsage usage;
  private final ScheduledExecutorService scheduler;

  public ViewerUsageScheduler(ViewerUsage usage, ServiceCheckProperties properties) {
    this.usage = usage;
    if (!Boolean.TRUE.equals(properties.enabled())) {
      this.scheduler = null;
      return;
    }
    this.scheduler =
        Executors.newSingleThreadScheduledExecutor(
            runnable -> {
              Thread thread = new Thread(runnable, "viewer-usage");
              thread.setDaemon(true);
              return thread;
            });
    long delay = properties.viewerFlushInterval().toMillis();
    scheduler.scheduleWithFixedDelay(this::flush, delay, delay, TimeUnit.MILLISECONDS);
  }

  @PreDestroy
  void shutdown() {
    if (scheduler == null) {
      return;
    }
    flush();
    scheduler.shutdown();
  }

  private void flush() {
    try {
      usage.flush();
    } catch (RuntimeException ex) {
      log.warn("Viewer usage flush failed: {}", ex.getClass().getSimpleName());
    }
  }
}
