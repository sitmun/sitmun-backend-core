package org.sitmun.domain.cartography.permission;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
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

@DataJpaTest
@DisplayName("Cartography Permission Repository JPA test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CartographyPermissionRepositoryTest {
  @Autowired private CartographyPermissionRepository cartographyPermissionRepository;

  @Autowired private RoleRepository roleRepository;

  @Test
  @DisplayName("Find permissions by username, application and territory")
  void findPermissionsByUsernameApplicationAndTerritory() {
    List<Role> roles = roleRepository.findRolesByApplicationAndUserAndTerritory("public", 1, 1);
    List<CartographyPermission> cp =
        cartographyPermissionRepository.findByRolesAndTerritory(roles, 1);
    assertThat(cp).hasSize(3);
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
