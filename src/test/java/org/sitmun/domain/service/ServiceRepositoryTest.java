package org.sitmun.domain.service;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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

@DataJpaTest
@DisplayName("Service Repository JPA Test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ServiceRepositoryTest {

  @Autowired private ServiceRepository serviceRepository;
  private Service service;

  @BeforeEach
  void init() {
    service = Service.builder().build();
  }

  @Test
  @DisplayName("Save a new service to database")
  void saveService() {
    Assertions.assertThat(service.getId()).isNull();
    serviceRepository.save(service);
    Assertions.assertThat(service.getId()).isNotZero();
  }

  @Test
  @DisplayName("Find a service by its ID")
  void findServiceById() {
    Assertions.assertThat(service.getId()).isNull();
    serviceRepository.save(service);
    Assertions.assertThat(service.getId()).isNotZero();

    Assertions.assertThat(serviceRepository.findById(service.getId())).isNotNull();
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
