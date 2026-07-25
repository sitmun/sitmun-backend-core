package org.sitmun.administration.service.i18n;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sitmun.domain.PersistenceConstants.LONG_DESCRIPTION;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.sitmun.administration.controller.dto.LiteralTranslationCsvExportRequestDto;
import org.sitmun.administration.controller.dto.LiteralTranslationCsvImportErrorDto;
import org.sitmun.administration.controller.dto.LiteralTranslationCsvImportResponseDto;
import org.sitmun.infrastructure.persistence.type.i18n.Language;
import org.sitmun.infrastructure.persistence.type.i18n.LanguageRepository;
import org.sitmun.infrastructure.persistence.type.i18n.LiteralTranslation;
import org.sitmun.infrastructure.persistence.type.i18n.LiteralTranslationRepository;
import org.sitmun.infrastructure.persistence.type.i18n.LiteralTranslationValue;
import org.sitmun.infrastructure.persistence.type.i18n.LiteralTranslationValueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@Transactional
class LiteralTranslationCsvServiceTest {

  @Autowired private LiteralTranslationCsvService service;
  @Autowired private LanguageRepository languageRepository;
  @Autowired private LiteralTranslationRepository literalTranslationRepository;
  @Autowired private LiteralTranslationValueRepository literalTranslationValueRepository;

  @Test
  void exportCsvWritesBomHeaderAndSourceLanguage() {
    Language ca = languageRepository.findByShortname("ca").orElseThrow();
    Language es = languageRepository.findByShortname("es").orElseThrow();

    LiteralTranslation literal = saveLiteral("Hola món!", ca);
    saveValue(literal, es, "Hola mundo!");

    LiteralTranslationCsvExportRequestDto request = new LiteralTranslationCsvExportRequestDto();
    request.setTargetLanguage("es");

    byte[] bytes = service.exportCsv(request);
    String csv = new String(bytes, StandardCharsets.UTF_8);

    assertThat(csv).startsWith("\uFEFF");
    assertThat(csv).contains("\"source_language\",\"literal\",\"translation\"");
    assertThat(csv).contains("\"ca\",\"Hola món!\",\"Hola mundo!\"");
  }

  @Test
  void importCsvUpsertsRowsAndReportsIncidents() throws Exception {
    Language ca = languageRepository.findByShortname("ca").orElseThrow();
    Language es = languageRepository.findByShortname("es").orElseThrow();

    LiteralTranslation actualizar = saveLiteral("Actualizar", ca);
    saveValue(actualizar, es, "Viejo");

    LiteralTranslation vacio = saveLiteral("Vacio", ca);
    saveValue(vacio, es, "Algo");

    saveLiteral("No informado", ca);

    long existingLiteralsBefore = literalTranslationRepository.count();
    // CSV matches two pre-existing keys (Actualizar, Vacio); the rest stay unmatched.
    long existingKeysMatchedInCsv = 2;

    String csv =
        "source_language,literal,translation\r\n"
            + "\"en\",\"Actualizar\",\"Nuevo\"\r\n"
            + "\"ca\",\"Vacio\",\"\"\r\n"
            + "\"es\",\"Nuevo literal\",\"Traducción nueva\"\r\n"
            + "\"es\",\"Sin traducción\",\"\"\r\n";
    MockMultipartFile file =
        new MockMultipartFile(
            "file", "literal-translations.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

    LiteralTranslationCsvImportResponseDto response = service.importCsv("es", file);

    assertThat(response.getTargetLanguage()).isEqualTo("es");
    assertThat(response.getTotalRows()).isEqualTo(4);
    assertThat(response.getCreatedLiterals()).isEqualTo(2);
    assertThat(response.getCreatedTranslations()).isEqualTo(1);
    assertThat(response.getUpdatedTranslations()).isEqualTo(1);
    assertThat(response.getEmptiedTranslations()).isEqualTo(1);
    assertThat(response.getUnchangedRows()).isEqualTo(1);
    assertThat(response.getExistingKeysNotInCsv())
        .isEqualTo(Math.max(0, existingLiteralsBefore - existingKeysMatchedInCsv));
    assertThat(response.getEmptyValueRows()).isEqualTo(2);
    assertThat(response.getFailedRows()).isEqualTo(0);
    assertThat(response.getErrors()).isEmpty();
    assertThat(response.getSourceLanguages()).containsExactly("en", "ca", "es");

    assertThat(
            literalTranslationValueRepository.findValueByLiteralIdAndLanguage(
                actualizar.getId(), "es"))
        .contains("Nuevo");
    assertThat(
            literalTranslationValueRepository.findValueByLiteralIdAndLanguage(vacio.getId(), "es"))
        .isEmpty();

    LiteralTranslation nuevo =
        literalTranslationRepository.findByLiteral("Nuevo literal").orElseThrow();
    assertThat(nuevo.getSourceLanguage().getShortname()).isEqualTo("es");
    assertThat(
            literalTranslationValueRepository.findValueByLiteralIdAndLanguage(nuevo.getId(), "es"))
        .contains("Traducción nueva");

    LiteralTranslation sinTraduccion =
        literalTranslationRepository.findByLiteral("Sin traducción").orElseThrow();
    assertThat(sinTraduccion.getSourceLanguage().getShortname()).isEqualTo("es");
    assertThat(
            literalTranslationValueRepository.findValueByLiteralIdAndLanguage(
                sinTraduccion.getId(), "es"))
        .isEmpty();
  }

  @Test
  void importCsvImportsValidRowsAndReportsInvalidOnes() throws Exception {
    languageRepository.findByShortname("ca").orElseThrow();

    String csv =
        "source_language,literal,translation\r\n"
            + "\"ca\",\"Literal valido 1\",\"Traduccion 1\"\r\n"
            + "\"xx\",\"Idioma inexistente\",\"Valor\"\r\n"
            + "\"ca\",\"\",\"Sin literal\"\r\n"
            + "\"ca\",\"Duplicado\",\"Uno\"\r\n"
            + "\"ca\",\"Duplicado\",\"Dos\"\r\n"
            + "\"ca\",\"Literal valido 2\",\"Traduccion 2\"\r\n";
    MockMultipartFile file =
        new MockMultipartFile(
            "file", "literal-translations.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

    LiteralTranslationCsvImportResponseDto response = service.importCsv("es", file);

    assertThat(response.getTotalRows()).isEqualTo(6);
    assertThat(response.getCreatedLiterals()).isEqualTo(3);
    assertThat(response.getCreatedTranslations()).isEqualTo(3);
    assertThat(response.getFailedRows()).isEqualTo(3);
    assertThat(response.getErrors()).hasSize(3);
    assertThat(response.getErrors())
        .extracting(LiteralTranslationCsvImportErrorDto::message)
        .contains(
            "One or more source languages do not exist",
            "entity.literalTranslation.error.literal_required",
            "entity.literalTranslation.error.duplicate_literal");

    assertThat(literalTranslationRepository.findByLiteral("Literal valido 1")).isPresent();
    assertThat(literalTranslationRepository.findByLiteral("Literal valido 2")).isPresent();
    assertThat(literalTranslationRepository.findByLiteral("Duplicado")).isPresent();
    assertThat(literalTranslationRepository.findByLiteral("Idioma inexistente")).isEmpty();
  }

  @Test
  void importCsvCreatesSourceLanguageValueForNewLiterals() throws Exception {
    String csv =
        "source_language,literal,translation\r\n" + "\"ca\",\"Literal nou\",\"Literal nuevo\"\r\n";
    MockMultipartFile file =
        new MockMultipartFile(
            "file", "literal-translations.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

    LiteralTranslationCsvImportResponseDto response = service.importCsv("es", file);

    assertThat(response.getCreatedLiterals()).isEqualTo(1);
    assertThat(response.getCreatedTranslations()).isEqualTo(1);

    LiteralTranslation literal =
        literalTranslationRepository.findByLiteral("Literal nou").orElseThrow();
    assertThat(literal.getSourceLanguage().getShortname()).isEqualTo("ca");
    assertThat(
            literalTranslationValueRepository.findValueByLiteralIdAndLanguage(
                literal.getId(), "ca"))
        .contains("Literal nou");
    assertThat(
            literalTranslationValueRepository.findValueByLiteralIdAndLanguage(
                literal.getId(), "es"))
        .contains("Literal nuevo");
  }

  @Test
  void importCsvRejectsOversizeLiteralAndTranslation() throws Exception {
    String tooLong = "x".repeat(LONG_DESCRIPTION + 1);
    String csv =
        "source_language,literal,translation\r\n"
            + "\"ca\",\""
            + tooLong
            + "\",\"ok\"\r\n"
            + "\"ca\",\"ok-literal\",\""
            + tooLong
            + "\"\r\n"
            + "\"ca\",\"Literal curt\",\"Traducció curta\"\r\n";
    MockMultipartFile file =
        new MockMultipartFile(
            "file", "literal-translations.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

    LiteralTranslationCsvImportResponseDto response = service.importCsv("es", file);

    assertThat(response.getFailedRows()).isEqualTo(2);
    assertThat(response.getCreatedLiterals()).isEqualTo(1);
    assertThat(response.getErrors())
        .extracting(LiteralTranslationCsvImportErrorDto::message)
        .contains(
            "entity.literalTranslation.error.literal_too_long",
            "entity.literalTranslation.error.translation_too_long");
    assertThat(literalTranslationRepository.findByLiteral("Literal curt")).isPresent();
  }

  @Test
  void importCsvRejectsSemicolonSeparatedFiles() throws Exception {
    String csv =
        "source_language;literal;translation\r\n"
            + "ca;Literal amb punt i coma;Literal con punto y coma\r\n";
    MockMultipartFile file =
        new MockMultipartFile(
            "file", "literal-translations.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

    assertThatThrownBy(() -> service.importCsv("es", file))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Invalid CSV header");
  }

  @Test
  void importCsvRejectsInvalidHeader() throws Exception {
    languageRepository.findByShortname("es").orElseThrow();

    MockMultipartFile file =
        new MockMultipartFile(
            "file",
            "literal-translations.csv",
            "text/csv",
            "wrong,header,columns\r\n\"ca\",\"test\",\"value\"\r\n"
                .getBytes(StandardCharsets.UTF_8));

    assertThatThrownBy(() -> service.importCsv("es", file))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Invalid CSV header");
  }

  private LiteralTranslation saveLiteral(String literal, Language sourceLanguage) {
    return literalTranslationRepository.saveAndFlush(
        LiteralTranslation.builder().literal(literal).sourceLanguage(sourceLanguage).build());
  }

  private void saveValue(LiteralTranslation literal, Language language, String value) {
    literalTranslationValueRepository.saveAndFlush(
        LiteralTranslationValue.builder()
            .literalTranslation(literal)
            .language(language)
            .value(value)
            .build());
  }
}
