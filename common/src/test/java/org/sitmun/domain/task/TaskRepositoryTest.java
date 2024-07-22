package org.sitmun.domain.task;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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


@DataJpaTest
class TaskRepositoryTest {

  @Autowired
  private TaskRepository taskRepository;
  private Task task;

  @BeforeEach
  void init() {
    task = new Task();
  }

  @AfterEach
  void cleanup() {
    taskRepository.delete(task);
  }

  @Test
  void saveTask() {
    Assertions.assertThat(task.getId()).isNull();
    taskRepository.save(task);
    Assertions.assertThat(task.getId()).isNotZero();
  }

  @Test
  void findOneTaskById() {
    Assertions.assertThat(task.getId()).isNull();
    taskRepository.save(task);
    Assertions.assertThat(task.getId()).isNotZero();

    Assertions.assertThat(taskRepository.findById(task.getId())).isNotNull();
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
