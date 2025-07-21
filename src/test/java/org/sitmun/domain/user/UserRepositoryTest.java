package org.sitmun.domain.user;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.domain.territory.type.TerritoryType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

@SpringBootTest
@DisplayName("User Repository JPA Test")
class UserRepositoryTest {

  @Autowired private UserRepository userRepository;

  private User user;

  @BeforeEach
  void init() {
    TerritoryType type = TerritoryType.builder().build();
    type.setName("tipo Territorio 1");

    user = new User();
    user.setFirstName("Admin");
    user.setLastName("AdminLastName");
    user.setAdministrator(true);
    user.setBlocked(false);
    user.setPassword("prCTmrOYKHQ=");
    user.setUsername("admin-new");
    user.setPositions(null);
    user.setPermissions(null);
  }

  @AfterEach
  @WithMockUser("admin")
  void cleanup() {
    userRepository.delete(user);
  }

  @Test
  @WithMockUser("admin")
  @DisplayName("Save a new user to database")
  void saveUser() {
    Assertions.assertThat(user.getId()).isNull();
    userRepository.save(user);
    Assertions.assertThat(user.getId()).isNotZero();
    Assertions.assertThat(user.getCreatedDate()).isNotNull();
  }

  @Test
  @WithMockUser("admin")
  @DisplayName("Find a user by its ID")
  void findOneUserById() {
    Assertions.assertThat(user.getId()).isNull();
    userRepository.save(user);
    Assertions.assertThat(user.getId()).isNotZero();

    Assertions.assertThat(userRepository.findById(user.getId())).isNotNull();
  }
}
