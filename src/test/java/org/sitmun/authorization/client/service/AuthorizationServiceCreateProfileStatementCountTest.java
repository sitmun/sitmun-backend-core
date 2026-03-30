package org.sitmun.authorization.client.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sitmun.authorization.client.service.ProfileContext.NodeSectionBehaviour.VIRTUAL_ROOT_ALL_NODES;

import jakarta.persistence.EntityManagerFactory;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

/**
 * Optional statistics test to guard against N+1 query regressions in {@link
 * AuthorizationService#createProfile}.
 *
 * <p><b>Related PR:</b> <a
 * href="https://github.com/sitmun/sitmun-backend-core/pull/250">sitmun/sitmun-backend-core#250</a>
 * - Improve app config request loading times
 *
 * <p>Calibrated on the test dataset under {@code src/test/resources/db/changelog/04_data/}.
 * Includes CartographyPermission {@code @BatchSize} (post-PR-250) and Task/Tree repository paths
 * without {@code EntityGraph} (batched {@code roles}/{@code ui}/{@code type} and tree collections).
 *
 * <p><b>Measured (admin / app 1 / terr 1):</b> total {@code 21} JDBC statements — {@code 20} for
 * {@code createProfile} repository work, {@code 1} when touching CartographyPermission.roles after
 * the service method returns. Baseline {@code 21}, ceiling {@code 27} (~25% margin).
 *
 * <p>To re-calibrate: run this test once, observe the statement count, document the value, and set
 * the ceiling slightly above that number.
 *
 * <p><b>Related tests:</b> See {@link AuthorizationServiceCreateProfileTransactionBehaviorTest} for
 * transaction boundary behavior tests (Test A & B).
 *
 * <p><b>Lazy-load detection:</b> Runs with lazy-load detection in WARN mode (test profile default)
 * to track lazy initialization patterns.
 */
@Slf4j
@SpringBootTest
@TestPropertySource(
    properties = {
      "spring.jpa.properties.hibernate.generate_statistics=true",
      "spring.jpa.open-in-view=false"
    })
@DisplayName("AuthorizationService.createProfile statement count guard (optional)")
class AuthorizationServiceCreateProfileStatementCountTest {

  @Autowired private AuthorizationService authorizationService;
  @Autowired private EntityManagerFactory entityManagerFactory;

  @Test
  @Transactional(readOnly = true)
  @DisplayName("createProfile stays within JDBC statement budget (guards N+1 regressions)")
  void createProfile_statementCountWithinBaseline() {
    SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
    Statistics statistics = sessionFactory.getStatistics();
    statistics.setStatisticsEnabled(true);
    statistics.clear();

    ProfileContext context =
        ProfileContext.builder()
            .username("admin")
            .appId(1)
            .territoryId(1)
            .nodeSectionBehaviour(VIRTUAL_ROOT_ALL_NODES)
            .nodeId(null)
            .build();

    long statementsBeforeProfile = statistics.getPrepareStatementCount();
    log.debug("=== Statement Count Analysis ===");
    log.debug("Statements before createProfile: {}", statementsBeforeProfile);

    Optional<Profile> profileOpt = authorizationService.createProfile(context);
    assertThat(profileOpt).isPresent();

    long statementsAfterProfile = statistics.getPrepareStatementCount();
    log.debug("Statements after createProfile: {}", statementsAfterProfile);
    log.debug(
        "Statements for createProfile: {}", (statementsAfterProfile - statementsBeforeProfile));

    Profile profile = profileOpt.get();

    // Touch lazy associations to force initialization
    log.debug("--- Touching lazy associations ---");

    long beforeService = statistics.getPrepareStatementCount();
    profile.getLayers().get(0).getService();
    long afterService = statistics.getPrepareStatementCount();
    log.debug("Statements for Cartography.service: {}", (afterService - beforeService));

    long beforeMembers = statistics.getPrepareStatementCount();
    int memberCount = profile.getGroups().iterator().next().getMembers().size();
    long afterMembers = statistics.getPrepareStatementCount();
    log.debug(
        "Statements for CartographyPermission.members (loaded {} members): {}",
        memberCount,
        (afterMembers - beforeMembers));

    long beforeRoles = statistics.getPrepareStatementCount();
    int roleCount = profile.getGroups().iterator().next().getRoles().size();
    long afterRoles = statistics.getPrepareStatementCount();
    log.debug(
        "Statements for CartographyPermission.roles (loaded {} roles): {}",
        roleCount,
        (afterRoles - beforeRoles));

    long statements = statistics.getPrepareStatementCount();
    log.debug("=== Total Statements: {} ===", statements);
    log.debug("Breakdown:");
    log.debug(
        "  - Base queries (createProfile): {}", (statementsAfterProfile - statementsBeforeProfile));
    log.debug("  - Cartography.service: {}", (afterService - beforeService));
    log.debug("  - CartographyPermission.members batch: {}", (afterMembers - beforeMembers));
    log.debug("  - CartographyPermission.roles batch: {}", (afterRoles - beforeRoles));
    log.debug("Profile contains {} CartographyPermission(s)", profile.getGroups().size());

    // Baseline: 21 statements (H2 + 04_data). createProfile base 20; +1 touching permission.roles.
    // Task/Tree use @BatchSize instead of EntityGraph on findByRolesAndTerritory /
    // findByAppAndRoles.

    String detailedBreakdown =
        String.format(
            """
            
            Statement breakdown:
              Total: %d
              - createProfile base: %d
              - Cartography.service: %d
              - CartographyPermission.members batch: %d
              - CartographyPermission.roles batch: %d
              Profile has %d permission(s), %d member(s), %d role(s)""",
            statements,
            statementsAfterProfile - statementsBeforeProfile,
            afterService - beforeService,
            afterMembers - beforeMembers,
            afterRoles - beforeRoles,
            profile.getGroups().size(),
            memberCount,
            roleCount);

    assertThat(statements)
        .as("JDBC statement count (baseline: 21, ceiling: 27)%s", detailedBreakdown)
        .isLessThanOrEqualTo(27);
  }
}
