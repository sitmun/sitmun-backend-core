package org.sitmun.plugin.core.repository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sitmun.plugin.core.config.LiquibaseConfig;
import org.sitmun.plugin.core.domain.Task;
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

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@DataJpaTest
@ActiveProfiles("dev")
public class TaskRepositoryTest {

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
  private TaskRepository taskRepository;

  private Task task;

  @BeforeEach
  public void init() {
    task = new Task();
  }

  @AfterEach
  public void cleanup() {
    taskRepository.delete(task);
  }

  @Test
  public void saveTask() {
    assertThat(task.getId()).isNull();
    taskRepository.save(task);
    assertThat(task.getId()).isNotZero();
  }

  @Test
  public void findOneTaskById() {
    assertThat(task.getId()).isNull();
    taskRepository.save(task);
    assertThat(task.getId()).isNotZero();

    assertThat(taskRepository.findById(task.getId())).isNotNull();
  }

}
