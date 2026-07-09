package org.sitmun.administration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sitmun.SitmunConstants;
import org.sitmun.domain.configuration.ConfigurationParameter;
import org.sitmun.domain.configuration.ConfigurationParameterRepository;
import org.sitmun.infrastructure.persistence.type.i18n.Language;
import org.sitmun.infrastructure.persistence.type.i18n.LanguageRepository;

@ExtendWith(MockitoExtension.class)
class LanguageAdministrationServiceTest {

  @Mock private LanguageRepository languageRepository;
  @Mock private ConfigurationParameterRepository configurationParameterRepository;

  @InjectMocks private LanguageAdministrationService service;

  @Test
  @DisplayName("setDefaultLanguage updates language flag and legacy configuration parameter")
  void setDefaultLanguageUpdatesFlagAndConfigurationParameter() {
    Language language = Language.builder().id(3).shortname("ca").name("Catala").build();
    ConfigurationParameter parameter =
        ConfigurationParameter.builder()
            .name(SitmunConstants.LANGUAGE_DEFAULT_CONF_KEY)
            .value("es")
            .build();

    when(languageRepository.findById(3)).thenReturn(Optional.of(language));
    when(configurationParameterRepository.findByName(SitmunConstants.LANGUAGE_DEFAULT_CONF_KEY))
        .thenReturn(Optional.of(parameter));

    service.setDefaultLanguage(3);

    assertThat(language.getDefaultLanguage()).isTrue();
    assertThat(parameter.getValue()).isEqualTo("ca");
    verify(languageRepository).clearAllDefaultFlags();
    verify(languageRepository).save(language);
    verify(configurationParameterRepository).save(parameter);
  }
}
