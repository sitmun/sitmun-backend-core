package org.sitmun.plugin.core.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sitmun.plugin.core.domain.Territory;
import org.sitmun.plugin.core.domain.User;
import org.sitmun.plugin.core.domain.UserPosition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;


@ExtendWith(SpringExtension.class)
@DataJpaTest
@ActiveProfiles("dev")
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
}
