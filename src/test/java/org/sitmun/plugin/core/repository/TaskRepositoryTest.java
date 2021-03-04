package org.sitmun.plugin.core.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sitmun.plugin.core.domain.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@DataJpaTest
@ActiveProfiles("dev")
public class TaskRepositoryTest {

  @Autowired
  private TaskRepository taskRepository;

  private Task task;

  @BeforeEach
  public void init() {
    task = new Task();

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
