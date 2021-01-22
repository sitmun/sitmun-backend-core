package org.sitmun.plugin.core.repository;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sitmun.plugin.core.domain.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@DataJpaTest
public class UserRepositoryTest {

  @Autowired
  private UserRepository userRepository;

  private User user;

  @Before
  public void init() {
    TerritoryType type = new TerritoryType.Builder().build();
    type.setName("tipo Territorio 1");

    Territory territory = Territory.builder()
      .setName("Admin")
      .setBlocked(false)
      .setTerritorialAuthorityEmail("email@email.org")
      .setCreatedDate(new Date())
      .setTerritorialAuthorityName("Test")
      .setType(type)
      .build();

    user = new User();
    user.setFirstName("Admin");
    user.setLastName("AdminLastName");
    user.setAdministrator(true);
    user.setBlocked(false);
    user.setPassword("prCTmrOYKHQ=");
    user.setUsername("admin");
    user.setPositions(null);
    user.setPermissions(null);

    Role role = Role.builder()
      .setName("rol-admin")
      .setDescription("rol de administrador")
      .build();
    UserConfiguration conf = new UserConfiguration();
    conf.setUser(user);
    conf.setRole(role);
    conf.setTerritory(territory);

  }

  @Test
  public void saveUser() {
    assertThat(user.getId()).isNull();
    userRepository.save(user);
    assertThat(user.getId()).isNotZero();
  }

  @Test
  public void findOneUserById() {
    assertThat(user.getId()).isNull();
    userRepository.save(user);
    assertThat(user.getId()).isNotZero();

    assertThat(userRepository.findById(user.getId())).isNotNull();
  }

}
