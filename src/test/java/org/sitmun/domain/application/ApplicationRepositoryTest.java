package org.sitmun.domain.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashSet;
import org.assertj.core.api.Assumptions;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Assertions;
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
import org.sitmun.infrastructure.persistence.config.LiquibaseConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
@DisplayName("Application Repository JPA Test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ApplicationRepositoryTest {

  @Autowired private ApplicationRepository applicationRepository;
  @Autowired private CartographyPermissionRepository cartographyPermissionRepository;
  @Autowired private BackgroundRepository backgroundRepository;
  private Application application;

  @BeforeEach
  void init() {

    application =
        Application.builder()
            .name("Test")
            .createdDate(Date.from(Instant.now()))
            .trees(null)
            .treeAutoRefresh(true)
            .scales(null)
            .situationMap(null)
            .parameters(null)
            .srs(null)
            .parameters(new HashSet<>())
            .theme(null)
            .type(null)
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
  @DisplayName("Create a new application with all related entities")
  void createApplicationWithAllEntities() {
    assertThat(application.getId()).isNull();
    applicationRepository.save(application);
    assertThat(application.getId()).isNotZero();
  }

  @Test
  @DisplayName("Find an application by its ID with all related entities")
  void findApplicationByIdWithAllEntities() {
    assertThat(application.getId()).isNull();
    applicationRepository.save(application);
    assertThat(application.getId()).isNotZero();

    application = applicationRepository.findById(application.getId()).orElseGet(Assertions::fail);
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
    Assumptions.assumeThat(application.getId()).isNotZero();

    Integer id = application.getId();
    applicationRepository.delete(application);
    assertThat(applicationRepository.findById(id)).isEmpty();
  }

  @Test
  @DisplayName("Find applications filtered by username")
  void findApplicationsByUsername() {
    assertThat(applicationRepository.findByUser("public", PageRequest.of(0, 10))).hasSize(4);
  }

  @Test
  @DisplayName("Find applications filtered by username and territory")
  void findApplicationsByUsernameAndTerritory() {
    assertThat(applicationRepository.findByUserAndTerritory("public", 1, PageRequest.of(0, 10)))
        .hasSize(4);
  }

  @TestConfiguration
  @Import(LiquibaseConfig.class)
  static class Configuration {
    @Bean
    @Primary
    TaskExecutor taskExecutor() {
      return new SyncTaskExecutor();
    }
  }
}
