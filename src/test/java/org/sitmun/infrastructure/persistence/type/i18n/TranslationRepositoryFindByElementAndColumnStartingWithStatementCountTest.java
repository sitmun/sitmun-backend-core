package org.sitmun.infrastructure.persistence.type.i18n;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

/**
 * Baseline JDBC statement count for {@link
 * TranslationRepository#findByElementAndColumnStartingWith} with explicit {@code JOIN FETCH
 * tr.language}.
 *
 * <p>Single {@code ManyToOne} join fetch — low cartesian risk; replaced {@code @EntityGraph} to
 * maintain consistent fetch strategy across repository methods.
 *
 * <p><b>Measured (element 1, column prefix {@code Language}):</b> total {@code 1}; language is
 * fetched eagerly in the same query. Baseline {@code 1}, ceiling {@code 2}.
 */
@SpringBootTest
@TestPropertySource(
    properties = {
      "spring.jpa.properties.hibernate.generate_statistics=true",
      "spring.jpa.open-in-view=false"
    })
@DisplayName("TranslationRepository.findByElementAndColumnStartingWith statement count baseline")
class TranslationRepositoryFindByElementAndColumnStartingWithStatementCountTest {

  /** From {@code STM_TRANSLATION_ES.csv} (e.g. {@code Language.name} rows for element 1). */
  private static final int ELEMENT_ID = 1;

  private static final String COLUMN_PREFIX = "Language";

  /** Measured: 1 statement; 4 translation rows for element 1 / prefix Language. */
  private static final int BASELINE_STATEMENTS = 1;

  private static final int CEILING_STATEMENTS = 2;

  @Autowired private TranslationRepository translationRepository;
  @Autowired private EntityManagerFactory entityManagerFactory;

  @Test
  @Transactional(readOnly = true)
  @DisplayName("findByElementAndColumnStartingWith baseline with JOIN FETCH language")
  void findByElementAndColumnStartingWith_joinFetchBaseline() {
    SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
    Statistics statistics = sessionFactory.getStatistics();
    statistics.setStatisticsEnabled(true);
    statistics.clear();

    long beforeQuery = statistics.getPrepareStatementCount();
    List<Translation> list =
        translationRepository.findByElementAndColumnStartingWith(ELEMENT_ID, COLUMN_PREFIX);
    long afterQuery = statistics.getPrepareStatementCount();
    long queryStatements = afterQuery - beforeQuery;

    long beforeTouch = statistics.getPrepareStatementCount();
    for (Translation t : list) {
      if (t.getLanguage() != null) {
        t.getLanguage().getId();
      }
    }
    long afterTouch = statistics.getPrepareStatementCount();
    long touchStatements = afterTouch - beforeTouch;

    long total = statistics.getPrepareStatementCount();

    String detailedBreakdown =
        String.format(
            """
            \nStatement breakdown (TranslationRepository.findByElementAndColumnStartingWith):
              Total: %d
              - query: %d
              - touch language: %d (rows: %d)
              element=%d columnPrefix=%s
            """,
            total, queryStatements, touchStatements, list.size(), ELEMENT_ID, COLUMN_PREFIX);

    assertThat(list).isNotEmpty();

    assertThat(total)
        .as(
            "JDBC statement count (baseline: %d, ceiling: %d)%s",
            BASELINE_STATEMENTS, CEILING_STATEMENTS, detailedBreakdown)
        .isLessThanOrEqualTo(CEILING_STATEMENTS);

    assertThat(queryStatements + touchStatements)
        .as("sum of phased counts should match total" + detailedBreakdown)
        .isEqualTo(total);
  }
}
