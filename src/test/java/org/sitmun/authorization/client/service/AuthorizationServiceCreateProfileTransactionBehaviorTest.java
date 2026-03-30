package org.sitmun.authorization.client.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sitmun.authorization.client.service.ProfileContext.NodeSectionBehaviour.VIRTUAL_ROOT_ALL_NODES;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration test for {@link AuthorizationService#createProfile} transaction behavior.
 *
 * <p>This test class validates the transaction boundary contract for createProfile. PR #250
 * replaced {@code Propagation.SUPPORTS} with the default {@code Propagation.REQUIRED}, ensuring
 * createProfile opens its own read-only transaction regardless of caller context.
 *
 * <p><b>Related PR:</b> <a
 * href="https://github.com/sitmun/sitmun-backend-core/pull/250">sitmun/sitmun-backend-core#250</a>
 * - Improve app config request loading times
 *
 * <p>OSIV is explicitly disabled via {@code @TestPropertySource} to isolate service-layer
 * transaction behavior. In production, the controller {@code getProfile} method has its own
 * {@code @Transactional(readOnly = true)} boundary, and OSIV provides a secondary safety net.
 *
 * <p><b>Why BatchSize + REQUIRED is better than EntityGraph:</b>
 *
 * <ul>
 *   <li><b>Cartesian product elimination:</b> EntityGraph on multiple collections (members + roles)
 *       creates NxM result rows. For 100 members and 50 roles, that's 5,000 rows returned for 150
 *       logical entities. BatchSize loads linearly: 100 + 50 = 150 rows.
 *   <li><b>Scalability:</b> EntityGraph performance degrades as collections grow (quadratic result
 *       set size). BatchSize stays constant (3 queries regardless of collection sizes).
 *   <li><b>Memory efficiency:</b> EntityGraph forces Hibernate to de-duplicate the cartesian result
 *       in memory. BatchSize loads only what's needed.
 *   <li><b>Network overhead:</b> Smaller result sets mean less data transfer from database to
 *       application.
 *   <li><b>Query predictability:</b> BatchSize produces consistent query patterns; EntityGraph
 *       performance varies wildly with data shape.
 * </ul>
 *
 * <p><b>Access pattern fit:</b> The code in buildProfile/pruneProfile iterates over ALL permissions
 * (line 231 filter, line 233 forEach, line 273 situationMap.getMembers(), line 390
 * background.getCartographyGroup().getMembers()). This dense, complete access pattern is ideal for
 * BatchSize — it amortizes the lazy loads into batch queries instead of N+1.
 *
 * <p><b>Trade-off:</b> Profile path uses {@code @BatchSize} on CartographyPermission, Task, and
 * Tree instead of multi-collection EntityGraphs — see {@code
 * AuthorizationServiceCreateProfileStatementCountTest} for the current JDBC budget.
 */
@SpringBootTest
@TestPropertySource(properties = "spring.jpa.open-in-view=false")
@DisplayName("AuthorizationService.createProfile transaction behavior")
class AuthorizationServiceCreateProfileTransactionBehaviorTest {

  @Autowired private AuthorizationService authorizationService;

  private ProfileContext buildTestContext() {
    return ProfileContext.builder()
        .username("admin")
        .appId(1)
        .territoryId(1)
        .nodeSectionBehaviour(VIRTUAL_ROOT_ALL_NODES)
        .nodeId(null)
        .build();
  }

  @Test
  @DisplayName("Test A: createProfile without outer transaction")
  void createProfile_noOuterTransaction_succeeds() {
    // Test A: No @Transactional on test method
    // Validates that createProfile opens its own transaction (REQUIRED propagation)
    // when called without an existing transaction context.
    ProfileContext context = buildTestContext();

    Optional<Profile> profileOpt = authorizationService.createProfile(context);

    assertThat(profileOpt).isPresent();
    Profile profile = profileOpt.get();

    assertThat(profile.getLayers()).isNotEmpty();
    assertThat(profile.getLayers().get(0).getService()).isNotNull();

    assertThat(profile.getGroups()).isNotEmpty();
    assertThat(profile.getGroups().iterator().next().getMembers()).isNotNull();
    assertThat(profile.getGroups().iterator().next().getRoles()).isNotNull();
  }

  @Test
  @Transactional(readOnly = true)
  @DisplayName("Test B: createProfile with outer transaction (mirrors production controller path)")
  void createProfile_withOuterTransaction_succeeds() {
    // Test B: @Transactional(readOnly=true) on test method
    // Validates that createProfile participates in existing transaction (REQUIRED propagation)
    // and that lazy-loaded associations work correctly within the outer transaction scope.
    // Mirrors production: controller has @Transactional, calls service, OSIV extends context.
    ProfileContext context = buildTestContext();

    Optional<Profile> profileOpt = authorizationService.createProfile(context);

    assertThat(profileOpt).isPresent();
    Profile profile = profileOpt.get();

    assertThat(profile.getLayers()).isNotEmpty();
    assertThat(profile.getLayers().get(0).getService()).isNotNull();

    assertThat(profile.getGroups()).isNotEmpty();
    assertThat(profile.getGroups().iterator().next().getMembers()).isNotNull();
    assertThat(profile.getGroups().iterator().next().getRoles()).isNotNull();
  }
}
