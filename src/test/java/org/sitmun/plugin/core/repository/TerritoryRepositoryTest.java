package org.sitmun.plugin.core.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sitmun.plugin.core.config.LiquibaseConfig;
import org.sitmun.plugin.core.domain.Territory;
import org.sitmun.plugin.core.domain.TerritoryType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@DataJpaTest
@ActiveProfiles("dev")
public class TerritoryRepositoryTest {

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
  private TerritoryRepository territoryRepository;

  @Autowired
  private TerritoryTypeRepository territoryTypeRepository;

  private Territory territory;

  @BeforeEach
  public void init() {
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
  public void saveTerritory() {
    assertThat(territory.getId()).isNull();
    territoryRepository.save(territory);
    assertThat(territory.getId()).isNotZero();
  }

  @Test
  public void findOneTerritoryById() {
    assertThat(territory.getId()).isNull();
    territoryRepository.save(territory);
    assertThat(territory.getId()).isNotZero();

    assertThat(territoryRepository.findById(territory.getId())).isNotNull();
  }
}
