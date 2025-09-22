package org.sitmun.domain.user.position;

import java.util.Date;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.domain.role.Role;
import org.sitmun.domain.role.RoleRepository;
import org.sitmun.domain.territory.Territory;
import org.sitmun.domain.territory.TerritoryRepository;
import org.sitmun.domain.territory.type.TerritoryType;
import org.sitmun.domain.territory.type.TerritoryTypeRepository;
import org.sitmun.domain.user.User;
import org.sitmun.domain.user.UserRepository;
import org.sitmun.domain.user.configuration.UserConfiguration;
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
import org.springframework.test.annotation.DirtiesContext;

@DataJpaTest
@Import({UserPositionBusinessLogic.class, LiquibaseConfig.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("User Position Business Logic JPA Test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserPositionBusinessLogicTest {

  @Autowired private UserPositionBusinessLogic userPositionBusinessLogic;

  @Autowired private UserPositionRepository userPositionRepository;

  @Autowired private UserRepository userRepository;

  @Autowired private TerritoryRepository territoryRepository;

  @Autowired private RoleRepository roleRepository;

  @Autowired private TerritoryTypeRepository territoryTypeRepository;

  private User user;
  private Territory territory;
  private Role role;
  private UserConfiguration userConfiguration;
  private long initialUserPositionCount;

  @BeforeEach
  void setUp() {
    // Create and persist TerritoryType with all required fields
    TerritoryType type =
        TerritoryType.builder()
            .name("Test Type")
            .official(false)
            .topType(false)
            .bottomType(false)
            .build();
    type = territoryTypeRepository.save(type);

    // Create test user
    user = new User();
    user.setFirstName("Test");
    user.setLastName("User");
    user.setAdministrator(false);
    user.setBlocked(false);
    user.setPassword("password");
    user.setUsername("testuser-position-" + System.currentTimeMillis());
    user.setEmail("test@example.com");
    user = userRepository.save(user);

    // Create test territory with managed TerritoryType and all required fields (no code)
    territory =
        Territory.builder()
            .name("Test Territory")
            .code("Test code")
            .blocked(false)
            .territorialAuthorityEmail("admin@example.com")
            .createdDate(new Date())
            .territorialAuthorityName("Test Authority")
            .type(type)
            .build();
    territory = territoryRepository.save(territory);

    // Create test role
    role = Role.builder().name("Test Role").description("Test role description").build();
    role = roleRepository.save(role);

    // Create test user configuration
    userConfiguration =
        UserConfiguration.builder()
            .user(user)
            .territory(territory)
            .role(role)
            .appliesToChildrenTerritories(false)
            .build();
    // Record initial UserPosition count
    initialUserPositionCount = userPositionRepository.count();
  }

  @AfterEach
  void tearDown() {
    // Ensure database content is the same after each test
    long finalUserPositionCount = userPositionRepository.count();
    Assertions.assertThat(finalUserPositionCount).isEqualTo(initialUserPositionCount);
  }

  @Test
  @DisplayName("Create user position when user configuration is provided")
  void createUserPositionWithUserConfiguration() {
    userPositionRepository.deleteAll(userPositionRepository
      .findByUserAndTerritory(user, territory));
    Assertions.assertThat(userPositionRepository.findByUserAndTerritory(user, territory)).isEmpty();

    // Call the business logic method directly
    userPositionBusinessLogic.createUserPositionIfNotExists(userConfiguration);

    // Verify UserPosition was created
    List<UserPosition> createdPosition =
        userPositionRepository.findByUserAndTerritory(user, territory);
    Assertions.assertThat(createdPosition).isNotEmpty();
    UserPosition position = createdPosition.get(0);
    Assertions.assertThat(position.getUser()).isEqualTo(user);
    Assertions.assertThat(position.getTerritory()).isEqualTo(territory);
    // Clean up
    userPositionRepository.delete(position);
  }

  @Test
  @DisplayName("Prevent duplicate user position creation")
  void preventDuplicateUserPositionCreation() {
    // Create a UserPosition manually first
    UserPosition existingPosition =
        UserPosition.builder()
            .user(user)
            .territory(territory)
            .name("Existing Position")
            .organization("Existing Organization")
            .email("existing@example.com")
            .createdDate(new Date())
            .build();
    existingPosition = userPositionRepository.save(existingPosition);

    // Call the business logic method directly
    userPositionBusinessLogic.createUserPositionIfNotExists(userConfiguration);

    // Verify no duplicate was created
    long positionCount = userPositionRepository.findAll().spliterator().getExactSizeIfKnown();
    Assertions.assertThat(positionCount).isEqualTo(initialUserPositionCount + 1);
    // Clean up
    userPositionRepository.delete(existingPosition);
  }

  @Test
  @DisplayName("Handle null user gracefully without throwing exception")
  void handleNullUserGracefully() {
    UserConfiguration configWithNullUser =
        UserConfiguration.builder()
            .user(null)
            .territory(territory)
            .role(role)
            .appliesToChildrenTerritories(false)
            .build();
    Assertions.assertThatCode(
            () -> userPositionBusinessLogic.createUserPositionIfNotExists(configWithNullUser))
        .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("Handle null territory gracefully without throwing exception")
  void handleNullTerritoryGracefully() {
    UserConfiguration configWithNullTerritory =
        UserConfiguration.builder()
            .user(user)
            .territory(null)
            .role(role)
            .appliesToChildrenTerritories(false)
            .build();
    Assertions.assertThatCode(
            () -> userPositionBusinessLogic.createUserPositionIfNotExists(configWithNullTerritory))
        .doesNotThrowAnyException();
  }

  @TestConfiguration
  static class Configuration {
    @Bean
    @Primary
    TaskExecutor taskExecutor() {
      return new SyncTaskExecutor();
    }
  }
}
