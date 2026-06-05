package org.sitmun.infrastructure.persistence.type.i18n;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("TranslationService unit tests")
class TranslationServiceTest {

  @Mock private TranslationRepository translationRepository;
  @Mock private EntityManager entityManager;
  @InjectMocks private TranslationService service;

  private Locale previousLocale;

  /** Minimal entity used to exercise translation field replacement. */
  static class TranslatableEntity {
    @Id Integer id;
    String name;
    String description;

    TranslatableEntity(Integer id, String name, String description) {
      this.id = id;
      this.name = name;
      this.description = description;
    }
  }

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(service, "defaultLanguage", "es");
    previousLocale = LocaleContextHolder.getLocale();
  }

  @AfterEach
  void restoreLocale() {
    LocaleContextHolder.setLocale(previousLocale);
  }

  @Test
  @DisplayName("source text unchanged when no translation rows exist for selected language")
  void sourceTextUnchangedWhenNoTranslations() {
    LocaleContextHolder.setLocale(Locale.forLanguageTag("ca"));
    var entity = new TranslatableEntity(1, "Original Name", "Original Desc");
    when(translationRepository.findTranslation(1, "TranslatableEntity", "ca"))
        .thenReturn(Collections.emptyList());

    service.updateInternationalization(entity);

    assertThat(entity.name).isEqualTo("Original Name");
    assertThat(entity.description).isEqualTo("Original Desc");
  }

  @Test
  @DisplayName("translated fields replace source text when translation rows exist")
  void translatedFieldsReplaceSourceText() {
    LocaleContextHolder.setLocale(Locale.forLanguageTag("ca"));
    var entity = new TranslatableEntity(1, "Original Name", "Original Desc");
    var nameTr =
        Translation.builder()
            .id(10)
            .element(1)
            .column("TranslatableEntity.name")
            .translation("Nom en catala")
            .build();
    when(translationRepository.findTranslation(1, "TranslatableEntity", "ca"))
        .thenReturn(List.of(nameTr));

    service.updateInternationalization(entity);

    assertThat(entity.name).isEqualTo("Nom en catala");
    assertThat(entity.description).isEqualTo("Original Desc");
  }

  @Test
  @DisplayName("translation skipped when locale equals defaultLanguage (es)")
  void translationSkippedForDefaultLanguage() {
    LocaleContextHolder.setLocale(Locale.forLanguageTag("es"));
    var entity = new TranslatableEntity(1, "Nombre Original", "Descripcion");

    service.updateInternationalization(entity);

    verify(translationRepository, never()).findTranslation(any(), any(), any());
    assertThat(entity.name).isEqualTo("Nombre Original");
  }

  @Test
  @DisplayName("full oc-aranes tag used for translation lookup, not just oc")
  void ocAranesFullTagUsedForLookup() {
    LocaleContextHolder.setLocale(Locale.forLanguageTag("oc-aranes"));
    var entity = new TranslatableEntity(1, "Nom original", "Descripcion original");
    var nameTr =
        Translation.builder()
            .id(20)
            .element(1)
            .column("TranslatableEntity.name")
            .translation("Nom en aranés")
            .build();
    when(translationRepository.findTranslation(1, "TranslatableEntity", "oc-aranes"))
        .thenReturn(List.of(nameTr));

    service.updateInternationalization(entity);

    assertThat(entity.name).isEqualTo("Nom en aranés");
    verify(translationRepository).findTranslation(1, "TranslatableEntity", "oc-aranes");
  }
}
