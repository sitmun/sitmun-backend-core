package org.sitmun.domain.cartography;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.domain.role.Role;
import org.sitmun.domain.role.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

/**
 * Baseline JDBC statement count for {@link CartographyRepository#findByRolesAndTerritory} with lazy
 * loading (no {@code @EntityGraph}).
 *
 * <p>Post-PR-250: EntityGraph removed; {@code Cartography.service} and collections use
 * {@code @BatchSize} for on-demand batch loading. This test validates current fetch cost.
 *
 * <p>Uses the same role resolution as {@code AuthorizationService.buildProfile}: {@link
 * RoleRepository#findRolesByApplicationAndUserAndTerritory} for admin / app 1 / territory 1.
 *
 * <p><b>Measured (04_data, admin/app1/terr1):</b> 1 JDBC statement for {@code
 * findByRolesAndTerritory} query; touching {@code service} may add batched loads. Baseline {@code
 * 1}, ceiling {@code 2}.
 */
@SpringBootTest
@TestPropertySource(
    properties = {
      "spring.jpa.properties.hibernate.generate_statistics=true",
      "spring.jpa.open-in-view=false"
    })
@DisplayName("CartographyRepository.findByRolesAndTerritory statement count baseline")
class CartographyRepositoryFindByRolesAndTerritoryStatementCountTest {

  private static final String USERNAME = "admin";
  private static final int APP_ID = 1;
  private static final int TERRITORY_ID = 1;

  /** Measured baseline (H2 + 04_data fixtures): 1 statement (lazy loading, no EntityGraph). */
  private static final int BASELINE_STATEMENTS = 1;

  /** Ceiling with ~25% margin for fixture drift. */
  private static final int CEILING_STATEMENTS = 2;

  @Autowired private CartographyRepository cartographyRepository;
  @Autowired private RoleRepository roleRepository;
  @Autowired private EntityManagerFactory entityManagerFactory;

  @Test
  @Transactional(readOnly = true)
  @DisplayName("findByRolesAndTerritory baseline with lazy loading (post-EntityGraph removal)")
  void findByRolesAndTerritory_lazyLoadBaseline() {
    SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
    Statistics statistics = sessionFactory.getStatistics();
    statistics.setStatisticsEnabled(true);
    statistics.clear();

    List<Role> roles =
        roleRepository.findRolesByApplicationAndUserAndTerritory(USERNAME, APP_ID, TERRITORY_ID);
    assertThat(roles).isNotEmpty();

    statistics.clear();

    long beforeQuery = statistics.getPrepareStatementCount();
    List<Cartography> layers = cartographyRepository.findByRolesAndTerritory(roles, TERRITORY_ID);
    long afterQuery = statistics.getPrepareStatementCount();
    long queryStatements = afterQuery - beforeQuery;

    int servicesTouched = 0;
    long beforeTouchService = statistics.getPrepareStatementCount();
    for (Cartography layer : layers) {
      if (layer.getService() != null) {
        layer.getService().getId();
        servicesTouched++;
      }
    }
    long afterTouchService = statistics.getPrepareStatementCount();
    long touchServiceStatements = afterTouchService - beforeTouchService;

    long total = statistics.getPrepareStatementCount();

    String detailedBreakdown =
        String.format(
            """
            \nStatement breakdown (CartographyRepository.findByRolesAndTerritory):
              Total: %d
              - findByRolesAndTerritory query: %d
              - touch Cartography.service: %d (non-null services touched: %d)
              Cartographies returned: %d
            """,
            total, queryStatements, touchServiceStatements, servicesTouched, layers.size());

    assertThat(total)
        .as(
            "JDBC statement count isolated to repository + service touches (baseline: %d, ceiling: %d)%s",
            BASELINE_STATEMENTS, CEILING_STATEMENTS, detailedBreakdown)
        .isLessThanOrEqualTo(CEILING_STATEMENTS);

    assertThat(queryStatements + touchServiceStatements)
        .as("sum of phased counts should match total" + detailedBreakdown)
        .isEqualTo(total);
  }
}
