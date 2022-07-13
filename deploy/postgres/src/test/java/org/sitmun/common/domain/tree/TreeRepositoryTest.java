package org.sitmun.common.domain.tree;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sitmun.common.domain.tree.Tree;
import org.sitmun.common.domain.tree.TreeRepository;
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

class TreeRepositoryTest {

  @Autowired
  private TreeRepository treeRepository;
  private Tree tree;

  @BeforeEach
  void init() {
    tree = new Tree();
    tree.setName("Test");

  }

  @Test
  void saveTerritory() {
    Assertions.assertThat(tree.getId()).isNull();
    treeRepository.save(tree);
    Assertions.assertThat(tree.getId()).isNotZero();
  }

  @Test
  void findOneTerritoryById() {
    Assertions.assertThat(tree.getId()).isNull();
    treeRepository.save(tree);
    Assertions.assertThat(tree.getId()).isNotZero();

    Assertions.assertThat(treeRepository.findById(tree.getId())).isNotNull();
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
