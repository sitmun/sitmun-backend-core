package org.sitmun.plugin.core.repository;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sitmun.plugin.core.config.LiquibaseConfig;
import org.sitmun.plugin.core.domain.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Instant;
import java.util.Date;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

@ExtendWith(SpringExtension.class)
@DataJpaTest

public class ApplicationRepositoryTest {

  @TestConfiguration
  @Import(LiquibaseConfig.class)
  static class Configuration {
    @Bean
    @Primary
    public TaskExecutor taskExecutor() {
      return new SyncTaskExecutor();
    }
  }

  @Autowired
  private ApplicationRepository applicationRepository;

  @Autowired
  private CartographyPermissionRepository cartographyPermissionRepository;

  @Autowired
  private BackgroundRepository backgroundRepository;

  private Application application;


  @BeforeEach
  public void init() {

    application = Application.builder()
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

    Role rol = Role.builder()
      .name("Rol 1")
      .build();
    application.getAvailableRoles().add(rol);

    CartographyPermission backgroundMap;
    backgroundMap = CartographyPermission.builder()
      .name("Background map")
      .build();
    cartographyPermissionRepository.save(backgroundMap);

    Background background = new Background();
    background.setActive(true);
    background.setDescription(null);
    background.setName("fondo");
    background.setCartographyGroup(backgroundMap);
    background.setCreatedDate(new Date());
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
  public void saveApplication() {
    assertThat(application.getId()).isNull();
    applicationRepository.save(application);
    assertThat(application.getId()).isNotZero();
  }

  @Test
  public void findOneApplicationById() {
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
  public void deleteApplicationById() {
    assertThat(application.getId()).isNull();
    applicationRepository.save(application);
    assumeThat(application.getId()).isNotZero();

    Integer id = application.getId();
    applicationRepository.delete(application);
    assertThat(applicationRepository.findById(id)).isEmpty();
  }
}
