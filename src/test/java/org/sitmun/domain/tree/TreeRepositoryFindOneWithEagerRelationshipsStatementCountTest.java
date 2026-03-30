package org.sitmun.domain.tree;

import static org.assertj.core.api.Assertions.assertThat;

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
 * Baseline JDBC statement count for {@link TreeRepository#findOneWithEagerRelationships} — {@code
 * JOIN FETCH} of {@code allNodes}.
 *
 * <p>Large trees can produce wide result sets; measure before switching to batch or partial fetch.
 *
 * <p><b>Measured (tree id 1):</b> total {@code 6} JDBC statements in the fetch path; {@code
 * allNodes} touch adds {@code 0}. Baseline {@code 6}, ceiling {@code 8}.
 */
@SpringBootTest
@TestPropertySource(
    properties = {
      "spring.jpa.properties.hibernate.generate_statistics=true",
      "spring.jpa.open-in-view=false"
    })
@DisplayName("TreeRepository.findOneWithEagerRelationships statement count baseline")
class TreeRepositoryFindOneWithEagerRelationshipsStatementCountTest {

  /** Matches fixture usage in {@code TreeControllerTest}. */
  private static final int TREE_ID = 1;

  /** Measured (tree id 1, 04_data): 6 JDBC statements for join fetch path; touches add 0. */
  private static final int BASELINE_STATEMENTS = 6;

  private static final int CEILING_STATEMENTS = 8;

  @Autowired private TreeRepository treeRepository;
  @Autowired private EntityManagerFactory entityManagerFactory;

  @Test
  @Transactional(readOnly = true)
  @DisplayName("findOneWithEagerRelationships baseline (join fetch allNodes)")
  void findOneWithEagerRelationships_baseline() {
    SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
    Statistics statistics = sessionFactory.getStatistics();
    statistics.setStatisticsEnabled(true);
    statistics.clear();

    long beforeQuery = statistics.getPrepareStatementCount();
    Tree tree = treeRepository.findOneWithEagerRelationships(TREE_ID);
    long afterQuery = statistics.getPrepareStatementCount();
    long queryStatements = afterQuery - beforeQuery;
    assertThat(tree).isNotNull();

    long beforeTouch = statistics.getPrepareStatementCount();
    int nodes = tree.getAllNodes().size();
    long afterTouch = statistics.getPrepareStatementCount();
    long touchStatements = afterTouch - beforeTouch;

    long total = statistics.getPrepareStatementCount();

    String detailedBreakdown =
        String.format(
            """
            \nStatement breakdown (TreeRepository.findOneWithEagerRelationships):
              Total: %d
              - query: %d
              - touch allNodes.size(): %d (node count: %d)
              Tree id: %d
            """,
            total, queryStatements, touchStatements, nodes, TREE_ID);

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
