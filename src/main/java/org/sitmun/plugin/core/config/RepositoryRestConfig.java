package org.sitmun.plugin.core.config;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.event.ValidatingRepositoryEventListener;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.validation.Validator;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

import javax.persistence.EntityManager;
import javax.persistence.metamodel.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class RepositoryRestConfig implements RepositoryRestConfigurer {

  private static final List<String> EVENTS;

  static {
    List<String> events = new ArrayList<>();
    events.add("beforeCreate");
    events.add("afterCreate");
    events.add("beforeSave");
    events.add("afterSave");
    events.add("beforeLinkSave");
    //events.add("afterLinkSave");
    events.add("beforeDelete");
    // events.add("afterDelete");
    EVENTS = Collections.unmodifiableList(events);
  }

  private final Validator validator;

  private final EntityManager entityManager;

  private final ListableBeanFactory beanFactory;

  public RepositoryRestConfig(Validator validator,
                              EntityManager entityManager,
                              ListableBeanFactory beanFactory
  ) {
    this.validator = validator;
    this.entityManager = entityManager;
    this.beanFactory = beanFactory;
  }

  /**
   * Links the annotation based validator to save and create events.
   */
  @Override
  public void configureValidatingRepositoryEventListener(
    ValidatingRepositoryEventListener validatingListener) {
    EVENTS.forEach(event -> validatingListener.addValidator(event, validator));

    Map<String, Validator> validators = beanFactory.getBeansOfType(Validator.class);
    for (Map.Entry<String, Validator> entry : validators.entrySet()) {
      EVENTS.stream().filter(p -> entry.getKey().startsWith(p)).findFirst()
        .ifPresent(p -> validatingListener.addValidator(p, entry.getValue()));
    }
  }

  @Override
  public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config, CorsRegistry corsRegistry) {
    config.setReturnBodyForPutAndPost(true);
    config.setBasePath("/api");
    config.exposeIdsFor(entityManager.getMetamodel().getEntities().stream()
      .map(Type::getJavaType)
      .toArray(Class[]::new));
  }
}
