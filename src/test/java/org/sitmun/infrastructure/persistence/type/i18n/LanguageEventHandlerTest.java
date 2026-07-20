package org.sitmun.infrastructure.persistence.type.i18n;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import org.sitmun.infrastructure.persistence.exception.RequirementException;

@ExtendWith(MockitoExtension.class)
@DisplayName("LanguageEventHandler")
class LanguageEventHandlerTest {

  @Mock private ConfigurationParameterRepository configurationParameterRepository;

  @InjectMocks private LanguageEventHandler handler;

  @Test
  @DisplayName("Cannot disable the database default language")
  void cannotDisableDefaultLanguage() {
    when(configurationParameterRepository.findByName(SitmunConstants.LANGUAGE_DEFAULT_CONF_KEY))
        .thenReturn(
            Optional.of(
                ConfigurationParameter.builder()
                    .name(SitmunConstants.LANGUAGE_DEFAULT_CONF_KEY)
                    .value("en")
                    .build()));

    Language language =
        Language.builder().id(1).shortname("en").name("English").enabled(false).build();

    assertThatThrownBy(() -> handler.handleLanguagePersist(language))
        .isInstanceOf(RequirementException.class)
        .hasMessageContaining("default language");
  }

  @Test
  @DisplayName("Can disable a non-default language")
  void canDisableNonDefaultLanguage() {
    when(configurationParameterRepository.findByName(SitmunConstants.LANGUAGE_DEFAULT_CONF_KEY))
        .thenReturn(
            Optional.of(
                ConfigurationParameter.builder()
                    .name(SitmunConstants.LANGUAGE_DEFAULT_CONF_KEY)
                    .value("en")
                    .build()));

    Language language =
        Language.builder().id(2).shortname("es").name("Castellano").enabled(false).build();

    assertThatCode(() -> handler.handleLanguagePersist(language)).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("Can enable the database default language")
  void canEnableDefaultLanguage() {
    Language language =
        Language.builder().id(1).shortname("en").name("English").enabled(true).build();

    assertThatCode(() -> handler.handleLanguagePersist(language)).doesNotThrowAnyException();
  }
}
