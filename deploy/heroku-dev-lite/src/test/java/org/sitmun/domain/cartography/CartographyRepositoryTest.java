package org.sitmun.domain.cartography;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.infrastructure.persistence.config.LiquibaseConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;


@DataJpaTest
@DisplayName("Cartography Repository JPA test")
class CartographyRepositoryTest {

  @Autowired
  private CartographyRepository cartographyRepository;

  @Test
  @DisplayName("Save a Cartography")
  void saveCartography() {
    Cartography cartography = cartographyBuilder().build();
    Assertions.assertThat(cartography.getId()).isNull();
    cartographyRepository.save(cartography);
    Assertions.assertThat(cartography.getId()).isNotZero();
  }

  @Test
  @DisplayName("Find a Cartography by id")
  void findOneCartographyById() {
    Cartography cartography = cartographyBuilder().build();
    Assertions.assertThat(cartography.getId()).isNull();
    cartographyRepository.save(cartography);
    Assertions.assertThat(cartography.getId()).isNotZero();

    Assertions.assertThat(cartographyRepository.findById(cartography.getId())).isNotNull();
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

