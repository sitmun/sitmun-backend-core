package org.sitmun.plugin.core.autoconfigure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurerAdapter;
import org.springframework.data.web.HateoasPageableHandlerMethodArgumentResolver;

@Configuration
public class RestApiConfiguration extends RepositoryRestConfigurerAdapter {

  @Bean
  public HateoasPageableHandlerMethodArgumentResolver customResolver(
      HateoasPageableHandlerMethodArgumentResolver pageableResolver) {
    pageableResolver.setOneIndexedParameters(true);
    pageableResolver.setFallbackPageable(null);
    return pageableResolver;
  }
}