package org.sitmun.domain.tree.node;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.infrastructure.persistence.config.LiquibaseConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

@DataJpaTest
@DisplayName("Tree Node Repository JPA Test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TreeNodeRepositoryTest {

  @Autowired
  private TreeNodeRepository treeNodeRepository;
  private TreeNode treeNode;

  @BeforeEach
  void init() {
    treeNode = new TreeNode();
  }

  @Test
  @DisplayName("Save a new tree node to database")
  void saveTreeNode() {
    Assertions.assertThat(treeNode.getId()).isNull();
    treeNodeRepository.save(treeNode);
    Assertions.assertThat(treeNode.getId()).isNotZero();
  }

  @Test
  @DisplayName("Find a tree node by its ID")
  void findTreeNodeById() {
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
