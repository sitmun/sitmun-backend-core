package org.sitmun.domain.territory;

import static org.assertj.core.api.Assertions.assertThat;

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
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
@DisplayName("Territory Repository Test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TerritoryRepositoryTest {

  @Autowired private TerritoryRepository territoryRepository;
  @Autowired private TerritoryTypeRepository territoryTypeRepository;

  private Territory territory;
  private TerritoryType type;

  @BeforeEach
  void init() {
    type = TerritoryType.builder().build();
    type.setName("tipo Territorio 1");
    territoryTypeRepository.save(type);

    territory =
        Territory.builder()
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
    if (type.getId() != null) {
      territoryTypeRepository.deleteById(type.getId());
    }
  }

  @Test
  @DisplayName("Save a territory")
  void saveTerritory() {
    assertThat(territory.getId()).isNull();
    territoryRepository.save(territory);
    assertThat(territory.getId()).isNotZero();
  }

  @Test
  @DisplayName("Find a territory by id")
  void findOneTerritoryById() {
    assertThat(territory.getId()).isNull();
    territoryRepository.save(territory);
    assertThat(territory.getId()).isNotZero();

    assertThat(territoryRepository.findById(territory.getId())).isNotNull();
  }

  @Test
  @DisplayName("Save and retrieve territory description")
  void saveAndRetrieveTerritoryDescription() {
    territoryRepository.save(territory);
    Territory savedTerritory = territoryRepository.findById(territory.getId()).orElse(null);
    assertThat(savedTerritory).isNotNull();
    assertThat(savedTerritory.getDescription()).isEqualTo("Test territory description");
  }

  @Test
  @DisplayName("Find territories for a user and an application")
  void findTerritoriesByUserAndApplication() {
    Page<Territory> territories =
        territoryRepository.findByRestrictedUserAndApplication(
            SecurityConstants.PUBLIC_PRINCIPAL, 1, PageRequest.of(0, 10));
    assertThat(territories.getTotalElements()).isEqualTo(3);
  }

  @Test
  @DisplayName("Find territories for a user")
  void findTerritoriesOfAnUser() {
    Page<Territory> territories =
        territoryRepository.findByRestrictedUser(
            SecurityConstants.PUBLIC_PRINCIPAL, PageRequest.of(0, 10));
    assertThat(territories.getTotalElements()).isEqualTo(3);
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

  @TestConfiguration
  @Import(I18nTestConfiguration.class)
  static class Configuration {}
}
