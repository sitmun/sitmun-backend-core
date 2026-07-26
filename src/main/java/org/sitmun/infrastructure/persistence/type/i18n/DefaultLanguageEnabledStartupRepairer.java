package org.sitmun.infrastructure.persistence.type.i18n;

import lombok.extern.slf4j.Slf4j;
import org.sitmun.SitmunConstants;
import org.sitmun.domain.configuration.ConfigurationParameter;
import org.sitmun.domain.configuration.ConfigurationParameterRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ensures the configured database default language ({@code language.default}) is enabled at
 * startup. Soft-repairs inconsistent data without aborting the process.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 30)
@Slf4j
public class DefaultLanguageEnabledStartupRepairer implements ApplicationRunner {

  private final ConfigurationParameterRepository configurationParameterRepository;
  private final LanguageRepository languageRepository;

  public DefaultLanguageEnabledStartupRepairer(
      ConfigurationParameterRepository configurationParameterRepository,
      LanguageRepository languageRepository) {
    this.configurationParameterRepository = configurationParameterRepository;
    this.languageRepository = languageRepository;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    String defaultShortname =
        configurationParameterRepository
            .findByName(SitmunConstants.LANGUAGE_DEFAULT_CONF_KEY)
            .map(ConfigurationParameter::getValue)
            .map(String::trim)
            .filter(value -> !value.isEmpty())
            .orElse(null);
    if (defaultShortname == null) {
      log.debug(
          "No {} configuration; skipping default-language enablement repair",
          SitmunConstants.LANGUAGE_DEFAULT_CONF_KEY);
      return;
    }

    languageRepository
        .findByShortname(defaultShortname)
        .ifPresentOrElse(
            language -> {
              if (Boolean.FALSE.equals(language.getEnabled())) {
                language.setEnabled(true);
                languageRepository.save(language);
                log.warn(
                    "Re-enabled database default language '{}' (was disabled)", defaultShortname);
              }
            },
            () ->
                log.warn(
                    "Configured {}='{}' does not match any Language.shortname",
                    SitmunConstants.LANGUAGE_DEFAULT_CONF_KEY,
                    defaultShortname));
  }
}
