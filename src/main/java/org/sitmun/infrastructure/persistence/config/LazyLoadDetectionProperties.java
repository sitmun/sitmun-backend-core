package org.sitmun.infrastructure.persistence.config;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for lazy-loading detection.
 *
 * <p>Controls Hibernate listener behavior for detecting N+1 query patterns via lazy collection and
 * entity initialization tracking.
 *
 * <p><b>Modes:</b>
 *
 * <ul>
 *   <li>{@code OFF} — no detection (default for prod)
 *   <li>{@code WARN} — log lazy loads with counts (for dev)
 *   <li>{@code FAIL} — throw on lazy loads (for strict tests)
 * </ul>
 *
 * <p>Enable via:
 *
 * <pre>
 * sitmun.lazy-detection.enabled=true
 * sitmun.lazy-detection.mode=fail
 * </pre>
 */
@ConfigurationProperties(prefix = "sitmun.lazy-detection")
@Validated
@Getter
@Setter
public class LazyLoadDetectionProperties {

  /** Enable lazy-load detection. Default: false (disabled). */
  private boolean enabled = false;

  /** Detection mode. Default: OFF. */
  @NotNull private DetectionMode mode = DetectionMode.OFF;

  /** Maximum lazy loads to log before suppressing (prevent log spam). Default: 100. */
  private int maxLoggedLoads = 100;

  public enum DetectionMode {
    /** No detection. */
    OFF,
    /** Log warnings with counts. */
    WARN,
    /** Throw exceptions on lazy loads. */
    FAIL
  }
}
