package org.sitmun.domain.tree;

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
 * JDBC statement budget for {@link TreeRepository#findByAppAndRoles} after removing {@code
 * EntityGraph}: {@code @BatchSize} on {@code availableRoles} and {@code availableApplications}.
 *
 * <p>Uses the same role resolution as {@code AuthorizationService.buildProfile}: {@link
 * RoleRepository#findRolesByApplicationAndUserAndTerritory} for admin / app 1 / territory 1.
 *
 * <p><b>Measured (04_data, admin/app1/terr1, 1 tree):</b> total {@code 3} — {@code 1} list query,
 * {@code 1} batched roles, {@code 1} batched applications. Baseline {@code 3}, ceiling {@code 4}.
 */
@SpringBootTest
@TestPropertySource(
    properties = {
      "spring.jpa.properties.hibernate.generate_statistics=true",
      "spring.jpa.open-in-view=false"
    })
@DisplayName("TreeRepository.findByAppAndRoles statement count baseline")
class TreeRepositoryFindByAppAndRolesStatementCountTest {

  private static final String USERNAME = "admin";
  private static final int APP_ID = 1;
  private static final int TERRITORY_ID = 1;

  private static final int BASELINE_STATEMENTS = 3;

  private static final int CEILING_STATEMENTS = 4;

  @Autowired private TreeRepository treeRepository;
  @Autowired private RoleRepository roleRepository;
  @Autowired private EntityManagerFactory entityManagerFactory;

  @Test
  @Transactional(readOnly = true)
  @DisplayName("findByAppAndRoles statement budget (BatchSize, no EntityGraph)")
  void findByAppAndRoles_batchSizeBaseline() {
    SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
    Statistics statistics = sessionFactory.getStatistics();
    statistics.setStatisticsEnabled(true);
    statistics.clear();

    List<Role> roles =
        roleRepository.findRolesByApplicationAndUserAndTerritory(USERNAME, APP_ID, TERRITORY_ID);
    assertThat(roles).isNotEmpty();

    statistics.clear();

    long beforeQuery = statistics.getPrepareStatementCount();
    List<Tree> trees = treeRepository.findByAppAndRoles(APP_ID, roles);
    long afterQuery = statistics.getPrepareStatementCount();
    long queryStatements = afterQuery - beforeQuery;

    int totalRoleRefs = 0;
    long beforeTouchRoles = statistics.getPrepareStatementCount();
    for (Tree t : trees) {
      totalRoleRefs += t.getAvailableRoles().size();
    }
    long afterTouchRoles = statistics.getPrepareStatementCount();
    long touchRolesStatements = afterTouchRoles - beforeTouchRoles;

    int totalAppRefs = 0;
    long beforeTouchApps = statistics.getPrepareStatementCount();
    for (Tree t : trees) {
      totalAppRefs += t.getAvailableApplications().size();
    }
    long afterTouchApps = statistics.getPrepareStatementCount();
    long touchAppsStatements = afterTouchApps - beforeTouchApps;

    long total = statistics.getPrepareStatementCount();

    String detailedBreakdown =
        String.format(
            """
            \nStatement breakdown (TreeRepository.findByAppAndRoles):
              Total: %d
              - findByAppAndRoles query: %d
              - touch Tree.availableRoles: %d (sum of collection sizes: %d)
              - touch Tree.availableApplications: %d (sum of collection sizes: %d)
              Trees returned: %d
            """,
            total,
            queryStatements,
            touchRolesStatements,
            totalRoleRefs,
            touchAppsStatements,
            totalAppRefs,
            trees.size());

    assertThat(total)
        .as(
            "JDBC statement count isolated to repository + association touches (baseline: %d, ceiling: %d)%s",
            BASELINE_STATEMENTS, CEILING_STATEMENTS, detailedBreakdown)
        .isLessThanOrEqualTo(CEILING_STATEMENTS);

    assertThat(queryStatements + touchRolesStatements + touchAppsStatements)
        .as("sum of phased counts should match total" + detailedBreakdown)
        .isEqualTo(total);
  }
}
