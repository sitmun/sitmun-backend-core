package org.sitmun.administration.config;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.event.ValidatingRepositoryEventListener;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

import javax.persistence.EntityManager;
import javax.persistence.metamodel.Type;
import java.util.List;
import java.util.Map;

@Configuration
@EnableJpaAuditing
public class AdministrationRestConfigurer implements RepositoryRestConfigurer {

  private static final List<String> EVENTS;

  static {
    EVENTS = List.of("beforeCreate", "afterCreate", "beforeSave", "afterSave", "beforeLinkSave", "beforeDelete");
  }

  private final EntityManager entityManager;
  private final ListableBeanFactory beanFactory;

  public AdministrationRestConfigurer(EntityManager entityManager,
                                      ListableBeanFactory beanFactory
  ) {
    this.entityManager = entityManager;
    this.beanFactory = beanFactory;
  }

  @Bean
  public Validator validator() {
    return new LocalValidatorFactoryBean();
  }

  /**
   * Links the annotation based validator to save and create events.
   */
  @Override
  public void configureValidatingRepositoryEventListener(ValidatingRepositoryEventListener validatingListener) {
    EVENTS.forEach(event -> validatingListener.addValidator(event, validator()));

    Map<String, Validator> validators = beanFactory.getBeansOfType(Validator.class);
    for (Map.Entry<String, Validator> entry : validators.entrySet()) {
      EVENTS.stream()
        .filter(p -> entry.getKey().startsWith(p))
        .findFirst()
        .ifPresent(p -> validatingListener.addValidator(p, entry.getValue()));
    }
  }

  @Override
  public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config, CorsRegistry cors) {
    config.setReturnBodyForPutAndPost(true);
    config.setBasePath("/api");
    config.exposeIdsFor(entityManager.getMetamodel()
      .getEntities()
      .stream()
      .map(Type::getJavaType)
      .toArray(Class[]::new));
  }
}
