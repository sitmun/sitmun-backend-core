package org.sitmun.domain.task;

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
 * JDBC statement budget for {@link TaskRepository#findByRolesAndTerritory} after removing
 * {@code @EntityGraph}: {@code @BatchSize} on {@code Task.roles} and on {@code TaskUI} / {@code
 * TaskType} entities for {@code ManyToOne} batch loading.
 *
 * <p>Uses the same role resolution as {@code AuthorizationService.buildProfile}: {@link
 * RoleRepository#findRolesByApplicationAndUserAndTerritory} for admin / app 1 / territory 1.
 *
 * <p><b>Measured (04_data, admin/app1/terr1, 7 tasks):</b> total {@code 7} JDBC statements — {@code
 * 6} for the main query path (incl. batch loads for {@code ui}/{@code type}), {@code 1} for batched
 * {@code roles}. Baseline {@code 7}, ceiling {@code 9}.
 */
@SpringBootTest
@TestPropertySource(
    properties = {
      "spring.jpa.properties.hibernate.generate_statistics=true",
      "spring.jpa.open-in-view=false"
    })
@DisplayName("TaskRepository.findByRolesAndTerritory statement count baseline")
class TaskRepositoryFindByRolesAndTerritoryStatementCountTest {

  private static final String USERNAME = "admin";
  private static final int APP_ID = 1;
  private static final int TERRITORY_ID = 1;

  private static final int BASELINE_STATEMENTS = 7;

  private static final int CEILING_STATEMENTS = 9;

  @Autowired private TaskRepository taskRepository;
  @Autowired private RoleRepository roleRepository;
  @Autowired private EntityManagerFactory entityManagerFactory;

  @Test
  @Transactional(readOnly = true)
  @DisplayName("findByRolesAndTerritory statement budget (BatchSize, no EntityGraph)")
  void findByRolesAndTerritory_batchSizeBaseline() {
    SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
    Statistics statistics = sessionFactory.getStatistics();
    statistics.setStatisticsEnabled(true);
    statistics.clear();

    List<Role> roles =
        roleRepository.findRolesByApplicationAndUserAndTerritory(USERNAME, APP_ID, TERRITORY_ID);
    assertThat(roles).isNotEmpty();

    statistics.clear();

    long beforeQuery = statistics.getPrepareStatementCount();
    List<Task> tasks = taskRepository.findByRolesAndTerritory(roles, TERRITORY_ID);
    long afterQuery = statistics.getPrepareStatementCount();
    long queryStatements = afterQuery - beforeQuery;

    int totalRoleRefs = 0;
    long beforeTouchRoles = statistics.getPrepareStatementCount();
    for (Task t : tasks) {
      totalRoleRefs += t.getRoles().size();
    }
    long afterTouchRoles = statistics.getPrepareStatementCount();
    long touchRolesStatements = afterTouchRoles - beforeTouchRoles;

    long beforeTouchUi = statistics.getPrepareStatementCount();
    for (Task t : tasks) {
      if (t.getUi() != null) {
        t.getUi().getId();
      }
    }
    long afterTouchUi = statistics.getPrepareStatementCount();
    long touchUiStatements = afterTouchUi - beforeTouchUi;

    long beforeTouchType = statistics.getPrepareStatementCount();
    for (Task t : tasks) {
      if (t.getType() != null) {
        t.getType().getId();
      }
    }
    long afterTouchType = statistics.getPrepareStatementCount();
    long touchTypeStatements = afterTouchType - beforeTouchType;

    long total = statistics.getPrepareStatementCount();

    String detailedBreakdown =
        String.format(
            """
            \nStatement breakdown (TaskRepository.findByRolesAndTerritory):
              Total: %d
              - findByRolesAndTerritory query: %d
              - touch Task.roles: %d (sum of collection sizes: %d)
              - touch Task.ui: %d
              - touch Task.type: %d
              Tasks returned: %d
            """,
            total,
            queryStatements,
            touchRolesStatements,
            totalRoleRefs,
            touchUiStatements,
            touchTypeStatements,
            tasks.size());

    assertThat(total)
        .as(
            "JDBC statement count isolated to repository + association touches (baseline: %d, ceiling: %d)%s",
            BASELINE_STATEMENTS, CEILING_STATEMENTS, detailedBreakdown)
        .isLessThanOrEqualTo(CEILING_STATEMENTS);

    assertThat(queryStatements + touchRolesStatements + touchUiStatements + touchTypeStatements)
        .as("sum of phased counts should match total" + detailedBreakdown)
        .isEqualTo(total);
  }
}
