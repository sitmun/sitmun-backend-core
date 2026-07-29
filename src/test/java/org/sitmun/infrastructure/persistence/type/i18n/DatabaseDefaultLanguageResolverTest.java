package org.sitmun.infrastructure.persistence.type.i18n;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sitmun.SitmunConstants;
import org.sitmun.domain.configuration.ConfigurationParameter;
import org.sitmun.domain.configuration.ConfigurationParameterRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("DatabaseDefaultLanguageResolver")
class DatabaseDefaultLanguageResolverTest {

  @Mock private ConfigurationParameterRepository configurationParameterRepository;
  @Mock private LanguageRepository languageRepository;

  private DatabaseDefaultLanguageResolver resolver;

  @BeforeEach
  void setUp() {
    resolver =
        new DatabaseDefaultLanguageResolver(
            configurationParameterRepository, languageRepository, "en");
  }

  @Test
  @DisplayName("uses language.default from configuration when present")
  void usesConfiguredDefault() {
    when(configurationParameterRepository.findByName(SitmunConstants.LANGUAGE_DEFAULT_CONF_KEY))
        .thenReturn(
            Optional.of(
                ConfigurationParameter.builder()
                    .name(SitmunConstants.LANGUAGE_DEFAULT_CONF_KEY)
                    .value("ca")
                    .build()));

    assertThat(resolver.resolveShortname()).isEqualTo("ca");
  }

  @Test
  @DisplayName("falls back to sitmun.language property when config missing")
  void fallsBackToProperty() {
    when(configurationParameterRepository.findByName(SitmunConstants.LANGUAGE_DEFAULT_CONF_KEY))
        .thenReturn(Optional.empty());

    assertThat(resolver.resolveShortname()).isEqualTo("en");
  }

  @Test
  @DisplayName("requireLanguage resolves STM_LANGUAGE row")
  void requireLanguage() {
    when(configurationParameterRepository.findByName(SitmunConstants.LANGUAGE_DEFAULT_CONF_KEY))
        .thenReturn(
            Optional.of(
                ConfigurationParameter.builder()
                    .name(SitmunConstants.LANGUAGE_DEFAULT_CONF_KEY)
                    .value("ca")
                    .build()));
    Language ca = Language.builder().id(3).shortname("ca").name("Català").build();
    when(languageRepository.findByShortname("ca")).thenReturn(Optional.of(ca));

    assertThat(resolver.requireLanguage()).isSameAs(ca);
  }

  @Test
  @DisplayName("requireLanguage fails when language row missing")
  void requireLanguageMissing() {
    when(configurationParameterRepository.findByName(SitmunConstants.LANGUAGE_DEFAULT_CONF_KEY))
        .thenReturn(Optional.empty());
    when(languageRepository.findByShortname("en")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> resolver.requireLanguage())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("en");
  }
}
