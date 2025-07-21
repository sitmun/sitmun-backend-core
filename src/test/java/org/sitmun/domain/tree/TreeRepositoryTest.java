package org.sitmun.domain.tree;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.domain.role.Role;
import org.sitmun.domain.role.RoleRepository;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


@DataJpaTest
@DisplayName("Tree Repository JPA Test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TreeRepositoryTest {

  @Autowired
  private TreeRepository treeRepository;

  @Autowired
  private RoleRepository roleRepository;

  private Tree tree;

  @BeforeEach
  void init() {
    tree = new Tree();
    tree.setName("Test");

  }

  @Test
  @DisplayName("Save a new tree to database")
  void saveTerritory() {
    Assertions.assertThat(tree.getId()).isNull();
    treeRepository.save(tree);
    Assertions.assertThat(tree.getId()).isNotZero();
  }

  @Test
  @DisplayName("Find a tree by its ID")
  void findOneTerritoryById() {
    Assertions.assertThat(tree.getId()).isNull();
    treeRepository.save(tree);
    Assertions.assertThat(tree.getId()).isNotZero();

    Assertions.assertThat(treeRepository.findById(tree.getId())).isNotNull();
  }

  @Test
  @DisplayName("Find trees by roles and territory")
  void findTreesByRolesAndTerritory() {
    List<Role> roles = roleRepository.findRolesByApplicationAndUserAndTerritory("public", 1, 1);
    List<Tree> tr = treeRepository.findByAppAndRoles(1, roles);
    assertThat(tr).hasSize(1);
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
