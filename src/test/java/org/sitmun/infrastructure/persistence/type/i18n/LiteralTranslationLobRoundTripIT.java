package org.sitmun.infrastructure.persistence.type.i18n;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
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
@DisplayName("LiteralTranslation LOB round-trip (H2/Postgres/Oracle)")
class LiteralTranslationLobRoundTripIT {

  private static final String SHORT = "short literal";
  private static final String UNICODE = "Καλημέρα 你好 مرحبا 🌍";
  private static final String HTML_LIKE = "<div class=\"x\">Sense &amp; dades</div>";
  private static final String OVER_4000 =
      IntStream.range(0, 4100).mapToObj(i -> "あ").collect(Collectors.joining());

  @Autowired private LiteralTranslationRepository literalTranslationRepository;
  @Autowired private LiteralTranslationValueRepository literalTranslationValueRepository;
  @Autowired private LanguageRepository languageRepository;
  @Autowired private EntityManager entityManager;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  @DisplayName("persists and reloads LTR_LITERAL / LTV_VALUE for short, >4000, unicode, HTML-like")
  void roundTripsLiteralAndValueThroughJpaAndJdbc() {
    Language source = languageRepository.findByShortname("ca").orElseThrow();
    Language target = languageRepository.findByShortname("es").orElseThrow();

    assertRoundTrip(SHORT, "valor corto", source, target);
    assertRoundTrip(OVER_4000, OVER_4000 + "-es", source, target);
    assertRoundTrip(UNICODE, UNICODE + " ES", source, target);
    assertRoundTrip(HTML_LIKE, HTML_LIKE.replace("Sense", "Sin"), source, target);
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

    LiteralTranslation reloaded =
        literalTranslationRepository.findById(saved.getId()).orElseThrow();
    LiteralTranslationValue reloadedValue =
        literalTranslationValueRepository.findById(savedValue.getId()).orElseThrow();

    assertThat(reloaded.getLiteral()).isEqualTo(literalText);
    assertThat(reloadedValue.getValue()).isEqualTo(valueText);
    assertNotOidLike(reloaded.getLiteral());
    assertNotOidLike(reloadedValue.getValue());

    String jdbcLiteral =
        jdbcTemplate.queryForObject(
            "SELECT LTR_LITERAL FROM STM_LITERAL_TRANSLATION WHERE LTR_ID = ?",
            String.class,
            saved.getId());
    String jdbcValue =
        jdbcTemplate.queryForObject(
            "SELECT LTV_VALUE FROM STM_LITERAL_TRANSLATION_VALUE WHERE LTV_ID = ?",
            String.class,
            savedValue.getId());
    assertThat(jdbcLiteral).isEqualTo(literalText);
    assertThat(jdbcValue).isEqualTo(valueText);
    assertNotOidLike(jdbcLiteral);
    assertNotOidLike(jdbcValue);

    String updatedLiteral = literalText + "-upd";
    String updatedValue = valueText + "-upd";
    reloaded.setLiteral(updatedLiteral);
    reloadedValue.setValue(updatedValue);
    literalTranslationRepository.save(reloaded);
    literalTranslationValueRepository.save(reloadedValue);
    entityManager.flush();
    entityManager.clear();

    assertThat(literalTranslationRepository.findById(saved.getId()).orElseThrow().getLiteral())
        .isEqualTo(updatedLiteral);
    assertThat(
            literalTranslationValueRepository.findById(savedValue.getId()).orElseThrow().getValue())
        .isEqualTo(updatedValue);
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT LTR_LITERAL FROM STM_LITERAL_TRANSLATION WHERE LTR_ID = ?",
                String.class,
                saved.getId()))
        .isEqualTo(updatedLiteral);
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT LTV_VALUE FROM STM_LITERAL_TRANSLATION_VALUE WHERE LTV_ID = ?",
                String.class,
                savedValue.getId()))
        .isEqualTo(updatedValue);
  }

  private static void assertNotOidLike(String value) {
    assertThat(value).isNotNull();
    assertThat(value).doesNotMatch("^\\d+$");
  }
}
