package org.sitmun.infrastructure.persistence.config;

import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.LazyInitializationException;
import org.hibernate.event.spi.InitializeCollectionEvent;
import org.hibernate.event.spi.InitializeCollectionEventListener;

/**
 * Hibernate event listener that detects lazy collection initialization.
 *
 * <p>Tracks each lazy collection load and either logs a warning or throws an exception based on
 * configured detection mode. Helps identify N+1 query patterns where collections are accessed in
 * loops or outside proper fetch boundaries.
 */
@Slf4j
public class LazyLoadDetectionCollectionListener implements InitializeCollectionEventListener {

  private final LazyLoadDetectionProperties.DetectionMode mode;
  private final int maxLoggedLoads;
  private final AtomicInteger collectionLoadCount = new AtomicInteger(0);

  public LazyLoadDetectionCollectionListener(
      LazyLoadDetectionProperties.DetectionMode mode, int maxLoggedLoads) {
    this.mode = mode;
    this.maxLoggedLoads = maxLoggedLoads;
  }

  @Override
  public void onInitializeCollection(InitializeCollectionEvent event) {
    int count = collectionLoadCount.incrementAndGet();
    String role = event.getCollection().getRole();

    if (mode == LazyLoadDetectionProperties.DetectionMode.FAIL) {
      throw new LazyInitializationException(
          String.format(
              "Lazy loading of collection detected: %s (count: %d). "
                  + "This may indicate a missing fetch join or @BatchSize and can cause N+1 queries.",
              role, count));
    } else if (mode == LazyLoadDetectionProperties.DetectionMode.WARN && count <= maxLoggedLoads) {
      log.warn("Lazy collection load #{}: {} (limit: {} logged)", count, role, maxLoggedLoads);
      if (count == maxLoggedLoads) {
        log.warn("Lazy collection load logging limit reached; suppressing further warnings");
      }
    }
  }

  public int getCollectionLoadCount() {
    return collectionLoadCount.get();
  }

  public void reset() {
    collectionLoadCount.set(0);
  }
}
