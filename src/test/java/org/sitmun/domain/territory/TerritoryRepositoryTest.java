package org.sitmun.domain.territory;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.util.Date;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.domain.territory.type.TerritoryType;
import org.sitmun.domain.territory.type.TerritoryTypeRepository;
import org.sitmun.infrastructure.persistence.type.i18n.I18nTestConfiguration;
import org.sitmun.infrastructure.security.core.SecurityConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
@Import(I18nTestConfiguration.class)
@DisplayName("Territory Repository Test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TerritoryRepositoryTest {

  @Autowired private TerritoryRepository territoryRepository;
  @Autowired private EntityManager entityManager;
  @Autowired private TerritoryTypeRepository territoryTypeRepository;

  private Territory territory;

  @BeforeEach
  void init() {
    TerritoryType type = territoryTypeRepository.findById(8).orElseThrow();

    territory =
        Territory.builder()
            .code("T-DESC")
            .name("Admin")
            .description("Test territory description")
            .blocked(false)
            .territorialAuthorityEmail("email@email.org")
            .createdDate(new Date())
            .territorialAuthorityName("Test")
            .type(type)
            .build();
  }

  @AfterEach
  void clean() {
    if (territory.getId() != null) {
      territoryRepository.deleteById(territory.getId());
    }
  }

  @Test
  @DisplayName("Save and retrieve territory description")
  void saveAndRetrieveTerritoryDescription() {
    territoryRepository.save(territory);
    entityManager.flush();
    entityManager.detach(territory);
    Territory savedTerritory = territoryRepository.findById(territory.getId()).orElseThrow();
    assertThat(savedTerritory).isNotNull();
    assertThat(savedTerritory.getDescription()).isEqualTo("Test territory description");
  }

  @Test
  @DisplayName("Application 5 returns only the territory that holds its role")
  void findTerritoriesByUserAndApplication() {
    assertThat(
            names(
                territoryRepository.findByRestrictedUserAndApplication(
                    "internal", 5, PageRequest.of(0, 10))))
        .containsExactly("Provincia A");
    assertThat(
            names(
                territoryRepository.findByPublicUserAndApplication(
                    "internal", 5, PageRequest.of(0, 10))))
        .containsExactly("Provincia A");
  }

  @Test
  @DisplayName("Find territories for a user")
  void findTerritoriesOfAnUser() {
    assertThat(
            names(
                territoryRepository.findByRestrictedUser(
                    SecurityConstants.PUBLIC_PRINCIPAL, PageRequest.of(0, 10))))
        .containsExactlyInAnyOrder("Municipio B", "Otro C", "Provincia A");
    assertThat(names(territoryRepository.findByRestrictedUser("internal", PageRequest.of(0, 10))))
        .containsExactlyInAnyOrder("Municipio B", "Otro C", "Provincia A");
  }

  @Test
  @DisplayName("ComputedView returns null when extent is null")
  void computedViewReturnsNullWhenExtentIsNull() {
    Territory territoryWithoutExtent =
        Territory.builder().name("Test").code("TEST").blocked(false).build();

    assertThat(territoryWithoutExtent.getComputedView()).isNull();
  }

  @Test
  @DisplayName("ComputedView returns extent when center is null")
  void computedViewReturnsExtentWhenCenterIsNull() {
    org.sitmun.infrastructure.persistence.type.envelope.Envelope extent =
        org.sitmun.infrastructure.persistence.type.envelope.Envelope.builder()
            .minX(100.0)
            .maxX(200.0)
            .minY(50.0)
            .maxY(150.0)
            .build();

    Territory territoryWithoutCenter =
        Territory.builder().name("Test").code("TEST").blocked(false).extent(extent).build();

    assertThat(territoryWithoutCenter.getComputedView()).isEqualTo(extent);
  }

  @Test
  @DisplayName("ComputedView returns extent when center coordinates are null")
  void computedViewReturnsExtentWhenCenterCoordinatesAreNull() {
    org.sitmun.infrastructure.persistence.type.envelope.Envelope extent =
        org.sitmun.infrastructure.persistence.type.envelope.Envelope.builder()
            .minX(100.0)
            .maxX(200.0)
            .minY(50.0)
            .maxY(150.0)
            .build();

    org.sitmun.infrastructure.persistence.type.point.Point incompleteCenter =
        org.sitmun.infrastructure.persistence.type.point.Point.builder().x(150.0).build();

    Territory territoryWithIncompleteCenter =
        Territory.builder()
            .name("Test")
            .code("TEST")
            .blocked(false)
            .extent(extent)
            .center(incompleteCenter)
            .build();

    assertThat(territoryWithIncompleteCenter.getComputedView()).isEqualTo(extent);
  }

  @Test
  @DisplayName("ComputedView returns extent when center is (0, 0) from legacy data")
  void computedViewReturnsExtentWhenCenterIsZeroZero() {
    org.sitmun.infrastructure.persistence.type.envelope.Envelope extent =
        org.sitmun.infrastructure.persistence.type.envelope.Envelope.builder()
            .minX(100.0)
            .maxX(200.0)
            .minY(50.0)
            .maxY(150.0)
            .build();

    org.sitmun.infrastructure.persistence.type.point.Point legacyCenter =
        org.sitmun.infrastructure.persistence.type.point.Point.builder().x(0.0).y(0.0).build();

    Territory territoryWithLegacyCenter =
        Territory.builder()
            .name("Test")
            .code("TEST")
            .blocked(false)
            .extent(extent)
            .center(legacyCenter)
            .build();

    assertThat(territoryWithLegacyCenter.getComputedView()).isEqualTo(extent);
  }

  @Test
  @DisplayName("ComputedView centers on point of interest when center is at extent center")
  void computedViewCentersOnPointWhenCenterIsAtExtentCenter() {
    org.sitmun.infrastructure.persistence.type.envelope.Envelope extent =
        org.sitmun.infrastructure.persistence.type.envelope.Envelope.builder()
            .minX(100.0)
            .maxX(200.0)
            .minY(50.0)
            .maxY(150.0)
            .build();

    // Center point is exactly in the middle of the extent
    org.sitmun.infrastructure.persistence.type.point.Point center =
        org.sitmun.infrastructure.persistence.type.point.Point.builder().x(150.0).y(100.0).build();

    Territory territoryWithCenteredPoint =
        Territory.builder()
            .name("Test")
            .code("TEST")
            .blocked(false)
            .extent(extent)
            .center(center)
            .build();

    org.sitmun.infrastructure.persistence.type.envelope.Envelope computedView =
        territoryWithCenteredPoint.getComputedView();

    assertThat(computedView).isNotNull();
    // When center is at the middle, computed view should equal the original extent
    assertThat(computedView.getMinX()).isEqualTo(100.0);
    assertThat(computedView.getMaxX()).isEqualTo(200.0);
    assertThat(computedView.getMinY()).isEqualTo(50.0);
    assertThat(computedView.getMaxY()).isEqualTo(150.0);
  }

  @Test
  @DisplayName("Seed territory 3: computedView equals raw extent when center is absent")
  void territory3ComputedView() {
    Territory territory = territoryRepository.findById(3).orElseThrow();
    org.sitmun.infrastructure.persistence.type.envelope.Envelope view = territory.getComputedView();
    assertThat(view).isNotNull();
    assertThat(view.getMinX()).isEqualTo(430250.0);
    assertThat(view.getMinY()).isEqualTo(4612070.0);
    assertThat(view.getMaxX()).isEqualTo(469609.0);
    assertThat(view.getMaxY()).isEqualTo(4638298.0);
  }

  @Test
  @DisplayName("ComputedView expands to keep center in middle when point is offset")
  void computedViewExpandsWhenCenterIsOffset() {
    org.sitmun.infrastructure.persistence.type.envelope.Envelope extent =
        org.sitmun.infrastructure.persistence.type.envelope.Envelope.builder()
            .minX(100.0)
            .maxX(200.0)
            .minY(50.0)
            .maxY(150.0)
            .build();

    // Center point is offset to the left and down from the extent center
    org.sitmun.infrastructure.persistence.type.point.Point center =
        org.sitmun.infrastructure.persistence.type.point.Point.builder()
            .x(120.0) // Closer to minX than maxX
            .y(70.0) // Closer to minY than maxY
            .build();

    Territory territoryWithOffsetPoint =
        Territory.builder()
            .name("Test")
            .code("TEST")
            .blocked(false)
            .extent(extent)
            .center(center)
            .build();

    org.sitmun.infrastructure.persistence.type.envelope.Envelope computedView =
        territoryWithOffsetPoint.getComputedView();

    assertThat(computedView).isNotNull();
    // Center should be at the middle of the computed view
    double computedCenterX = (computedView.getMinX() + computedView.getMaxX()) / 2;
    double computedCenterY = (computedView.getMinY() + computedView.getMaxY()) / 2;
    assertThat(computedCenterX).isEqualTo(120.0);
    assertThat(computedCenterY).isEqualTo(70.0);

    // Original extent should be fully contained in computed view
    assertThat(computedView.getMinX()).isLessThanOrEqualTo(100.0);
    assertThat(computedView.getMaxX()).isGreaterThanOrEqualTo(200.0);
    assertThat(computedView.getMinY()).isLessThanOrEqualTo(50.0);
    assertThat(computedView.getMaxY()).isGreaterThanOrEqualTo(150.0);
  }

  private static Iterable<String> names(Page<Territory> territories) {
    return territories.getContent().stream().map(Territory::getName).toList();
  }
}
