package org.sitmun.administration.service.i18n;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TemplateLiteralProcessorTest {

  @Test
  void processReplacesTranslatedLiteralAndPreservesOuterHtml() {
    TemplateLiteralProcessor processor =
        new TemplateLiteralProcessor(
            (literal, language) -> "es".equals(language) ? "Hola mundo!" : literal);

    assertThat(processor.process("<p><t>Hola món!</t></p>", "es")).isEqualTo("<p>Hola mundo!</p>");
  }

  @Test
  void processRemovesTagsAndKeepsOriginalWhenTranslationIsMissing() {
    TemplateLiteralProcessor processor =
        new TemplateLiteralProcessor((literal, language) -> literal);

    assertThat(processor.process("<p><t>Hola món!</t></p>", "es")).isEqualTo("<p>Hola món!</p>");
  }

  @Test
  void processSupportsMultilineAndReplacementSpecialCharacters() {
    TemplateLiteralProcessor processor =
        new TemplateLiteralProcessor((literal, language) -> "$1\\value");

    assertThat(processor.process("<div><t>Hola\n món!</t></div>", "es"))
        .isEqualTo("<div>$1\\value</div>");
  }
}
