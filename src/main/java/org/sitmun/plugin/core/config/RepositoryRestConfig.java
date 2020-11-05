package org.sitmun.plugin.core.config;

import javax.persistence.EntityManager;
import javax.persistence.metamodel.Type;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.event.ValidatingRepositoryEventListener;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.validation.Validator;

public class RepositoryRestConfig implements RepositoryRestConfigurer {

  @Autowired
  private EntityManager entityManager;

  private final Validator validator;

  public RepositoryRestConfig(Validator validator) {
    this.validator = validator;
  }

  /**
   * Links the annotation based validator to save and create events.
   */
  @Override
  public void configureValidatingRepositoryEventListener(
      ValidatingRepositoryEventListener validatingListener) {
    validatingListener.addValidator("beforeSave", validator);
    validatingListener.addValidator("beforeCreate", validator);
  }

  @Override
  public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config) {
    config.setReturnBodyForPutAndPost(true);
    config.exposeIdsFor(entityManager.getMetamodel().getEntities().stream()
        .map(Type::getJavaType)
        .toArray(Class[]::new));
  }

}
