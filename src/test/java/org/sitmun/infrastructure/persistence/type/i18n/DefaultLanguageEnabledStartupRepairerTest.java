package org.sitmun.infrastructure.persistence.type.i18n;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sitmun.SitmunConstants;
import org.sitmun.domain.configuration.ConfigurationParameter;
import org.sitmun.domain.configuration.ConfigurationParameterRepository;
import org.springframework.boot.DefaultApplicationArguments;

@ExtendWith(MockitoExtension.class)
@DisplayName("DefaultLanguageEnabledStartupRepairer")
class DefaultLanguageEnabledStartupRepairerTest {

  @Mock private ConfigurationParameterRepository configurationParameterRepository;
  @Mock private LanguageRepository languageRepository;

  @InjectMocks private DefaultLanguageEnabledStartupRepairer repairer;

  @Test
  @DisplayName("Re-enables the database default language when disabled")
  void reenablesDisabledDefaultLanguage() {
    when(configurationParameterRepository.findByName(SitmunConstants.LANGUAGE_DEFAULT_CONF_KEY))
        .thenReturn(
            Optional.of(
                ConfigurationParameter.builder()
                    .name(SitmunConstants.LANGUAGE_DEFAULT_CONF_KEY)
                    .value("en")
                    .build()));
    Language disabled =
        Language.builder().id(1).shortname("en").name("English").enabled(false).build();
    when(languageRepository.findByShortname("en")).thenReturn(Optional.of(disabled));

    repairer.run(new DefaultApplicationArguments(new String[] {}));

    ArgumentCaptor<Language> captor = ArgumentCaptor.forClass(Language.class);
    verify(languageRepository).save(captor.capture());
    assertThat(captor.getValue().getEnabled()).isTrue();
  }

  @Test
  @DisplayName("Does nothing when the default language is already enabled")
  void leavesEnabledDefaultLanguageUntouched() {
    when(configurationParameterRepository.findByName(SitmunConstants.LANGUAGE_DEFAULT_CONF_KEY))
        .thenReturn(
            Optional.of(
                ConfigurationParameter.builder()
                    .name(SitmunConstants.LANGUAGE_DEFAULT_CONF_KEY)
                    .value("en")
                    .build()));
    when(languageRepository.findByShortname("en"))
        .thenReturn(
            Optional.of(
                Language.builder().id(1).shortname("en").name("English").enabled(true).build()));

    repairer.run(new DefaultApplicationArguments(new String[] {}));

    verify(languageRepository, never()).save(any());
  }

  @Test
  @DisplayName("Does nothing when language.default is missing")
  void skipsWhenDefaultParameterMissing() {
    when(configurationParameterRepository.findByName(SitmunConstants.LANGUAGE_DEFAULT_CONF_KEY))
        .thenReturn(Optional.empty());

    repairer.run(new DefaultApplicationArguments(new String[] {}));

    verify(languageRepository, never()).findByShortname(any());
    verify(languageRepository, never()).save(any());
  }
}
