package org.sitmun.domain.application;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashSet;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.domain.application.background.ApplicationBackground;
import org.sitmun.domain.application.parameter.ApplicationParameter;
import org.sitmun.domain.background.Background;
import org.sitmun.domain.background.BackgroundRepository;
import org.sitmun.domain.cartography.permission.CartographyPermission;
import org.sitmun.domain.cartography.permission.CartographyPermissionRepository;
import org.sitmun.domain.role.Role;
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
@DisplayName("Application Repository JPA Test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ApplicationRepositoryTest {

  @Autowired private ApplicationRepository applicationRepository;
  @Autowired private EntityManager entityManager;
  @Autowired private CartographyPermissionRepository cartographyPermissionRepository;
  @Autowired private BackgroundRepository backgroundRepository;
  private Application application;

  @BeforeEach
  void init() {

    application =
        Application.builder()
            .name("Test")
            .createdDate(Date.from(Instant.now()))
            .lastUpdate(Date.from(Instant.now()))
            .trees(null)
            .treeAutoRefresh(true)
            .scales(null)
            .situationMap(null)
            .parameters(null)
            .srs(null)
            .parameters(new HashSet<>())
            .theme(null)
            .type("T")
            .title("Test")
            .availableRoles(new HashSet<>())
            .backgrounds(new HashSet<>())
            .build();

    Role rol = Role.builder().name("Rol 1").build();
    application.getAvailableRoles().add(rol);

    CartographyPermission backgroundMap =
        CartographyPermission.builder().name("Background map").build();
    cartographyPermissionRepository.save(backgroundMap);

    Background background = new Background();
    background.setActive(true);
    background.setDescription(null);
    background.setName("fondo");
    background.setCartographyGroup(backgroundMap);
    background.setCreatedDate(
        Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()));
    backgroundRepository.save(background);

    ApplicationBackground applicationBackground = new ApplicationBackground();
    applicationBackground.setApplication(application);
    applicationBackground.setBackground(background);
    applicationBackground.setOrder(1);
    application.getBackgrounds().add(applicationBackground);

    ApplicationParameter parameter = new ApplicationParameter();
    parameter.setApplication(application);
    parameter.setName("param1");
    parameter.setType("tipo1");
    parameter.setValue("valor1");
    application.getParameters().add(parameter);
  }

  @Test
  @DisplayName("Find an application by its ID with all related entities")
  void findApplicationByIdWithAllEntities() {
    assertThat(application.getId()).isNull();
    applicationRepository.save(application);
    assertThat(application.getId()).isNotZero();
    entityManager.flush();
    entityManager.detach(application);

    application = applicationRepository.findById(application.getId()).orElseThrow();
    SoftAssertions softly = new SoftAssertions();
    softly.assertThat(application.getAvailableRoles()).isNotEmpty();
    softly.assertThat(application.getBackgrounds()).isNotEmpty();
    softly.assertThat(application.getParameters()).isNotEmpty();
    softly.assertAll();
  }

  @Test
  @DisplayName("Remove an application by its ID")
  void removeApplicationById() {
    assertThat(application.getId()).isNull();
    applicationRepository.save(application);
    assertThat(application.getId()).isNotZero();

    Integer id = application.getId();
    applicationRepository.delete(application);
    assertThat(applicationRepository.findById(id)).isEmpty();
  }

  @Test
  @DisplayName("Find applications filtered by username")
  void findApplicationsByUsername() {
    assertThat(titles(applicationRepository.findByUser(SecurityConstants.PUBLIC_PRINCIPAL, page())))
        .containsExactlyInAnyOrder(
            "SITMUN - Externa",
            "SITMUN - Municipal",
            "SITMUN - Provincial",
            "SITMUN - Supramunicipal");
    assertThat(titles(applicationRepository.findByUser("internal", page())))
        .containsExactlyInAnyOrder(
            "SITMUN - Externa",
            "SITMUN - Externa protegida",
            "SITMUN - Municipal",
            "SITMUN - Provincial",
            "SITMUN - Supramunicipal");
  }

  @Test
  @DisplayName("Territory 2 omits the application granted only on territory 1")
  void findApplicationsByUsernameAndTerritory() {
    assertThat(
            titles(applicationRepository.findByRestrictedUserAndTerritory("internal", 2, page())))
        .containsExactlyInAnyOrder(
            "SITMUN - Externa",
            "SITMUN - Municipal",
            "SITMUN - Provincial",
            "SITMUN - Supramunicipal");
    assertThat(titles(applicationRepository.findByPublicUserAndTerritory("internal", 2, page())))
        .containsExactlyInAnyOrder(
            "SITMUN - Externa",
            "SITMUN - Municipal",
            "SITMUN - Provincial",
            "SITMUN - Supramunicipal");
  }

  private static PageRequest page() {
    return PageRequest.of(0, 10);
  }

  private static Iterable<String> titles(Page<Application> applications) {
    return applications.getContent().stream().map(Application::getTitle).toList();
  }
}
