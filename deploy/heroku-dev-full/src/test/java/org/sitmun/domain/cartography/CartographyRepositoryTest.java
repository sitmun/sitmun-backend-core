package org.sitmun.domain.cartography;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sitmun.infrastructure.persistence.config.LiquibaseConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Date;

@ExtendWith(SpringExtension.class)
@DataJpaTest

class CartographyRepositoryTest {

  @Autowired
  private CartographyRepository cartographyRepository;

  @Test
  void saveCartography() {
    Cartography cartography = cartographyBuilder().build();
    Assertions.assertThat(cartography.getId()).isNull();
    cartographyRepository.save(cartography);
    Assertions.assertThat(cartography.getId()).isNotZero();
  }

  @Test
  void findOneCartographyById() {
    Cartography cartography = cartographyBuilder().build();
    Assertions.assertThat(cartography.getId()).isNull();
    cartographyRepository.save(cartography);
    Assertions.assertThat(cartography.getId()).isNotZero();

    Assertions.assertThat(cartographyRepository.findById(cartography.getId())).isNotNull();
  }

  private Cartography.CartographyBuilder cartographyBuilder() {
    return Cartography.builder()
      .name("Test")
      .createdDate(new Date())
      .order(0)
      .queryableFeatureAvailable(true)
      .queryableFeatureEnabled(true)
      .selectableFeatureEnabled(true)
      .thematic(true)
      .transparency(0);
  }

  @TestConfiguration
  @Import(LiquibaseConfig.class)
  static class Configuration {
    @Bean
    @Primary
    public TaskExecutor taskExecutor() {
      return new SyncTaskExecutor();
    }
  }
}

