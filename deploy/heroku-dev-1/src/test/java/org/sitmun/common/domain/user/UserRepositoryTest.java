package org.sitmun.common.domain.user;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sitmun.common.domain.role.Role;
import org.sitmun.common.domain.territory.Territory;
import org.sitmun.common.domain.territory.type.TerritoryType;
import org.sitmun.common.domain.user.User;
import org.sitmun.common.domain.user.UserRepository;
import org.sitmun.common.domain.user.configuration.UserConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.Date;

@SpringBootTest

public class UserRepositoryTest {

  @Autowired
  private UserRepository userRepository;

  private User user;

  @BeforeEach
  public void init() {
    TerritoryType type = TerritoryType.builder().build();
    type.setName("tipo Territorio 1");

    Territory territory = Territory.builder()
      .name("Admin")
      .blocked(false)
      .territorialAuthorityEmail("email@email.org")
      .createdDate(new Date())
      .territorialAuthorityName("Test")
      .type(type)
      .build();

    user = new User();
    user.setFirstName("Admin");
    user.setLastName("AdminLastName");
    user.setAdministrator(true);
    user.setBlocked(false);
    user.setPassword("prCTmrOYKHQ=");
    user.setUsername("admin-new");
    user.setPositions(null);
    user.setPermissions(null);

    Role role = Role.builder()
      .name("rol-admin")
      .description("rol de administrador")
      .build();
    UserConfiguration conf = UserConfiguration.builder()
      .user(user)
      .role(role)
      .territory(territory)
      .appliesToChildrenTerritories(false)
      .build();

  }

  @AfterEach
  @WithMockUser("admin")
  public void cleanup() {
    userRepository.delete(user);
  }

  @Test
  @WithMockUser("admin")
  public void saveUser() {
    Assertions.assertThat(user.getId()).isNull();
    userRepository.save(user);
    Assertions.assertThat(user.getId()).isNotZero();
    Assertions.assertThat(user.getCreatedDate()).isNotNull();
  }

  @Test
  @WithMockUser("admin")
  public void findOneUserById() {
    Assertions.assertThat(user.getId()).isNull();
    userRepository.save(user);
    Assertions.assertThat(user.getId()).isNotZero();

    Assertions.assertThat(userRepository.findById(user.getId())).isNotNull();
  }

}
