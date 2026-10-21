package org.sitmun.domain.tree;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.domain.application.tree.ApplicationTreeRepository;
import org.sitmun.domain.role.Role;
import org.sitmun.domain.role.RoleRepository;
import org.sitmun.infrastructure.persistence.type.i18n.I18nTestConfiguration;
import org.sitmun.infrastructure.security.core.SecurityConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(I18nTestConfiguration.class)
@DisplayName("Tree Repository JPA Test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TreeRepositoryTest {

  @Autowired private ApplicationTreeRepository applicationTreeRepository;

  @Autowired private RoleRepository roleRepository;

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
}
