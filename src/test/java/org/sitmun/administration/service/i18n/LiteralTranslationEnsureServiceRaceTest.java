package org.sitmun.administration.service.i18n;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import java.util.Optional;
import org.hibernate.Session;
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
  @Mock private EntityManager entityManager;
  @Mock private Session session;

  private LiteralTranslationEnsureService service;
  private Language english;

  @BeforeEach
  void setUp() {
    when(entityManager.unwrap(Session.class)).thenReturn(session);
    service =
        new LiteralTranslationEnsureService(
            literalTranslationRepository,
            literalTranslationValueRepository,
            databaseDefaultLanguageResolver,
            entityManager);
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
    doThrow(new DataIntegrityViolationException("unique constraint")).when(session).doWork(any());
    when(literalTranslationValueRepository.findByLiteralTranslationIdAndLanguageShortname(42, "en"))
        .thenReturn(Optional.of(new LiteralTranslationValue()));

    assertThatCode(() -> service.ensureLiteral(key, english)).doesNotThrowAnyException();

    verify(session, times(1)).doWork(any());
    verify(literalTranslationRepository, times(2)).findByLiteral(key);
    verify(literalTranslationValueRepository, never()).save(any());
  }
}
