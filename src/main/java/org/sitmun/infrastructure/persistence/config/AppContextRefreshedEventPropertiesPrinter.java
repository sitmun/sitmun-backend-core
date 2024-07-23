package org.sitmun.infrastructure.persistence.config;


import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
@Profile("dev")
@Slf4j
public class AppContextRefreshedEventPropertiesPrinter {

  @EventListener
  public void handleContextRefreshed(ContextRefreshedEvent event) {
    ConfigurableEnvironment env = (ConfigurableEnvironment) event.getApplicationContext().getEnvironment();
    env.getPropertySources()
      .stream()
      .filter(ps -> ps instanceof MapPropertySource)
      .map(ps -> ((MapPropertySource) ps).getSource().keySet())
      .flatMap(Collection::stream)
      .distinct()
      .sorted()
      .forEach(key -> log.info("{}={}", key, env.getProperty(key)));
  }

}
