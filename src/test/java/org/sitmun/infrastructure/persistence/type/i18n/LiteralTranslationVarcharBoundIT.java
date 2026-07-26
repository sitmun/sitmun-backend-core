package org.sitmun.infrastructure.persistence.type.i18n;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sitmun.domain.PersistenceConstants.LONG_DESCRIPTION;

import jakarta.persistence.EntityManager;
import jakarta.validation.ConstraintViolationException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@DisplayName("LiteralTranslation VARCHAR(4000) bound (H2/Postgres/Oracle)")
class LiteralTranslationVarcharBoundIT {

  private static final String SHORT = "short literal";
  private static final String UNICODE = "Καλημέρα 你好 مرحبا 🌍";
  private static final String AT_LIMIT =
      IntStream.range(0, LONG_DESCRIPTION).mapToObj(i -> "a").collect(Collectors.joining());
  private static final String OVER_LIMIT = AT_LIMIT + "x";

  @Autowired private LiteralTranslationRepository literalTranslationRepository;
  @Autowired private LiteralTranslationValueRepository literalTranslationValueRepository;
  @Autowired private LanguageRepository languageRepository;
  @Autowired private EntityManager entityManager;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  @DisplayName("LTR_LITERAL / LTV_VALUE columns are bounded VARCHAR(4000), not CLOB/TEXT")
  void schemaColumnsAreBoundedVarchar() throws Exception {
    try (Connection connection = jdbcTemplate.getDataSource().getConnection()) {
      DatabaseMetaData meta = connection.getMetaData();
      assertBoundedVarchar(meta, "STM_LITERAL_TRANSLATION", "LTR_LITERAL");
      assertBoundedVarchar(meta, "STM_LITERAL_TRANSLATION_VALUE", "LTV_VALUE");
    }
  }

  @Test
  @DisplayName("persists short and unicode; findByLiteral uses plain equality")
  void roundTripsShortAndUnicodeAndFindsByLiteral() {
    Language source = languageRepository.findByShortname("ca").orElseThrow();
    Language target = languageRepository.findByShortname("es").orElseThrow();

    assertRoundTrip(SHORT, "valor corto", source, target);
    assertRoundTrip(UNICODE, UNICODE + " ES", source, target);

    LiteralTranslation found = literalTranslationRepository.findByLiteral(SHORT).orElseThrow();
    assertThat(found.getLiteral()).isEqualTo(SHORT);
  }

  @Test
  @DisplayName("length 4000 round-trips; length 4001 is rejected")
  void rejectsOverMaxLength() {
    Language source = languageRepository.findByShortname("ca").orElseThrow();
    Language target = languageRepository.findByShortname("es").orElseThrow();

    assertRoundTrip(AT_LIMIT, AT_LIMIT, source, target);

    assertThatThrownBy(
            () ->
                literalTranslationRepository.saveAndFlush(
                    LiteralTranslation.builder()
                        .literal(OVER_LIMIT)
                        .sourceLanguage(source)
                        .build()))
        .isInstanceOf(ConstraintViolationException.class);
    entityManager.clear();

    LiteralTranslation ok =
        literalTranslationRepository.saveAndFlush(
            LiteralTranslation.builder().literal("ok-key").sourceLanguage(source).build());
    assertThatThrownBy(
            () ->
                literalTranslationValueRepository.saveAndFlush(
                    LiteralTranslationValue.builder()
                        .literalTranslation(ok)
                        .language(target)
                        .value(OVER_LIMIT)
                        .build()))
        .isInstanceOf(ConstraintViolationException.class);
  }

  private void assertRoundTrip(
      String literalText, String valueText, Language source, Language target) {
    LiteralTranslation saved =
        literalTranslationRepository.save(
            LiteralTranslation.builder().literal(literalText).sourceLanguage(source).build());
    LiteralTranslationValue savedValue =
        literalTranslationValueRepository.save(
            LiteralTranslationValue.builder()
                .literalTranslation(saved)
                .language(target)
                .value(valueText)
                .build());

    entityManager.flush();
    entityManager.clear();

    assertThat(literalTranslationRepository.findById(saved.getId()).orElseThrow().getLiteral())
        .isEqualTo(literalText);
    assertThat(
            literalTranslationValueRepository.findById(savedValue.getId()).orElseThrow().getValue())
        .isEqualTo(valueText);
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT LTR_LITERAL FROM STM_LITERAL_TRANSLATION WHERE LTR_ID = ?",
                String.class,
                saved.getId()))
        .isEqualTo(literalText);
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT LTV_VALUE FROM STM_LITERAL_TRANSLATION_VALUE WHERE LTV_ID = ?",
                String.class,
                savedValue.getId()))
        .isEqualTo(valueText);
  }

  private static void assertBoundedVarchar(
      DatabaseMetaData meta, String tableName, String columnName) throws Exception {
    ColumnInfo info = findColumn(meta, tableName, columnName);
    assertThat(info).as("%s.%s must exist", tableName, columnName).isNotNull();
    String type = info.typeName.toUpperCase(Locale.ROOT);
    assertThat(type)
        .as("%s.%s type=%s", tableName, columnName, info.typeName)
        .containsAnyOf("VARCHAR", "CHARACTER VARYING", "NVARCHAR");
    assertThat(type).doesNotContain("CLOB").doesNotContain("TEXT");
    if (info.columnSize > 0) {
      assertThat(info.columnSize)
          .as("%s.%s size", tableName, columnName)
          .isEqualTo(LONG_DESCRIPTION);
    }
  }

  private static ColumnInfo findColumn(DatabaseMetaData meta, String tableName, String columnName)
      throws Exception {
    for (String table : new String[] {tableName, tableName.toLowerCase(Locale.ROOT)}) {
      try (ResultSet rs = meta.getColumns(null, null, table, null)) {
        while (rs.next()) {
          if (columnName.equalsIgnoreCase(rs.getString("COLUMN_NAME"))) {
            return new ColumnInfo(rs.getString("TYPE_NAME"), rs.getInt("COLUMN_SIZE"));
          }
        }
      }
    }
    return null;
  }

  private record ColumnInfo(String typeName, int columnSize) {}
}
