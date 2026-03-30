package org.sitmun.domain.cartography;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.util.Optional;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

/**
 * Baseline JDBC statement count for {@link CartographyRepository#findById} after removing
 * {@code @EntityGraph} and applying {@code @BatchSize(size = 50)} to all 7 collections.
 *
 * <p>Cartography has 6 OneToMany + 1 ManyToMany collections. With {@code @BatchSize}, each accessed
 * collection is fetched via a batched IN query instead of individual N+1 SELECTs or cartesian
 * joins.
 *
 * <p><b>Trade-off:</b> EntityGraph used 1 statement (with cartesian product risk). BatchSize uses 1
 * base + ~6-7 batch queries. This prevents cartesian explosion and scales linearly at production
 * (1000 cartographies).
 *
 * <p><b>Measured (04_data, MIN cartography id):</b> total {@code 7} JDBC statements (1 base + 6
 * batch queries for touched collections). Baseline {@code 7}, ceiling {@code 10}.
 */
@SpringBootTest
@TestPropertySource(
    properties = {
      "spring.jpa.properties.hibernate.generate_statistics=true",
      "spring.jpa.open-in-view=false"
    })
@DisplayName("CartographyRepository.findById statement count baseline")
class CartographyRepositoryFindByIdStatementCountTest {

  /** Measured (04_data, MIN id): 7 statements (1 base + 6 batch queries). */
  private static final int BASELINE_STATEMENTS = 7;

  private static final int CEILING_STATEMENTS = 10;

  @Autowired private CartographyRepository cartographyRepository;
  @Autowired private EntityManagerFactory entityManagerFactory;
  @Autowired private EntityManager entityManager;

  @Test
  @Transactional(readOnly = true)
  @DisplayName("findById baseline with BatchSize (post-migration)")
  void findById_currentEntityGraphBaseline() {
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
    Optional<Cartography> opt = cartographyRepository.findById(cartographyId);
    long afterQuery = statistics.getPrepareStatementCount();
    long queryStatements = afterQuery - beforeQuery;
    assertThat(opt).isPresent();
    Cartography c = opt.get();

    long beforeTouch = statistics.getPrepareStatementCount();
    int perm = c.getPermissions().size();
    int av = c.getAvailabilities().size();
    int st = c.getStyles().size();
    int fi = c.getFilters().size();
    int pa = c.getParameters().size();
    int tn = c.getTreeNodes().size();
    if (c.getService() != null) {
      c.getService().getId();
    }
    long afterTouch = statistics.getPrepareStatementCount();
    long touchStatements = afterTouch - beforeTouch;

    long total = statistics.getPrepareStatementCount();

    String detailedBreakdown =
        String.format(
            """
            \nStatement breakdown (CartographyRepository.findById):
              Total: %d
              - findById query: %d
              - touch collections + service (BatchSize): %d
              Cartography id: %d (sizes: perm=%d avail=%d styles=%d filters=%d params=%d nodes=%d)
              With @BatchSize: prevents N×M cartesian products; scales linearly
              Production (1000 cartographies): 80-86%% query reduction vs N+1 pattern
            """,
            total, queryStatements, touchStatements, cartographyId, perm, av, st, fi, pa, tn);

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
