package org.sitmun.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sitmun.common.domain.territory.Territory;
import org.sitmun.common.domain.territory.TerritoryRepository;
import org.sitmun.common.domain.user.User;
import org.sitmun.common.domain.user.UserRepository;
import org.sitmun.common.domain.user.position.UserPosition;
import org.sitmun.common.domain.user.position.UserPositionRepository;
import org.sitmun.legacy.config.LiquibaseConfig;
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

import static org.assertj.core.api.Assertions.assertThat;


@ExtendWith(SpringExtension.class)
@DataJpaTest

public class UserPositionRepositoryTest {

  @Autowired
  private UserPositionRepository userPositionRepository;
  @Autowired
  private UserRepository userRepository;
  @Autowired
  private TerritoryRepository territorioRepository;
  private UserPosition userPosition;

  @BeforeEach
  public void init() {

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
  public void saveUserPosition() {
    assertThat(userPosition.getId()).isNull();
    userPositionRepository.save(userPosition);
    assertThat(userPosition.getId()).isNotZero();
  }

  @Test
  public void findOneUserPositionById() {
    assertThat(userPosition.getId()).isNull();
    userPositionRepository.save(userPosition);
    assertThat(userPosition.getId()).isNotZero();

    assertThat(userPositionRepository.findById(userPosition.getId())).isNotNull();
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
