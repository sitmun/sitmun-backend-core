package org.sitmun.administration.service.i18n;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.administration.controller.dto.LiteralTranslationCsvExportRequestDto;
import org.sitmun.administration.controller.dto.LiteralTranslationCsvImportErrorDto;
import org.sitmun.administration.controller.dto.LiteralTranslationCsvImportResponseDto;
import org.sitmun.infrastructure.persistence.type.i18n.Language;
import org.sitmun.infrastructure.persistence.type.i18n.LanguageRepository;
import org.sitmun.infrastructure.persistence.type.i18n.LiteralTranslation;
import org.sitmun.infrastructure.persistence.type.i18n.LiteralTranslationRepository;
import org.sitmun.infrastructure.persistence.type.i18n.LiteralTranslationValueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@Transactional
@DisplayName("LiteralTranslationCsvService integration tests")
class LiteralTranslationCsvServiceTest {

  @Autowired private LiteralTranslationCsvService service;
  @Autowired private LanguageRepository languageRepository;
  @Autowired private LiteralTranslationRepository literalTranslationRepository;
  @Autowired private LiteralTranslationValueRepository literalTranslationValueRepository;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  @DisplayName("exportCsv writes BOM, header and source language")
  void exportCsvWritesBomHeaderAndSourceLanguage() {
    Language ca = languageRepository.findByShortname("ca").orElseThrow();
    Language es = languageRepository.findByShortname("es").orElseThrow();

    insertLiteralTranslation(900001, "Hola món!", ca.getId());
    insertLiteralTranslationValue(900001, 900001, es.getId(), "Hola mundo!");

    LiteralTranslationCsvExportRequestDto request = new LiteralTranslationCsvExportRequestDto();
    request.setTargetLanguage("es");

    byte[] bytes = service.exportCsv(request);
    String csv = new String(bytes, StandardCharsets.UTF_8);

    assertThat(csv).startsWith("\uFEFF");
    assertThat(csv).contains("\"source_language\",\"literal\",\"translation\"");
    assertThat(csv).contains("\"ca\",\"Hola món!\",\"Hola mundo!\"");
  }

  @Test
  @DisplayName("importCsv upserts rows and reports incidents")
  void importCsvUpsertsRowsAndReportsIncidents() throws Exception {
    Language ca = languageRepository.findByShortname("ca").orElseThrow();
    Language es = languageRepository.findByShortname("es").orElseThrow();

    insertLiteralTranslation(900010, "Actualizar", ca.getId());
    insertLiteralTranslationValue(900010, 900010, es.getId(), "Viejo");

    insertLiteralTranslation(900011, "Vacio", ca.getId());
    insertLiteralTranslationValue(900011, 900011, es.getId(), "Algo");

    insertLiteralTranslation(900012, "No informado", ca.getId());
    setSequenceValue("LTR_ID", 900013);
    setSequenceValue("LTV_ID", 900012);

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
    assertThat(response.getExistingKeysNotInCsv()).isEqualTo(1);
    assertThat(response.getEmptyValueRows()).isEqualTo(2);
    assertThat(response.getFailedRows()).isEqualTo(0);
    assertThat(response.getErrors()).isEmpty();
    assertThat(response.getSourceLanguages()).containsExactly("en", "ca", "es");

    assertThat(literalTranslationValueRepository.findValueByLiteralIdAndLanguage(900010, "es"))
        .contains("Nuevo");
    assertThat(literalTranslationValueRepository.findValueByLiteralIdAndLanguage(900011, "es"))
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
  @DisplayName("importCsv imports valid rows and reports invalid ones")
  void importCsvImportsValidRowsAndReportsInvalidOnes() throws Exception {
    languageRepository.findByShortname("ca").orElseThrow();

    setSequenceValue("LTR_ID", 900030);
    setSequenceValue("LTV_ID", 900030);

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
  @DisplayName("importCsv creates source language value for new literals")
  void importCsvCreatesSourceLanguageValueForNewLiterals() throws Exception {
    setSequenceValue("LTR_ID", 900020);
    setSequenceValue("LTV_ID", 900020);

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
  @DisplayName("importCsv rejects semicolon separated files")
  void importCsvRejectsSemicolonSeparatedFiles() throws Exception {
    setSequenceValue("LTR_ID", 900040);
    setSequenceValue("LTV_ID", 900040);

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
  @DisplayName("importCsv rejects invalid header")
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

  private void insertLiteralTranslation(int id, String literal, int sourceLanguageId) {
    jdbcTemplate.update(
        "INSERT INTO STM_LITERAL_TRANSLATION (LTR_ID, LTR_LITERAL, LTR_LANID) VALUES (?, ?, ?)",
        id,
        literal,
        sourceLanguageId);
  }

  private void insertLiteralTranslationValue(int id, int literalId, int languageId, String value) {
    jdbcTemplate.update(
        "INSERT INTO STM_LITERAL_TRANSLATION_VALUE (LTV_ID, LTV_LTRID, LTV_LANID, LTV_VALUE) VALUES (?, ?, ?, ?)",
        id,
        literalId,
        languageId,
        value);
  }

  private void setSequenceValue(String sequenceName, int nextValue) {
    jdbcTemplate.update(
        "UPDATE STM_SEQUENCE SET SEQ_COUNT = ? WHERE SEQ_NAME = ?", nextValue, sequenceName);
  }
}
