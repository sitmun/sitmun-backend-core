package org.sitmun.administration.service.i18n;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.sitmun.infrastructure.persistence.type.i18n.LiteralTranslation;
import org.sitmun.infrastructure.persistence.type.i18n.LiteralTranslationRepository;
import org.sitmun.infrastructure.persistence.type.i18n.LiteralTranslationValueRepository;

class LiteralTranslationServiceTest {

  @Test
  void resolveReturnsTranslationForRequestedLanguage() {
    LiteralTranslationRepository literalRepository = mock(LiteralTranslationRepository.class);
    LiteralTranslationValueRepository valueRepository =
        mock(LiteralTranslationValueRepository.class);
    LiteralTranslationService service =
        new LiteralTranslationService(literalRepository, valueRepository);
    String literal = "Hola món!";
    LiteralTranslation literalTranslation =
        LiteralTranslation.builder().id(1).literal(literal).build();

    when(literalRepository.findByLiteral(literal)).thenReturn(Optional.of(literalTranslation));
    when(valueRepository.findValueByLiteralIdAndLanguage(1, "es"))
        .thenReturn(Optional.of("Hola mundo!"));

    assertThat(service.resolve(literal, "es")).isEqualTo("Hola mundo!");
  }

  @Test
  void resolveReturnsOriginalLiteralWhenTranslationIsMissing() {
    LiteralTranslationRepository literalRepository = mock(LiteralTranslationRepository.class);
    LiteralTranslationValueRepository valueRepository =
        mock(LiteralTranslationValueRepository.class);
    LiteralTranslationService service =
        new LiteralTranslationService(literalRepository, valueRepository);
    String literal = "Hola món!";

    when(literalRepository.findByLiteral(literal)).thenReturn(Optional.empty());

    assertThat(service.resolve(literal, "es")).isEqualTo(literal);
  }
}
