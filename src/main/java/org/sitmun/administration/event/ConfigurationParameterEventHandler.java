package org.sitmun.administration.event;

import org.sitmun.domain.configuration.ConfigurationParameter;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.HandleBeforeDelete;
import org.springframework.data.rest.core.annotation.HandleBeforeSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.stereotype.Component;

/**
 * Event handler to protect critical configuration parameters from direct modification. The
 * language.default parameter can only be changed through the safe migration API.
 */
@Component
@RepositoryEventHandler(ConfigurationParameter.class)
public class ConfigurationParameterEventHandler {

  private static final String LANGUAGE_DEFAULT_PARAM = "language.default";
  private static final String PROTECTION_MESSAGE =
      "Cannot modify 'language.default' directly. Use the language default change API (/api/language-default/change) to ensure lossless migration.";

  @HandleBeforeCreate
  public void handleBeforeCreate(ConfigurationParameter param) {
    if (LANGUAGE_DEFAULT_PARAM.equals(param.getName())) {
      throw new IllegalStateException(PROTECTION_MESSAGE);
    }
  }

  @HandleBeforeSave
  public void handleBeforeSave(ConfigurationParameter param) {
    if (LANGUAGE_DEFAULT_PARAM.equals(param.getName())) {
      throw new IllegalStateException(PROTECTION_MESSAGE);
    }
  }

  @HandleBeforeDelete
  public void handleBeforeDelete(ConfigurationParameter param) {
    if (LANGUAGE_DEFAULT_PARAM.equals(param.getName())) {
      throw new IllegalStateException(PROTECTION_MESSAGE);
    }
  }
}
