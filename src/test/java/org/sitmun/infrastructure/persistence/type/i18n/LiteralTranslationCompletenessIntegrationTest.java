package org.sitmun.infrastructure.persistence.type.i18n;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.test.BaseTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@DisplayName("Literal translation completeness (enabled languages only)")
class LiteralTranslationCompletenessIntegrationTest extends BaseTest {

  @Autowired private LanguageRepository languageRepository;
  @Autowired private LiteralTranslationRepository literalTranslationRepository;
  @Autowired private LiteralTranslationValueRepository literalTranslationValueRepository;

  @Test
  @DisplayName("isComplete ignores disabled languages")
  void isCompleteIgnoresDisabledLanguages() {
    Language french = languageRepository.findByShortname("fr").orElseThrow();
    boolean wasEnabled = !Boolean.FALSE.equals(french.getEnabled());
    french.setEnabled(false);
    languageRepository.save(french);

    try {
      String key = "complete-disabled-" + System.nanoTime();
      Language english = languageRepository.findByShortname("en").orElseThrow();
      LiteralTranslation literal =
          literalTranslationRepository.save(
              LiteralTranslation.builder().literal(key).sourceLanguage(english).build());

      for (Language language : languageRepository.findAll()) {
        if (Boolean.FALSE.equals(language.getEnabled())) {
          continue;
        }
        LiteralTranslationValue value = new LiteralTranslationValue();
        value.setLiteralTranslation(literal);
        value.setLanguage(language);
        value.setValue(key + "-" + language.getShortname());
        literalTranslationValueRepository.save(value);
      }

      assertThat(literalTranslationRepository.isCompleteByLiteral(key)).isTrue();
    } finally {
      if (wasEnabled) {
        french.setEnabled(true);
        languageRepository.save(french);
      }
    }
  }
}
