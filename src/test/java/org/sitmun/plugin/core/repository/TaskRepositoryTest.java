package org.sitmun.plugin.core.repository;

import static org.assertj.core.api.Assertions.assertThat;


import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sitmun.plugin.core.domain.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
public class TaskRepositoryTest {

  @Autowired
  private TaskRepository taskRepository;

  private Task task;

  @Before
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

    assertThat(taskRepository.findOne(task.getId())).isNotNull();
  }

}
