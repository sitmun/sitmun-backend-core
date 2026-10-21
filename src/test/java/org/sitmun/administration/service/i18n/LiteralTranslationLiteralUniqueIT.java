package org.sitmun.administration.service.i18n;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.sitmun.infrastructure.persistence.type.i18n.Language;
import org.sitmun.infrastructure.persistence.type.i18n.LanguageRepository;
import org.sitmun.infrastructure.persistence.type.i18n.LiteralTranslation;
import org.sitmun.infrastructure.persistence.type.i18n.LiteralTranslationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@EnabledIfSystemProperty(named = "spring.profiles.active", matches = ".*\\bh2\\b.*")
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:literaluk;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;LOCK_TIMEOUT=30000",
      "spring.datasource.hikari.maximum-pool-size=4"
    })
@DisplayName("Literal translation literal uniqueness")
class LiteralTranslationLiteralUniqueIT {

  @Autowired private LiteralTranslationRepository literalTranslationRepository;
  @Autowired private LiteralTranslationEnsureService literalTranslationEnsureService;
  @Autowired private LanguageRepository languageRepository;
  @Autowired private PlatformTransactionManager transactionManager;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  @DisplayName("a second row with the same literal is rejected and lookup returns one")
  void duplicateLiteralIsRejectedAndLookupReturnsOne() {
    String key = "dup-literal-" + System.nanoTime();

    insertLiteral(key);

    assertThatThrownBy(() -> insertLiteral(key))
        .isInstanceOf(DataIntegrityViolationException.class);

    assertThat(literalTranslationRepository.findByLiteral(key)).isPresent();
    assertThat(countLiterals(key)).isEqualTo(1);
  }

  @Test
  @DisplayName("ensure adopts a literal committed by a concurrent insert")
  void ensureAdoptsConcurrentLiteral() throws Exception {
    String key = "race-literal-" + System.nanoTime();
    CountDownLatch inserted = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    AtomicReference<Exception> winnerFailure = new AtomicReference<>();
    TransactionTemplate tx = new TransactionTemplate(transactionManager);

    Thread winner =
        new Thread(
            () -> {
              try {
                tx.executeWithoutResult(
                    status -> {
                      Language source = languageRepository.findByShortname("en").orElseThrow();
                      literalTranslationRepository.saveAndFlush(
                          LiteralTranslation.builder().literal(key).sourceLanguage(source).build());
                      inserted.countDown();
                      awaitRelease(release);
                    });
              } catch (Exception ex) {
                winnerFailure.set(ex);
                inserted.countDown();
              }
            });
    winner.start();
    assertThat(inserted.await(20, TimeUnit.SECONDS)).isTrue();
    assertThat(winnerFailure.get()).isNull();

    ExecutorService pool = Executors.newSingleThreadExecutor();
    try {
      var ensure =
          pool.submit(
              () ->
                  literalTranslationEnsureService.ensureLiteralsFromTemplateHtml(
                      "<p><t>" + key + "</t></p>"));
      // Loser must reach the insert while the winner still holds the row.
      Thread.sleep(300);
      release.countDown();
      assertThatCode(() -> ensure.get(20, TimeUnit.SECONDS)).doesNotThrowAnyException();
    } finally {
      release.countDown();
      pool.shutdownNow();
      winner.join(5000);
    }

    assertThat(winnerFailure.get()).isNull();
    assertThat(countLiterals(key)).isEqualTo(1);
    assertThat(literalTranslationRepository.findByLiteral(key)).isPresent();
  }

  @Test
  @DisplayName("changeset dedupe keeps the lowest id and its conflicting translation")
  void dedupeKeepsLowestIdBeforeUniqueKey() {
    String key = "dedupe-literal-" + System.nanoTime();
    int en = languageId("en");
    int es = languageId("es");
    TransactionTemplate tx = new TransactionTemplate(transactionManager);
    try {
      tx.executeWithoutResult(
          status -> {
            jdbcTemplate.execute(
                "ALTER TABLE STM_LITERAL_TRANSLATION DROP CONSTRAINT STM_LTR_LITERAL_UK");
            jdbcTemplate.update(
                "INSERT INTO STM_LITERAL_TRANSLATION (LTR_ID, LTR_LITERAL, LTR_LANID) VALUES (910001, ?, ?)",
                key,
                en);
            jdbcTemplate.update(
                "INSERT INTO STM_LITERAL_TRANSLATION (LTR_ID, LTR_LITERAL, LTR_LANID) VALUES (910002, ?, ?)",
                key,
                en);
            jdbcTemplate.update(
                "INSERT INTO STM_LITERAL_TRANSLATION_VALUE (LTV_ID, LTV_LTRID, LTV_LANID, LTV_VALUE) VALUES (910011, 910001, ?, 'keep')",
                en);
            jdbcTemplate.update(
                "INSERT INTO STM_LITERAL_TRANSLATION_VALUE (LTV_ID, LTV_LTRID, LTV_LANID, LTV_VALUE) VALUES (910012, 910002, ?, 'drop')",
                en);
            jdbcTemplate.update(
                "INSERT INTO STM_LITERAL_TRANSLATION_VALUE (LTV_ID, LTV_LTRID, LTV_LANID, LTV_VALUE) VALUES (910013, 910002, ?, 'moved')",
                es);

            dedupeLiterals();
            jdbcTemplate.execute(
                "ALTER TABLE STM_LITERAL_TRANSLATION ADD CONSTRAINT STM_LTR_LITERAL_UK UNIQUE (LTR_LITERAL)");

            assertThat(countLiterals(key)).isEqualTo(1);
            assertThat(
                    jdbcTemplate.queryForObject(
                        "SELECT LTR_ID FROM STM_LITERAL_TRANSLATION WHERE LTR_LITERAL = ?",
                        Integer.class,
                        key))
                .isEqualTo(910001);
            assertThat(valueFor(910001, en)).isEqualTo("keep");
            assertThat(valueFor(910001, es)).isEqualTo("moved");
            assertThat(
                    jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM STM_LITERAL_TRANSLATION_VALUE WHERE LTV_LTRID = 910002",
                        Integer.class))
                .isZero();
          });
    } finally {
      tx.executeWithoutResult(
          status -> {
            jdbcTemplate.update("DELETE FROM STM_LITERAL_TRANSLATION WHERE LTR_LITERAL = ?", key);
            if (!literalUniqueExists()) {
              jdbcTemplate.execute(
                  "ALTER TABLE STM_LITERAL_TRANSLATION ADD CONSTRAINT STM_LTR_LITERAL_UK UNIQUE (LTR_LITERAL)");
            }
          });
    }
  }

  private static void awaitRelease(CountDownLatch release) {
    try {
      if (!release.await(20, TimeUnit.SECONDS)) {
        throw new IllegalStateException("winner release timed out");
      }
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("winner interrupted", ex);
    }
  }

  private void insertLiteral(String key) {
    new TransactionTemplate(transactionManager)
        .executeWithoutResult(
            status -> {
              Language source = languageRepository.findByShortname("en").orElseThrow();
              literalTranslationRepository.saveAndFlush(
                  LiteralTranslation.builder().literal(key).sourceLanguage(source).build());
            });
  }

  private void dedupeLiterals() {
    jdbcTemplate.execute(
        """
        DELETE FROM STM_LITERAL_TRANSLATION_VALUE
        WHERE EXISTS (
          SELECT 1
          FROM STM_LITERAL_TRANSLATION d
          INNER JOIN STM_LITERAL_TRANSLATION k
            ON k.LTR_LITERAL = d.LTR_LITERAL AND k.LTR_ID < d.LTR_ID
          INNER JOIN STM_LITERAL_TRANSLATION_VALUE kv
            ON kv.LTV_LTRID = k.LTR_ID
           AND kv.LTV_LANID = STM_LITERAL_TRANSLATION_VALUE.LTV_LANID
          WHERE d.LTR_ID = STM_LITERAL_TRANSLATION_VALUE.LTV_LTRID
        )
        """);
    jdbcTemplate.execute(
        """
        UPDATE STM_LITERAL_TRANSLATION_VALUE
        SET LTV_LTRID = (
          SELECT MIN(k.LTR_ID)
          FROM STM_LITERAL_TRANSLATION d
          INNER JOIN STM_LITERAL_TRANSLATION k
            ON k.LTR_LITERAL = d.LTR_LITERAL
          WHERE d.LTR_ID = STM_LITERAL_TRANSLATION_VALUE.LTV_LTRID
        )
        WHERE EXISTS (
          SELECT 1
          FROM STM_LITERAL_TRANSLATION d
          INNER JOIN STM_LITERAL_TRANSLATION k
            ON k.LTR_LITERAL = d.LTR_LITERAL AND k.LTR_ID < d.LTR_ID
          WHERE d.LTR_ID = STM_LITERAL_TRANSLATION_VALUE.LTV_LTRID
        )
        """);
    jdbcTemplate.execute(
        """
        DELETE FROM STM_LITERAL_TRANSLATION
        WHERE EXISTS (
          SELECT 1
          FROM STM_LITERAL_TRANSLATION k
          WHERE k.LTR_LITERAL = STM_LITERAL_TRANSLATION.LTR_LITERAL
            AND k.LTR_ID < STM_LITERAL_TRANSLATION.LTR_ID
        )
        """);
  }

  private int languageId(String shortname) {
    Integer id =
        jdbcTemplate.queryForObject(
            "SELECT LAN_ID FROM STM_LANGUAGE WHERE LAN_SHORTNAME = ?", Integer.class, shortname);
    assertThat(id).isNotNull();
    return id;
  }

  private String valueFor(int literalId, int languageId) {
    return jdbcTemplate.queryForObject(
        "SELECT LTV_VALUE FROM STM_LITERAL_TRANSLATION_VALUE WHERE LTV_LTRID = ? AND LTV_LANID = ?",
        String.class,
        literalId,
        languageId);
  }

  private boolean literalUniqueExists() {
    Integer count =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
            WHERE CONSTRAINT_NAME = 'STM_LTR_LITERAL_UK'
            """,
            Integer.class);
    return count != null && count > 0;
  }

  private int countLiterals(String key) {
    Integer count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM STM_LITERAL_TRANSLATION WHERE LTR_LITERAL = ?",
            Integer.class,
            key);
    return count == null ? 0 : count;
  }
}
