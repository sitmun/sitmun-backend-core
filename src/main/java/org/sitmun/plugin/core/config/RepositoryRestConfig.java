package org.sitmun.plugin.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.event.ValidatingRepositoryEventListener;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@Configuration
public class RepositoryRestConfig implements RepositoryRestConfigurer {

  /**
   * Links the annotation based validator to save and create events.
   *
   * @param validatingListener
   */
  @Override
  public void configureValidatingRepositoryEventListener(
      ValidatingRepositoryEventListener validatingListener) {
    validatingListener.addValidator("beforeSave", validator());
    validatingListener.addValidator("beforeCreate", validator());
  }


  @Bean
  public Validator validator() {
    return new LocalValidatorFactoryBean();
  }
}
