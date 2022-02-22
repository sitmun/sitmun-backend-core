package org.sitmun.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sitmun.config.LiquibaseConfig;
import org.sitmun.domain.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@DataJpaTest

public class ServiceRepositoryTest {

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
  private ServiceRepository serviceRepository;

  private Service service;

  @BeforeEach
  public void init() {
    service = Service.builder().build();
  }

  @Test
  public void saveService() {
    assertThat(service.getId()).isNull();
    serviceRepository.save(service);
    assertThat(service.getId()).isNotZero();
  }

  @Test
  public void findOneServiceById() {
    assertThat(service.getId()).isNull();
    serviceRepository.save(service);
    assertThat(service.getId()).isNotZero();

    assertThat(serviceRepository.findById(service.getId())).isNotNull();
  }

}
