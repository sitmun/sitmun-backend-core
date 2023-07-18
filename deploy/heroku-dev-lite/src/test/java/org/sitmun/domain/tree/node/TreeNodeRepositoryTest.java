package org.sitmun.domain.tree.node;

import org.assertj.core.api.Assertions;
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
class TreeNodeRepositoryTest {

  @Autowired
  private TreeNodeRepository treeNodeRepository;
  private TreeNode treeNode;

  @BeforeEach
  void init() {
    treeNode = new TreeNode();
  }

  @Test
  void saveTreeNode() {
    Assertions.assertThat(treeNode.getId()).isNull();
    treeNodeRepository.save(treeNode);
    Assertions.assertThat(treeNode.getId()).isNotZero();
  }

  @Test
  void findOneTreeNodeById() {
    Assertions.assertThat(treeNode.getId()).isNull();
    treeNodeRepository.save(treeNode);
    Assertions.assertThat(treeNode.getId()).isNotZero();

    Assertions.assertThat(treeNodeRepository.findById(treeNode.getId())).isNotNull();
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
