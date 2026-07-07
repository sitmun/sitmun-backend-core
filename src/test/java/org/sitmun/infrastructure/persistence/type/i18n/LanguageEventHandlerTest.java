package org.sitmun.infrastructure.persistence.type.i18n;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class LanguageEventHandlerTest {

  private final LanguageEventHandler handler = new LanguageEventHandler();

  @Test
  void shouldAllowUpdatingLanguageWithoutChangingCode() {
    var language = Language.builder().id(1).shortname("en").name("English").build();
    language.postLoad();
    language.setName("English updated");

    assertThatCode(() -> handler.handleLanguageUpdate(language)).doesNotThrowAnyException();
  }

  @Test
  void shouldRejectLanguageCodeChanges() {
    var language = Language.builder().id(1).shortname("en").name("English").build();
    language.postLoad();
    language.setShortname("eng");

    assertThatThrownBy(() -> handler.handleLanguageUpdate(language))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Language code cannot be changed after creation");
  }
}
