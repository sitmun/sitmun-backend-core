package org.sitmun.domain.task;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.domain.role.Role;
import org.sitmun.domain.role.RoleRepository;
import org.sitmun.infrastructure.persistence.config.LiquibaseConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


@DataJpaTest
class TaskRepositoryTest {

  @Autowired
  private TaskRepository taskRepository;

  @Autowired
  private RoleRepository roleRepository;

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

  @Test
  @DisplayName("Find tasks by roles and territory")
  void findTasksByRolesAndTerritory() {
    List<Role> roles = roleRepository.findRolesByApplicationAndUserAndTerritory("public", 1, 1);
    List<Task> cp = taskRepository.findByRolesAndTerritory(roles, 1);
    assertThat(cp).hasSize(4);
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
