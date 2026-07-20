package org.sitmun.administration.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sitmun.domain.configuration.ConfigurationParameterRepository;
import org.sitmun.infrastructure.persistence.type.i18n.Language;
import org.sitmun.infrastructure.persistence.type.i18n.LanguageRepository;
import org.sitmun.infrastructure.persistence.type.i18n.TranslationRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("DefaultLanguageChangeService validation")
class DefaultLanguageChangeServiceTest {

  @Mock private LanguageRepository languageRepository;
  @Mock private TranslationRepository translationRepository;
  @Mock private ConfigurationParameterRepository configurationParameterRepository;
  @Mock private DataSource dataSource;

  @InjectMocks private DefaultLanguageChangeService service;

  @Test
  @DisplayName("Rejects disabled target language")
  void rejectsDisabledTargetLanguage() {
    when(languageRepository.findByShortname("en"))
        .thenReturn(
            Optional.of(
                Language.builder().id(1).shortname("en").name("English").enabled(true).build()));
    when(languageRepository.findByShortname("ca"))
        .thenReturn(
            Optional.of(
                Language.builder().id(3).shortname("ca").name("Català").enabled(false).build()));

    assertThatThrownBy(() -> service.preview("en", "ca"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("disabled")
        .hasMessageContaining("ca");
  }
}
