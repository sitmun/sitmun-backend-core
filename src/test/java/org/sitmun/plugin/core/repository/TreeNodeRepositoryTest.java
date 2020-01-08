package org.sitmun.plugin.core.repository;

import static org.assertj.core.api.Assertions.assertThat;


import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sitmun.plugin.core.domain.TreeNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
public class TreeNodeRepositoryTest {

  @Autowired
  private TreeNodeRepository treeNodeRepository;

  private TreeNode treeNode;

  @Before
  public void init() {
    treeNode = new TreeNode();
  }

  @Test
  public void saveTreeNode() {
    assertThat(treeNode.getId()).isNull();
    treeNodeRepository.save(treeNode);
    assertThat(treeNode.getId()).isNotZero();
  }

  @Test
  public void findOneTreeNodeById() {
    assertThat(treeNode.getId()).isNull();
    treeNodeRepository.save(treeNode);
    assertThat(treeNode.getId()).isNotZero();

    assertThat(treeNodeRepository.findOne(treeNode.getId())).isNotNull();
  }

}
