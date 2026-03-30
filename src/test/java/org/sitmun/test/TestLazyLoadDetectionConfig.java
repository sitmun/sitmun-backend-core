package org.sitmun.test;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.internal.SessionFactoryImpl;
import org.sitmun.infrastructure.config.Profiles;
import org.sitmun.infrastructure.persistence.config.LazyLoadDetectionCollectionListener;
import org.sitmun.infrastructure.persistence.config.LazyLoadDetectionProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Test-only configuration for Hibernate lazy-loading detection.
 *
 * <p>This configuration is activated in the test profile. It registers event listener independently
 * of the main LazyLoadDetectionConfig. The behavior is controlled by properties in
 * application-test.yml.
 *
 * <p><b>Scope:</b> Tracks lazy collection initialization only (via {@code INIT_COLLECTION} event).
 * Entity-level {@code LOAD} events are not tracked to avoid false positives from normal query
 * results.
 */
@Slf4j
@Configuration
@Profile(Profiles.TEST)
public class TestLazyLoadDetectionConfig {

  private final EntityManagerFactory entityManagerFactory;

  @Value("${sitmun.lazy-detection.enabled:false}")
  private boolean enabled;

  @Value("${sitmun.lazy-detection.mode:OFF}")
  private String mode;

  @Value("${sitmun.lazy-detection.max-logged-loads:100}")
  private int maxLoggedLoads;

  public TestLazyLoadDetectionConfig(EntityManagerFactory entityManagerFactory) {
    this.entityManagerFactory = entityManagerFactory;
  }

  @PostConstruct
  public void registerLazyLoadListeners() {
    log.info(
        "TestLazyLoadDetectionConfig @PostConstruct invoked: enabled={}, mode={}, maxLoggedLoads={}",
        enabled,
        mode,
        maxLoggedLoads);

    if (!enabled) {
      log.info("Test lazy-load detection is disabled; skipping listener registration");
      return;
    }

    LazyLoadDetectionProperties.DetectionMode detectionMode;
    try {
      detectionMode = LazyLoadDetectionProperties.DetectionMode.valueOf(mode.toUpperCase());
    } catch (IllegalArgumentException e) {
      log.warn("Invalid lazy-detection mode '{}', defaulting to OFF", mode);
      detectionMode = LazyLoadDetectionProperties.DetectionMode.OFF;
    }

    if (detectionMode == LazyLoadDetectionProperties.DetectionMode.OFF) {
      log.info("Test lazy-load detection mode is OFF; skipping listener registration");
      return;
    }

    SessionFactoryImpl sessionFactory = entityManagerFactory.unwrap(SessionFactoryImpl.class);
    EventListenerRegistry registry =
        sessionFactory.getServiceRegistry().getService(EventListenerRegistry.class);

    LazyLoadDetectionCollectionListener collectionListener =
        new LazyLoadDetectionCollectionListener(detectionMode, maxLoggedLoads);

    registry.appendListeners(EventType.INIT_COLLECTION, collectionListener);

    log.info(
        "Test lazy-load detection registered (collection listener only): mode={}, maxLoggedLoads={}",
        detectionMode,
        maxLoggedLoads);
  }
}
