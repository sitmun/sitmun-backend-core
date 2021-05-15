package org.sitmun.plugin.core.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sitmun.plugin.core.config.LiquibaseConfig;
import org.sitmun.plugin.core.domain.Tree;
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

public class TreeRepositoryTest {

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
  private TreeRepository treeRepository;

  private Tree tree;

  @BeforeEach
  public void init() {
    tree = new Tree();
    tree.setName("Test");

  }

  @Test
  public void saveTerritory() {
    assertThat(tree.getId()).isNull();
    treeRepository.save(tree);
    assertThat(tree.getId()).isNotZero();
  }

  @Test
  public void findOneTerritoryById() {
    assertThat(tree.getId()).isNull();
    treeRepository.save(tree);
    assertThat(tree.getId()).isNotZero();

    assertThat(treeRepository.findById(tree.getId())).isNotNull();
  }
}
