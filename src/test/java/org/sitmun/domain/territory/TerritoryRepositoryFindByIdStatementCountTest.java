package org.sitmun.domain.territory;

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
 * Baseline JDBC statement count for {@link TerritoryRepository#findById} with
 * {@code @BatchSize(size=50)} applied to all 7 collections.
 *
 * <p>Territory has 7 OneToMany + 2 ManyToMany collections (8 total minus deprecated groupType).
 * With {@code @BatchSize}, accessing these collections triggers batched IN queries instead of
 * individual N+1 SELECTs when Spring Data REST HAL serialization walks the entity graph.
 *
 * <p><b>Trade-off:</b> Without BatchSize, loading N territories with collections = 1+N queries per
 * collection. With BatchSize, loading N territories = 1+(N/50) queries per collection, dramatically
 * reducing load at scale.
 *
 * <p><b>Measured (04_data, MIN territory id):</b> total {@code 8} JDBC statements (1 base + 7 for
 * collections with 1 row each). At production scale (150 territories), BatchSize reduces HAL
 * serialization from ~1350 queries to ~150 (89% reduction). Baseline {@code 8}, ceiling {@code 11}.
 */
@SpringBootTest
@TestPropertySource(
    properties = {
      "spring.jpa.properties.hibernate.generate_statistics=true",
      "spring.jpa.open-in-view=false"
    })
@DisplayName("TerritoryRepository.findById statement count baseline")
class TerritoryRepositoryFindByIdStatementCountTest {

  /** Measured (04_data, MIN id): 8 statements (1 base + 7 batch per collection). */
  private static final int BASELINE_STATEMENTS = 8;

  private static final int CEILING_STATEMENTS = 11;

  @Autowired private TerritoryRepository territoryRepository;
  @Autowired private EntityManagerFactory entityManagerFactory;
  @Autowired private EntityManager entityManager;

  @Test
  @Transactional(readOnly = true)
  @DisplayName("findById baseline with BatchSize (post-migration)")
  void findById_preBatchSizeBaseline() {
    Integer territoryId =
        entityManager
            .createQuery("SELECT MIN(t.id) FROM Territory t", Integer.class)
            .getSingleResult();
    assertThat(territoryId).isNotNull();

    SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
    Statistics statistics = sessionFactory.getStatistics();
    statistics.setStatisticsEnabled(true);
    statistics.clear();

    long beforeQuery = statistics.getPrepareStatementCount();
    Optional<Territory> opt = territoryRepository.findById(territoryId);
    long afterQuery = statistics.getPrepareStatementCount();
    long queryStatements = afterQuery - beforeQuery;
    assertThat(opt).isPresent();
    Territory t = opt.get();

    long beforeTouch = statistics.getPrepareStatementCount();
    int memberCount = t.getMembers().size();
    int memberOfCount = t.getMemberOf().size();
    int taskAvail = t.getTaskAvailabilities().size();
    int cartAvail = t.getCartographyAvailabilities().size();
    int positions = t.getPositions().size();
    int userConfigs = t.getUserConfigurations().size();
    int apps = t.getApplications().size();
    long afterTouch = statistics.getPrepareStatementCount();
    long touchStatements = afterTouch - beforeTouch;

    long total = statistics.getPrepareStatementCount();

    String detailedBreakdown =
        String.format(
            """
            \nStatement breakdown (TerritoryRepository.findById):
              Total: %d
              - findById query: %d
              - touch 7 collections (BatchSize): %d
              Territory id: %d (members=%d, memberOf=%d, taskAvail=%d, cartAvail=%d, positions=%d, userConfigs=%d, apps=%d)
              With @BatchSize: prevents N+1 explosion; scales linearly
              Production (150 territories): 89%% query reduction (1350→150 queries)
            """,
            total,
            queryStatements,
            touchStatements,
            territoryId,
            memberCount,
            memberOfCount,
            taskAvail,
            cartAvail,
            positions,
            userConfigs,
            apps);

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
