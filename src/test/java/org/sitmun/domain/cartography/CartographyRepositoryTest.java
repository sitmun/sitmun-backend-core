package org.sitmun.domain.cartography;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.assertj.core.api.Assertions;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.domain.cartography.availability.CartographyAvailability;
import org.sitmun.domain.cartography.availability.CartographyAvailabilityRepository;
import org.sitmun.domain.cartography.permission.CartographyPermission;
import org.sitmun.domain.cartography.permission.CartographyPermissionRepository;
import org.sitmun.domain.cartography.style.CartographyStyle;
import org.sitmun.domain.cartography.style.CartographyStyleRepository;
import org.sitmun.domain.role.Role;
import org.sitmun.domain.role.RoleRepository;
import org.sitmun.domain.service.Service;
import org.sitmun.domain.service.ServiceRepository;
import org.sitmun.domain.territory.Territory;
import org.sitmun.domain.territory.TerritoryRepository;
import org.sitmun.infrastructure.persistence.type.i18n.I18nTestConfiguration;
import org.sitmun.infrastructure.security.core.SecurityConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

@DataJpaTest
@DisplayName("Cartography Repository JPA test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CartographyRepositoryTest {

  @Autowired private CartographyRepository cartographyRepository;

  @Autowired private RoleRepository roleRepository;

  @Autowired private TerritoryRepository territoryRepository;

  @Autowired private CartographyAvailabilityRepository cartographyAvailabilityRepository;

  @Autowired private CartographyStyleRepository cartographyStyleRepository;

  @Autowired private CartographyPermissionRepository cartographyPermissionRepository;

  @Autowired private ServiceRepository serviceRepository;

  @Autowired private EntityManager entityManager;

  @Test
  @DisplayName("Save a new cartography to database")
  void saveCartography() {
    Cartography cartography = cartographyBuilder().build();
    Assertions.assertThat(cartography.getId()).isNull();
    cartographyRepository.save(cartography);
    Assertions.assertThat(cartography.getId()).isNotZero();
  }

  @Test
  @DisplayName("Find a cartography by its ID")
  void findOneCartographyById() {
    Cartography cartography = cartographyBuilder().build();
    Assertions.assertThat(cartography.getId()).isNull();
    cartographyRepository.save(cartography);
    Assertions.assertThat(cartography.getId()).isNotZero();

    Assertions.assertThat(cartographyRepository.findById(cartography.getId())).isNotNull();
  }

  @Test
  @DisplayName("findById does not eager-load multi-bag collections")
  void findByIdDoesNotEagerLoadMultiBagCollections() {
    Territory territory =
        territoryRepository.save(
            Territory.builder()
                .name("Bag-graph territory")
                .code("batch-size")
                .blocked(false)
                .build());

    Service service =
        serviceRepository.save(
            Service.builder()
                .name("Batch-size service")
                .serviceURL("http://localhost/api/services/batch-size")
                .type("WMS")
                .blocked(false)
                .build());

    Cartography cartography =
        cartographyRepository.save(
            cartographyBuilder().name("Fat layer").service(service).layers(List.of("L1")).build());

    CartographyAvailability availability = new CartographyAvailability();
    availability.setCartography(cartography);
    availability.setTerritory(territory);
    availability.setCreatedDate(
        Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()));
    cartographyAvailabilityRepository.save(availability);

    cartographyStyleRepository.save(
        CartographyStyle.builder()
            .name("Style for batch-size")
            .cartography(cartography)
            .defaultStyle(true)
            .build());

    CartographyPermission permission =
        cartographyPermissionRepository.save(
            CartographyPermission.builder().name("Permission for batch-size").type("X").build());
    cartography.setPermissions(new HashSet<>(Set.of(permission)));
    cartographyRepository.save(cartography);

    entityManager.flush();
    entityManager.clear();

    Cartography loaded = cartographyRepository.findById(cartography.getId()).orElseThrow();

    assertThat(Hibernate.isInitialized(loaded.getAvailabilities())).isFalse();
    assertThat(Hibernate.isInitialized(loaded.getPermissions())).isFalse();
    assertThat(Hibernate.isInitialized(loaded.getFilters())).isFalse();
    assertThat(Hibernate.isInitialized(loaded.getParameters())).isFalse();
    assertThat(Hibernate.isInitialized(loaded.getTreeNodes())).isFalse();
    assertThat(Hibernate.isInitialized(loaded.getSpatialSelectionParameters())).isFalse();
    assertThat(Hibernate.isInitialized(loaded.getStyles())).isFalse();

    assertThat(loaded.getStyles()).isNotEmpty();
    assertThat(loaded.getStyles().iterator().next().getName()).isEqualTo("Style for batch-size");
  }

  private static Cartography.CartographyBuilder cartographyBuilder() {
    return Cartography.builder()
        .name("Test")
        .createdDate(Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()))
        .order(0)
        .queryableFeatureAvailable(true)
        .queryableFeatureEnabled(true)
        .selectableFeatureEnabled(true)
        .thematic(true)
        .transparency(0)
        .blocked(false)
        .useAllStyles(false);
  }

  @Test
  @DisplayName("Find cartographies by roles and territory")
  void findCartographiesByRolesAndTerritory() {
    List<Role> roles =
        roleRepository.findRolesByApplicationAndUserAndTerritory(
            SecurityConstants.PUBLIC_PRINCIPAL, 1, 1);
    List<Cartography> cp = cartographyRepository.findByRolesAndTerritory(roles, 1);
    assertThat(cp).hasSize(11);
  }

  @TestConfiguration
  @Import(I18nTestConfiguration.class)
  static class Configuration {}
}
