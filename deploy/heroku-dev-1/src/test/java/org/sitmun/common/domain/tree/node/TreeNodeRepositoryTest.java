package org.sitmun.common.domain.tree.node;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sitmun.common.domain.tree.node.TreeNode;
import org.sitmun.common.domain.tree.node.TreeNodeRepository;
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

public class TreeNodeRepositoryTest {

  @Autowired
  private TreeNodeRepository treeNodeRepository;
  private TreeNode treeNode;

  @BeforeEach
  public void init() {
    treeNode = new TreeNode();
  }

  @Test
  public void saveTreeNode() {
    Assertions.assertThat(treeNode.getId()).isNull();
    treeNodeRepository.save(treeNode);
    Assertions.assertThat(treeNode.getId()).isNotZero();
  }

  @Test
  public void findOneTreeNodeById() {
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
    public TaskExecutor taskExecutor() {
      return new SyncTaskExecutor();
    }
  }

}
