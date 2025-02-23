package org.sitmun.domain.territory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.domain.territory.type.TerritoryType;
import org.sitmun.domain.territory.type.TerritoryTypeRepository;
import org.sitmun.infrastructure.persistence.config.LiquibaseConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;


@DataJpaTest
@DisplayName("Territory Repository Test")
class TerritoryRepositoryTest {

  @Autowired
  private TerritoryRepository territoryRepository;
  @Autowired
  private TerritoryTypeRepository territoryTypeRepository;
  private Territory territory;

  @BeforeEach
  void init() {
    TerritoryType type = TerritoryType.builder().build();
    type.setName("tipo Territorio 1");
    territoryTypeRepository.save(type);

    territory = Territory.builder()
      .name("Admin")
      .blocked(false)
      .territorialAuthorityEmail("email@email.org")
      .createdDate(new Date())
      .territorialAuthorityName("Test")
      .type(type)
      .build();
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
  @DisplayName("Find territories for a user and an application")
  void findTerritoriesByUserAndApplication() {
    Page<Territory> territories = territoryRepository.findByUserAndApplication("public", 1, PageRequest.of(0, 10));
    assertThat(territories.getTotalElements()).isEqualTo(3);
  }

  @Test
  @DisplayName("Find territories for a user")
  void findTerritoriesOfAnUser() {
    Page<Territory> territories = territoryRepository.findByUser("public", PageRequest.of(0, 10));
    assertThat(territories.getTotalElements()).isEqualTo(2);
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
