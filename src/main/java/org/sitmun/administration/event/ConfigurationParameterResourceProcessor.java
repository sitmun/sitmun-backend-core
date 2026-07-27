package org.sitmun.administration.event;

import org.sitmun.domain.configuration.ConfigurationParameter;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * Spring Data REST omits JPA {@code @Transient} fields from {@code PersistentEntityResource}. When
 * save-time {@link ConfigurationParameter#getWarnings()} are present, re-wrap as a plain {@link
 * EntityModel} so Jackson includes them in the POST/PUT body. GET responses leave warnings unset
 * and keep the default resource representation.
 */
@Component
public class ConfigurationParameterResourceProcessor
    implements RepresentationModelProcessor<EntityModel<ConfigurationParameter>> {

  @Override
  @NonNull
  public EntityModel<ConfigurationParameter> process(
      @NonNull EntityModel<ConfigurationParameter> model) {
    ConfigurationParameter content = model.getContent();
    if (content == null || content.getWarnings() == null || content.getWarnings().isEmpty()) {
      return model;
    }
    return EntityModel.of(content, model.getLinks());
  }
}
