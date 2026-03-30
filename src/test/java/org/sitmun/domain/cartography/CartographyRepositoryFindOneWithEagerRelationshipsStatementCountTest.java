package org.sitmun.domain.cartography;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

/**
 * Baseline JDBC statement count for {@link CartographyRepository#findOneWithEagerRelationships} —
 * custom {@code JOIN FETCH} on {@code service} only (not the same as {@link
 * CartographyRepository#findById}'s EntityGraph).
 *
 * <p><b>Measured (MIN cartography id):</b> total {@code 2} ({@code 1} fetch query + {@code 1} when
 * touching lazy {@code styles}). Baseline {@code 2}, ceiling {@code 3}.
 */
@SpringBootTest
@TestPropertySource(
    properties = {
      "spring.jpa.properties.hibernate.generate_statistics=true",
      "spring.jpa.open-in-view=false"
    })
@DisplayName("CartographyRepository.findOneWithEagerRelationships statement count baseline")
class CartographyRepositoryFindOneWithEagerRelationshipsStatementCountTest {

  /** Measured (MIN id): 1 query + 1 for lazy styles batch after join fetch service. */
  private static final int BASELINE_STATEMENTS = 2;

  private static final int CEILING_STATEMENTS = 3;

  @Autowired private CartographyRepository cartographyRepository;
  @Autowired private EntityManagerFactory entityManagerFactory;
  @Autowired private EntityManager entityManager;

  @Test
  @Transactional(readOnly = true)
  @DisplayName("findOneWithEagerRelationships baseline (join fetch service)")
  void findOneWithEagerRelationships_baseline() {
    Integer cartographyId =
        entityManager
            .createQuery("SELECT MIN(c.id) FROM Cartography c", Integer.class)
            .getSingleResult();
    assertThat(cartographyId).isNotNull();

    SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
    Statistics statistics = sessionFactory.getStatistics();
    statistics.setStatisticsEnabled(true);
    statistics.clear();

    long beforeQuery = statistics.getPrepareStatementCount();
    Cartography c = cartographyRepository.findOneWithEagerRelationships(cartographyId);
    long afterQuery = statistics.getPrepareStatementCount();
    long queryStatements = afterQuery - beforeQuery;
    assertThat(c).isNotNull();

    long beforeTouch = statistics.getPrepareStatementCount();
    if (c.getService() != null) {
      c.getService().getId();
    }
    int styles = c.getStyles().size();
    long afterTouch = statistics.getPrepareStatementCount();
    long touchStatements = afterTouch - beforeTouch;

    long total = statistics.getPrepareStatementCount();

    String detailedBreakdown =
        String.format(
            """
            \nStatement breakdown (CartographyRepository.findOneWithEagerRelationships):
              Total: %d
              - query: %d
              - touch service + styles.size(): %d (styles count: %d)
              Cartography id: %d
            """,
            total, queryStatements, touchStatements, styles, cartographyId);

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
