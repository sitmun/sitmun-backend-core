package org.sitmun.infrastructure.web.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ForwardedHeaderFilter;

@Configuration
public class ForwardedHeaderConfigurer {
  @Bean
  ForwardedHeaderFilter forwardedHeaderFilter() {
    return new ForwardedHeaderFilter();
  }
}
