package org.sitmun.infrastructure.persistence.type.i18n;

import jakarta.validation.constraints.NotNull;
import org.sitmun.SitmunConstants;
import org.sitmun.domain.configuration.ConfigurationParameter;
import org.sitmun.domain.configuration.ConfigurationParameterRepository;
import org.sitmun.infrastructure.persistence.exception.RequirementException;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.HandleBeforeSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RepositoryEventHandler
public class LanguageEventHandler {

  static final String CANNOT_DISABLE_DEFAULT_LANGUAGE =
      "Cannot disable the database default language";

  private final ConfigurationParameterRepository configurationParameterRepository;

  public LanguageEventHandler(ConfigurationParameterRepository configurationParameterRepository) {
    this.configurationParameterRepository = configurationParameterRepository;
  }

  @HandleBeforeCreate
  @HandleBeforeSave
  @Transactional(rollbackFor = RequirementException.class)
  public void handleLanguagePersist(@NotNull Language language) {
    if (!Boolean.FALSE.equals(language.getEnabled())) {
      return;
    }
    String defaultShortname =
        configurationParameterRepository
            .findByName(SitmunConstants.LANGUAGE_DEFAULT_CONF_KEY)
            .map(ConfigurationParameter::getValue)
            .orElse(null);
    if (defaultShortname != null && defaultShortname.equals(language.getShortname())) {
      throw new RequirementException(CANNOT_DISABLE_DEFAULT_LANGUAGE);
    }
  }
}
