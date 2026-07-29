package org.sitmun.administration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sitmun.domain.configuration.ConfigurationParameterRepository;
import org.sitmun.infrastructure.persistence.type.i18n.Language;
import org.sitmun.infrastructure.persistence.type.i18n.LanguageRepository;
import org.sitmun.infrastructure.persistence.type.i18n.LiteralTranslation;
import org.sitmun.infrastructure.persistence.type.i18n.LiteralTranslationRepository;
import org.sitmun.infrastructure.persistence.type.i18n.LiteralTranslationValue;
import org.sitmun.infrastructure.persistence.type.i18n.LiteralTranslationValueRepository;
import org.sitmun.infrastructure.persistence.type.i18n.TranslationRepository;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("DefaultLanguageChangeService validation")
class DefaultLanguageChangeServiceTest {

  @Mock private LanguageRepository languageRepository;
  @Mock private TranslationRepository translationRepository;
  @Mock private ConfigurationParameterRepository configurationParameterRepository;
  @Mock private LiteralTranslationRepository literalTranslationRepository;
  @Mock private LiteralTranslationValueRepository literalTranslationValueRepository;
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

  @Test
  @DisplayName("Seeds missing literal value for new default without changing sourceLanguage")
  void seedsLiteralContinuityWithoutMutatingSourceLanguage() {
    Language en = Language.builder().id(1).shortname("en").name("English").enabled(true).build();
    Language ca = Language.builder().id(3).shortname("ca").name("Català").enabled(true).build();
    LiteralTranslation literal =
        LiteralTranslation.builder().id(10).literal("Hello").sourceLanguage(en).build();

    when(literalTranslationRepository.findAllWithSourceLanguageOrderByLiteral())
        .thenReturn(List.of(literal));
    when(literalTranslationValueRepository.findByLiteralTranslationIdAndLanguageShortname(10, "ca"))
        .thenReturn(Optional.empty());
    when(literalTranslationValueRepository.findValueByLiteralIdAndLanguage(10, "en"))
        .thenReturn(Optional.of("Hello"));
    when(literalTranslationValueRepository.save(any(LiteralTranslationValue.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    int seeded =
        (int) ReflectionTestUtils.invokeMethod(service, "seedLiteralContinuityValues", en, ca);

    assertThat(seeded).isEqualTo(1);
    ArgumentCaptor<LiteralTranslationValue> valueCaptor =
        ArgumentCaptor.forClass(LiteralTranslationValue.class);
    verify(literalTranslationValueRepository).save(valueCaptor.capture());
    LiteralTranslationValue row = valueCaptor.getValue();
    assertThat(row.getValue()).isEqualTo("Hello");
    assertThat(row.getLanguage().getShortname()).isEqualTo("ca");
    assertThat(literal.getSourceLanguage().getShortname()).isEqualTo("en");
    verify(literalTranslationRepository, never()).save(any());
  }

  @Test
  @DisplayName("Does not overwrite an existing literal value for the new default")
  void doesNotOverwriteExistingTargetLiteralValue() {
    Language en = Language.builder().id(1).shortname("en").name("English").enabled(true).build();
    Language ca = Language.builder().id(3).shortname("ca").name("Català").enabled(true).build();
    LiteralTranslation literal =
        LiteralTranslation.builder().id(11).literal("Hello").sourceLanguage(en).build();
    LiteralTranslationValue existingCa = new LiteralTranslationValue();
    existingCa.setValue("already Catalan");

    when(literalTranslationRepository.findAllWithSourceLanguageOrderByLiteral())
        .thenReturn(List.of(literal));
    when(literalTranslationValueRepository.findByLiteralTranslationIdAndLanguageShortname(11, "ca"))
        .thenReturn(Optional.of(existingCa));

    int seeded =
        (int) ReflectionTestUtils.invokeMethod(service, "seedLiteralContinuityValues", en, ca);

    assertThat(seeded).isZero();
    verify(literalTranslationValueRepository, never()).save(any());
  }
}
