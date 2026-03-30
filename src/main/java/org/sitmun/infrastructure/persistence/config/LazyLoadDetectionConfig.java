package org.sitmun.infrastructure.persistence.config;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.internal.SessionFactoryImpl;
import org.sitmun.infrastructure.config.Profiles;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Configuration for Hibernate lazy-loading detection.
 *
 * <p>Registers event listener to detect lazy collection initialization, helping identify N+1 query
 * patterns early in development and testing.
 *
 * <p><b>Activation:</b> Enabled via {@code sitmun.lazy-detection.enabled=true}.
 *
 * <p><b>Modes:</b>
 *
 * <ul>
 *   <li>{@code OFF} — no detection (default)
 *   <li>{@code WARN} — log warnings (for dev)
 *   <li>{@code FAIL} — throw exceptions (for strict tests)
 * </ul>
 *
 * <p><b>Usage examples:</b>
 *
 * <pre>
 * # Test profile (strict)
 * sitmun.lazy-detection.enabled=true
 * sitmun.lazy-detection.mode=fail
 *
 * # Dev profile (warn-only)
 * sitmun.lazy-detection.enabled=true
 * sitmun.lazy-detection.mode=warn
 *
 * # Prod profile (disabled)
 * sitmun.lazy-detection.enabled=false
 * </pre>
 *
 * <p><b>Note:</b> In test profile, use {@link org.sitmun.test.TestLazyLoadDetectionConfig} instead.
 *
 * <p><b>Scope:</b> Tracks lazy collection initialization only (via {@code INIT_COLLECTION} event).
 * Entity-level {@code LOAD} events are not tracked because they fire on all entity loads (including
 * normal query results), which would produce false positives and excessive noise.
 */
@Configuration
@EnableConfigurationProperties(LazyLoadDetectionProperties.class)
@ConditionalOnProperty(
    prefix = "sitmun.lazy-detection",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = false)
@Profile("!" + Profiles.TEST)
@Slf4j
public class LazyLoadDetectionConfig {

  private final EntityManagerFactory entityManagerFactory;
  private final LazyLoadDetectionProperties properties;

  public LazyLoadDetectionConfig(
      EntityManagerFactory entityManagerFactory, LazyLoadDetectionProperties properties) {
    this.entityManagerFactory = entityManagerFactory;
    this.properties = properties;
  }

  @PostConstruct
  public void registerLazyLoadListeners() {
    if (properties.getMode() == LazyLoadDetectionProperties.DetectionMode.OFF) {
      log.info("Lazy-load detection is enabled but mode is OFF; skipping listener registration");
      return;
    }

    SessionFactoryImpl sessionFactory = entityManagerFactory.unwrap(SessionFactoryImpl.class);
    EventListenerRegistry registry =
        sessionFactory.getServiceRegistry().getService(EventListenerRegistry.class);

    LazyLoadDetectionCollectionListener collectionListener =
        new LazyLoadDetectionCollectionListener(
            properties.getMode(), properties.getMaxLoggedLoads());

    registry.appendListeners(EventType.INIT_COLLECTION, collectionListener);

    log.info(
        "Lazy-load detection registered (collection listener only): mode={}, maxLoggedLoads={}",
        properties.getMode(),
        properties.getMaxLoggedLoads());
  }
}
