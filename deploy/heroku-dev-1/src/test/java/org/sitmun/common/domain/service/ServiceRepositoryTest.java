package org.sitmun.common.domain.service;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sitmun.common.domain.service.Service;
import org.sitmun.common.domain.service.ServiceRepository;
import org.sitmun.legacy.config.LiquibaseConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@DataJpaTest

class ServiceRepositoryTest {

  @Autowired
  private ServiceRepository serviceRepository;
  private Service service;

  @BeforeEach
  void init() {
    service = Service.builder().build();
  }

  @Test
  void saveService() {
    Assertions.assertThat(service.getId()).isNull();
    serviceRepository.save(service);
    Assertions.assertThat(service.getId()).isNotZero();
  }

  @Test
  void findOneServiceById() {
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
