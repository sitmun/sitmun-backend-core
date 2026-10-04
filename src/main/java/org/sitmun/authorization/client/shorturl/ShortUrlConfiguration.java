package org.sitmun.authorization.client.shorturl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ShortUrlProperties.class)
public class ShortUrlConfiguration {

  @Bean
  SelectedShortUrlProvider selectedShortUrlProvider(
      List<ShortUrlProvider> providers, ShortUrlProperties properties) {
    return new SelectedShortUrlProvider(select(providers, properties.getProvider()));
  }

  static ShortUrlProvider select(List<ShortUrlProvider> providers, String id) {
    Map<String, ShortUrlProvider> byId = new HashMap<>();
    for (ShortUrlProvider provider : providers) {
      ShortUrlProvider previous = byId.put(provider.id(), provider);
      if (previous != null) {
        throw new IllegalStateException("Duplicate short URL provider id " + provider.id());
      }
    }
    ShortUrlProvider selected = byId.get(id);
    if (selected == null) {
      throw new IllegalStateException("Missing short URL provider " + id);
    }
    return selected;
  }
}

record SelectedShortUrlProvider(ShortUrlProvider provider) {}
