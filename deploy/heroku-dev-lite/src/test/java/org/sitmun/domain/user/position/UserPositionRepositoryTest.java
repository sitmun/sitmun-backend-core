package org.sitmun.domain.user.position;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sitmun.domain.territory.Territory;
import org.sitmun.domain.territory.TerritoryRepository;
import org.sitmun.domain.user.User;
import org.sitmun.domain.user.UserRepository;
import org.sitmun.infrastructure.persistence.config.LiquibaseConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Date;


@ExtendWith(SpringExtension.class)
@DataJpaTest

class UserPositionRepositoryTest {

  @Autowired
  private UserPositionRepository userPositionRepository;
  @Autowired
  private UserRepository userRepository;
  @Autowired
  private TerritoryRepository territorioRepository;
  private UserPosition userPosition;

  @BeforeEach
  void init() {

    User user = new User();
    user.setFirstName("Admin");
    user.setLastName("Admin");
    user.setAdministrator(true);
    user.setBlocked(false);
    user.setPassword("prCTmrOYKHQ=");
    user.setUsername("admin");
    user.setPositions(null);
    user.setPermissions(null);
    userRepository.save(user);

    Territory territory = Territory.builder()
      .name("Admin")
      .blocked(false)
      .territorialAuthorityEmail("email@email.org")
      .createdDate(new Date())
      .territorialAuthorityName("Test")
      .build();
    territorioRepository.save(territory);

    userPosition = UserPosition.builder()
      .name("Test")
      .createdDate(new Date())
      .organization("Test")
      .territory(territory)
      .user(user)
      .build();

  }

  @Test
  void saveUserPosition() {
    Assertions.assertThat(userPosition.getId()).isNull();
    userPositionRepository.save(userPosition);
    Assertions.assertThat(userPosition.getId()).isNotZero();
  }

  @Test
  void findOneUserPositionById() {
    Assertions.assertThat(userPosition.getId()).isNull();
    userPositionRepository.save(userPosition);
    Assertions.assertThat(userPosition.getId()).isNotZero();

    Assertions.assertThat(userPositionRepository.findById(userPosition.getId())).isNotNull();
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
