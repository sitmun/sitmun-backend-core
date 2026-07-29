package org.sitmun.administration.service.i18n;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sitmun.infrastructure.persistence.type.i18n.DatabaseDefaultLanguageResolver;
import org.sitmun.infrastructure.persistence.type.i18n.Language;
import org.sitmun.infrastructure.persistence.type.i18n.LiteralTranslation;
import org.sitmun.infrastructure.persistence.type.i18n.LiteralTranslationRepository;
import org.sitmun.infrastructure.persistence.type.i18n.LiteralTranslationValue;
import org.sitmun.infrastructure.persistence.type.i18n.LiteralTranslationValueRepository;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
@DisplayName("LiteralTranslationEnsureService race")
class LiteralTranslationEnsureServiceRaceTest {

  @Mock private LiteralTranslationRepository literalTranslationRepository;
  @Mock private LiteralTranslationValueRepository literalTranslationValueRepository;
  @Mock private DatabaseDefaultLanguageResolver databaseDefaultLanguageResolver;

  private LiteralTranslationEnsureService service;
  private Language english;

  @BeforeEach
  void setUp() {
    service =
        new LiteralTranslationEnsureService(
            literalTranslationRepository,
            literalTranslationValueRepository,
            databaseDefaultLanguageResolver);
    english = Language.builder().id(1).shortname("en").name("English").build();
  }

  @Test
  @DisplayName("unique-race on insert treats existing row as success")
  void raceTreatsExistingRowAsSuccess() {
    String key = "race-key";
    LiteralTranslation existing =
        LiteralTranslation.builder().id(42).literal(key).sourceLanguage(english).build();

    when(literalTranslationRepository.findByLiteral(key))
        .thenReturn(Optional.empty())
        .thenReturn(Optional.of(existing));
    when(literalTranslationRepository.save(any(LiteralTranslation.class)))
        .thenThrow(new DataIntegrityViolationException("unique constraint"));
    when(literalTranslationValueRepository.findByLiteralTranslationIdAndLanguageShortname(42, "en"))
        .thenReturn(Optional.of(new LiteralTranslationValue()));

    assertThatCode(() -> service.ensureLiteral(key, english)).doesNotThrowAnyException();

    verify(literalTranslationRepository, times(1)).save(any(LiteralTranslation.class));
    verify(literalTranslationRepository, times(2)).findByLiteral(key);
    verify(literalTranslationValueRepository, never()).save(any());
  }
}
