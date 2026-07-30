package org.sitmun.administration.service.i18n;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("LiteralTranslationEnsureService key extraction")
class LiteralTranslationEnsureServiceTest {

  @Test
  @DisplayName("extracts exact TipTap nested inner HTML keys")
  void extractsNestedInnerHtmlKeys() {
    Set<String> keys =
        LiteralTranslationEnsureService.extractLiteralKeys(
            "<p><t><strong>Hola</strong></t> and <t>plain</t></p>");
    assertThat(keys).containsExactly("<strong>Hola</strong>", "plain");
  }

  @Test
  @DisplayName("returns empty set when no tags")
  void emptyWhenNoTags() {
    assertThat(LiteralTranslationEnsureService.extractLiteralKeys("<p>no marks</p>")).isEmpty();
  }
}
