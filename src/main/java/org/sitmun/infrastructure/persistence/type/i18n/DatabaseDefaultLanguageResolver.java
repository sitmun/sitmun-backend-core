package org.sitmun.infrastructure.persistence.type.i18n;

import java.util.Optional;
import org.sitmun.SitmunConstants;
import org.sitmun.domain.configuration.ConfigurationParameterRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Resolves the runtime database default language ({@code language.default}), falling back to {@code
 * sitmun.language} when the configuration parameter is absent.
 */
@Component
public class DatabaseDefaultLanguageResolver {

  private final ConfigurationParameterRepository configurationParameterRepository;
  private final LanguageRepository languageRepository;
  private final String propertyDefaultLanguage;

  public DatabaseDefaultLanguageResolver(
      ConfigurationParameterRepository configurationParameterRepository,
      LanguageRepository languageRepository,
      @Value("${sitmun.language:en}") String propertyDefaultLanguage) {
    this.configurationParameterRepository = configurationParameterRepository;
    this.languageRepository = languageRepository;
    this.propertyDefaultLanguage = propertyDefaultLanguage;
  }

  /** Shortname from {@code STM_CONF} {@code language.default}, else {@code sitmun.language}. */
  public String resolveShortname() {
    return configurationParameterRepository
        .findByName(SitmunConstants.LANGUAGE_DEFAULT_CONF_KEY)
        .map(param -> param.getValue())
        .filter(StringUtils::hasText)
        .orElse(propertyDefaultLanguage);
  }

  /** Optional shortname from DB only (no property fallback). */
  public Optional<String> findConfiguredShortname() {
    return configurationParameterRepository
        .findByName(SitmunConstants.LANGUAGE_DEFAULT_CONF_KEY)
        .map(param -> param.getValue())
        .filter(StringUtils::hasText);
  }

  /**
   * Language entity for {@link #resolveShortname()}, or empty if unknown in {@code STM_LANGUAGE}.
   */
  public Optional<Language> findLanguage() {
    return languageRepository.findByShortname(resolveShortname());
  }

  /** Language entity for {@link #resolveShortname()}. */
  public Language requireLanguage() {
    String shortname = resolveShortname();
    return languageRepository
        .findByShortname(shortname)
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "Default language not found in STM_LANGUAGE: " + shortname));
  }
}
