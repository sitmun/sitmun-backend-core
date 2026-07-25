package org.sitmun.domain.tree;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.domain.application.tree.ApplicationTreeRepository;
import org.sitmun.domain.role.Role;
import org.sitmun.domain.role.RoleRepository;
import org.sitmun.infrastructure.persistence.type.i18n.I18nTestConfiguration;
import org.sitmun.infrastructure.security.core.SecurityConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

@DataJpaTest
@DisplayName("Tree Repository JPA Test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TreeRepositoryTest {

  @Autowired private TreeRepository treeRepository;

  @Autowired private ApplicationTreeRepository applicationTreeRepository;

  @Autowired private RoleRepository roleRepository;

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
    List<Role> roles =
        roleRepository.findRolesByApplicationAndUserAndTerritory(
            SecurityConstants.PUBLIC_PRINCIPAL, 1, 1);
    List<Tree> tr =
        applicationTreeRepository.findByAppAndRoles(1, roles).stream()
            .map(OrderedTree::tree)
            .toList();
    // Seed oracle for app/ter 1: size ≠ 2 means another test polluted Liquibase seed — fix that
    // class; do not weaken this assert.
    assertThat(tr).hasSize(2);
  }

  @TestConfiguration
  @Import(I18nTestConfiguration.class)
  static class Configuration {}
}
