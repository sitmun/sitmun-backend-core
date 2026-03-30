package org.sitmun.domain.cartography;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

/**
 * Baseline JDBC statement count for {@link CartographyRepository#findAll(Pageable)} after removing
 * the 4-attribute {@code @EntityGraph} and with {@code @BatchSize(size=50)} on all collections.
 *
 * <p>Previously had EntityGraph on service, spatialSelectionService, spatialSelectionConnection,
 * and styles. With BatchSize, list pagination uses default lazy loading, with batch fetching when
 * collections are accessed.
 *
 * <p><b>Measured (page 0, size 5):</b> total {@code 3} (count query + content query + 1 batch for
 * styles when touched). Baseline {@code 3}, ceiling {@code 5}.
 */
@SpringBootTest
@TestPropertySource(
    properties = {
      "spring.jpa.properties.hibernate.generate_statistics=true",
      "spring.jpa.open-in-view=false"
    })
@DisplayName("CartographyRepository.findAll(Pageable) statement count baseline")
class CartographyRepositoryFindAllPageStatementCountTest {

  private static final int PAGE_SIZE = 5;

  /** Measured: 3 statements (count + content + 1 batch for styles). */
  private static final int BASELINE_STATEMENTS = 3;

  private static final int CEILING_STATEMENTS = 5;

  @Autowired private CartographyRepository cartographyRepository;
  @Autowired private EntityManagerFactory entityManagerFactory;

  @Test
  @Transactional(readOnly = true)
  @DisplayName("findAll first page baseline with BatchSize (post-migration)")
  void findAll_firstPage_entityGraphBaseline() {
    SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
    Statistics statistics = sessionFactory.getStatistics();
    statistics.setStatisticsEnabled(true);
    statistics.clear();

    long beforeQuery = statistics.getPrepareStatementCount();
    Page<Cartography> page = cartographyRepository.findAll(PageRequest.of(0, PAGE_SIZE));
    long afterQuery = statistics.getPrepareStatementCount();
    long queryStatements = afterQuery - beforeQuery;

    List<Cartography> content = page.getContent();
    long beforeTouch = statistics.getPrepareStatementCount();
    int styleRefs = 0;
    for (Cartography c : content) {
      if (c.getService() != null) {
        c.getService().getId();
      }
      styleRefs += c.getStyles().size();
    }
    long afterTouch = statistics.getPrepareStatementCount();
    long touchStatements = afterTouch - beforeTouch;

    long total = statistics.getPrepareStatementCount();

    String detailedBreakdown =
        String.format(
            """
            \nStatement breakdown (CartographyRepository.findAll page 0, size %d):
              Total: %d
              - findAll query: %d (count + content)
              - touch service + styles (BatchSize): %d (style rows: %d)
              Elements in page: %d
              With @BatchSize: prevents N+1 on styles; service loaded on-demand
            """,
            PAGE_SIZE, total, queryStatements, touchStatements, styleRefs, content.size());

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
