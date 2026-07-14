package org.sitmun.administration.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.constraints.NotNull;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.sitmun.SitmunConstants;
import org.sitmun.domain.configuration.ConfigurationParameter;
import org.sitmun.domain.configuration.ConfigurationParameterRepository;
import org.sitmun.infrastructure.persistence.type.i18n.Language;
import org.sitmun.infrastructure.persistence.type.i18n.LanguageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LanguageAdministrationService {

  private final LanguageRepository languageRepository;
  private final ConfigurationParameterRepository configurationParameterRepository;

  @Transactional
  public void setDefaultLanguage(@NotNull Integer languageId) {
    final Language language = findLanguage(languageId);

    languageRepository.clearAllDefaultFlags();
    language.setDefaultLanguage(Boolean.TRUE);
    languageRepository.save(language);

    ConfigurationParameter defaultLanguageParameter =
        configurationParameterRepository
            .findByName(SitmunConstants.LANGUAGE_DEFAULT_CONF_KEY)
            .orElseGet(
                () ->
                    ConfigurationParameter.builder()
                        .name(SitmunConstants.LANGUAGE_DEFAULT_CONF_KEY)
                        .build());
    defaultLanguageParameter.setValue(language.getShortname());
    configurationParameterRepository.save(defaultLanguageParameter);
  }

  @Transactional
  public void reorderLanguages(@NotNull List<Integer> languageIds) {
    final List<Language> languages = languageRepository.findAllByOrderByOrderAscIdAsc();

    boolean isValid =
        languages.stream()
            .map(Language::getId)
            .collect(Collectors.toCollection(HashSet::new))
            .equals(new HashSet<>(languageIds));

    if (!isValid) {
      throw new IllegalArgumentException(
          "Language reorder payload must contain all existing languages, and only existing languages");
    }

    for (int index = 0; index < languageIds.size(); index++) {
      Integer languageId = languageIds.get(index);
      Language language =
          languages.stream()
              .filter(lang -> Objects.equals(lang.getId(), languageId))
              .findFirst()
              .orElseThrow();

      language.setOrder(index);
    }

    languageRepository.saveAll(languages);
  }

  private Language findLanguage(Integer languageId) {
    return languageRepository
        .findById(languageId)
        .orElseThrow(() -> new EntityNotFoundException("Language not found: " + languageId));
  }
}
