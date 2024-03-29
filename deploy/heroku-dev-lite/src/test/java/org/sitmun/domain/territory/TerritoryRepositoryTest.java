package org.sitmun.domain.territory;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
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

import java.util.Date;


@DataJpaTest
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
  void saveTerritory() {
    Assertions.assertThat(territory.getId()).isNull();
    territoryRepository.save(territory);
    Assertions.assertThat(territory.getId()).isNotZero();
  }

  @Test
  void findOneTerritoryById() {
    Assertions.assertThat(territory.getId()).isNull();
    territoryRepository.save(territory);
    Assertions.assertThat(territory.getId()).isNotZero();

    Assertions.assertThat(territoryRepository.findById(territory.getId())).isNotNull();
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
