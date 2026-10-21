package org.sitmun.domain.cartography.permission;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
@DisplayName("Cartography Permission Repository JPA test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CartographyPermissionRepositoryTest {
  @Autowired private CartographyPermissionRepository cartographyPermissionRepository;

  @Autowired private RoleRepository roleRepository;

  @Test
  @DisplayName("Find permissions by username, application and territory")
  void findPermissionsByUsernameApplicationAndTerritory() {
    List<Role> roles =
        roleRepository.findRolesByApplicationAndUserAndTerritory(
            SecurityConstants.PUBLIC_PRINCIPAL, 1, 1);
    List<CartographyPermission> cp =
        cartographyPermissionRepository.findByRolesAndTerritory(roles, 1);
    assertThat(cp).hasSize(3);
  }
}
