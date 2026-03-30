package org.sitmun.domain.role;

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
 * Baseline JDBC statement count for {@link RoleRepository#findById} with
 * {@code @BatchSize(size=50)} applied to all 5 collections.
 *
 * <p>Role has 4 ManyToMany + 1 OneToMany collections. With {@code @BatchSize}, each accessed
 * collection triggers a batched IN query instead of individual N+1 SELECTs when Spring Data REST
 * HAL serialization walks the entity graph.
 *
 * <p><b>Trade-off:</b> Without BatchSize, loading N roles with collections = 1+N queries per
 * collection. With BatchSize, loading N roles = 1+(N/50) queries per collection.
 *
 * <p><b>Measured (04_data, MIN role id):</b> total {@code 6} JDBC statements (1 base + 5 for
 * collections). Baseline {@code 6}, ceiling {@code 9}.
 */
@SpringBootTest
@TestPropertySource(
    properties = {
      "spring.jpa.properties.hibernate.generate_statistics=true",
      "spring.jpa.open-in-view=false"
    })
@DisplayName("RoleRepository.findById statement count baseline")
class RoleRepositoryFindByIdStatementCountTest {

  /** Measured (04_data, MIN id): 6 statements (1 base + 5 batch per collection). */
  private static final int BASELINE_STATEMENTS = 6;

  private static final int CEILING_STATEMENTS = 9;

  @Autowired private RoleRepository roleRepository;
  @Autowired private EntityManagerFactory entityManagerFactory;
  @Autowired private EntityManager entityManager;

  @Test
  @Transactional(readOnly = true)
  @DisplayName("findById baseline with BatchSize (post-migration)")
  void findById_preBatchSizeBaseline() {
    Integer roleId =
        entityManager.createQuery("SELECT MIN(r.id) FROM Role r", Integer.class).getSingleResult();
    assertThat(roleId).isNotNull();

    SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
    Statistics statistics = sessionFactory.getStatistics();
    statistics.setStatisticsEnabled(true);
    statistics.clear();

    long beforeQuery = statistics.getPrepareStatementCount();
    Optional<Role> opt = roleRepository.findById(roleId);
    long afterQuery = statistics.getPrepareStatementCount();
    long queryStatements = afterQuery - beforeQuery;
    assertThat(opt).isPresent();
    Role role = opt.get();

    long beforeTouch = statistics.getPrepareStatementCount();
    int apps = role.getApplications().size();
    int tasks = role.getTasks().size();
    int permissions = role.getPermissions().size();
    int trees = role.getTrees().size();
    int userConfigs = role.getUserConfigurations().size();
    long afterTouch = statistics.getPrepareStatementCount();
    long touchStatements = afterTouch - beforeTouch;

    long total = statistics.getPrepareStatementCount();

    String detailedBreakdown =
        String.format(
            """
            \nStatement breakdown (RoleRepository.findById):
              Total: %d
              - findById query: %d
              - touch 5 collections (BatchSize): %d
              Role id: %d (apps=%d, tasks=%d, permissions=%d, trees=%d, userConfigs=%d)
              With @BatchSize: prevents N+1 explosion at scale
            """,
            total,
            queryStatements,
            touchStatements,
            roleId,
            apps,
            tasks,
            permissions,
            trees,
            userConfigs);

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
