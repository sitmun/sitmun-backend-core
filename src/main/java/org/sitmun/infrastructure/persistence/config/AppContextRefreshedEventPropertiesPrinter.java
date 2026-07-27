package org.sitmun.infrastructure.persistence.config;

import java.util.Collection;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.sitmun.infrastructure.config.Profiles;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.stereotype.Component;

@Component
@Profile(Profiles.DEV)
@Slf4j
public class AppContextRefreshedEventPropertiesPrinter {

  private static final String REDACTED = "<redacted>";

  @EventListener
  public void handleContextRefreshed(ContextRefreshedEvent event) {
    ConfigurableEnvironment env =
        (ConfigurableEnvironment) event.getApplicationContext().getEnvironment();
    env.getPropertySources().stream()
        .filter(MapPropertySource.class::isInstance)
        .map(ps -> ((MapPropertySource) ps).getSource().keySet())
        .flatMap(Collection::stream)
        .distinct()
        .sorted()
        .forEach(key -> log.info("{}={}", key, displayValue(key, env.getProperty(key))));
  }

  /** Returns a loggable representation that redacts secret-bearing property values. */
  static String displayValue(String key, Object value) {
    if (value == null) {
      return null;
    }
    return isSensitiveKey(key) ? REDACTED : String.valueOf(value);
  }

  static boolean isSensitiveKey(String key) {
    if (key == null || key.isBlank()) {
      return false;
    }
    String normalized = key.toLowerCase(Locale.ROOT);
    return normalized.contains("password")
        || normalized.contains("secret")
        || normalized.contains("token")
        || normalized.contains("credential");
  }
}
