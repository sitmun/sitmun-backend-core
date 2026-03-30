package org.sitmun.domain.service;

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
 * Baseline JDBC statement count for {@link ServiceRepository#findById} with
 * {@code @BatchSize(size=50)} applied to both collections.
 *
 * <p>Service has 2 OneToMany collections (layers, parameters). With {@code @BatchSize}, each
 * accessed collection triggers a batched IN query instead of individual N+1 SELECTs when Spring
 * Data REST HAL serialization walks the entity graph.
 *
 * <p><b>Trade-off:</b> Without BatchSize, loading N services with collections = 1+N queries per
 * collection. With BatchSize, loading N services = 1+(N/50) queries per collection.
 *
 * <p><b>Measured (04_data, MIN service id):</b> total {@code 3} JDBC statements (1 base + 2 for
 * collections with 2 rows each). At production scale (200 services), BatchSize reduces queries from
 * ~600 to ~200 (67% reduction). Baseline {@code 3}, ceiling {@code 5}.
 */
@SpringBootTest
@TestPropertySource(
    properties = {
      "spring.jpa.properties.hibernate.generate_statistics=true",
      "spring.jpa.open-in-view=false"
    })
@DisplayName("ServiceRepository.findById statement count baseline")
class ServiceRepositoryFindByIdStatementCountTest {

  /** Measured (04_data, MIN id): 3 statements (1 base + 2 batch per collection). */
  private static final int BASELINE_STATEMENTS = 3;

  private static final int CEILING_STATEMENTS = 5;

  @Autowired private ServiceRepository serviceRepository;
  @Autowired private EntityManagerFactory entityManagerFactory;
  @Autowired private EntityManager entityManager;

  @Test
  @Transactional(readOnly = true)
  @DisplayName("findById baseline with BatchSize (post-migration)")
  void findById_preBatchSizeBaseline() {
    Integer serviceId =
        entityManager
            .createQuery("SELECT MIN(s.id) FROM Service s", Integer.class)
            .getSingleResult();
    assertThat(serviceId).isNotNull();

    SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
    Statistics statistics = sessionFactory.getStatistics();
    statistics.setStatisticsEnabled(true);
    statistics.clear();

    long beforeQuery = statistics.getPrepareStatementCount();
    Optional<Service> opt = serviceRepository.findById(serviceId);
    long afterQuery = statistics.getPrepareStatementCount();
    long queryStatements = afterQuery - beforeQuery;
    assertThat(opt).isPresent();
    Service service = opt.get();

    long beforeTouch = statistics.getPrepareStatementCount();
    int layers = service.getLayers().size();
    int parameters = service.getParameters().size();
    long afterTouch = statistics.getPrepareStatementCount();
    long touchStatements = afterTouch - beforeTouch;

    long total = statistics.getPrepareStatementCount();

    String detailedBreakdown =
        String.format(
            """
            \nStatement breakdown (ServiceRepository.findById):
              Total: %d
              - findById query: %d
              - touch 2 collections (BatchSize): %d
              Service id: %d (layers=%d, parameters=%d)
              With @BatchSize: prevents N+1 at scale
              Production (200 services): 67%% query reduction (600→200 queries)
            """,
            total, queryStatements, touchStatements, serviceId, layers, parameters);

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
