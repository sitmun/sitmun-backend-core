package org.sitmun.domain.user.position;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@AutoConfigureWebMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Transactional(rollbackFor = Exception.class)
@Rollback
@DisplayName("User Position Repository Data REST Test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserPositionRepositoryDataRestTest {

  @Autowired private WebApplicationContext webApplicationContext;

  @Autowired private UserPositionRepository userPositionRepository;

  @Autowired private UserRepository userRepository;

  @Autowired private TerritoryRepository territoryRepository;

  @Autowired private RoleRepository roleRepository;

  @Autowired private TerritoryTypeRepository territoryTypeRepository;

  private MockMvc mockMvc;
  private User user;
  private Territory territory;
  private Role role;
  private TerritoryType territoryType;
  private final List<UserPosition> createdUserPositions = new ArrayList<>();

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

    // Create test territory type
    territoryType =
        TerritoryType.builder()
            .name("Test Territory Type")
            .official(false)
            .topType(false)
            .bottomType(false)
            .build();
    territoryType = territoryTypeRepository.save(territoryType);

    // Create test user with unique username
    String uniqueUsername =
        "testuser" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    user =
        User.builder()
            .username(uniqueUsername)
            .password("testpassword")
            .firstName("Test")
            .lastName("User")
            .email("test@example.com")
            .administrator(false)
            .blocked(false)
            .build();
    user = userRepository.save(user);

    // Create test territory with unique name
    String uniqueTerritoryName =
        "Test Territory "
            + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    territory =
        Territory.builder()
            .name(uniqueTerritoryName)
            .code("TEST" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 10))
            .type(territoryType)
            .blocked(false)
            .build();
    territory = territoryRepository.save(territory);

    // Create test role with unique name
    String uniqueRoleName =
        "Test Role " + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    role = Role.builder().name(uniqueRoleName).description("Test role description").build();
    role = roleRepository.save(role);
  }

  @AfterEach
  void tearDown() {
    // Clean up specific UserPosition entities created during the test
    for (UserPosition userPosition : createdUserPositions) {
      if (userPosition.getId() != null) {
        userPositionRepository.deleteById(userPosition.getId());
      }
    }
    createdUserPositions.clear();

    // Then clean up test data in reverse dependency order
    if (role != null) {
      roleRepository.deleteById(role.getId());
    }
    if (territory != null) {
      territoryRepository.deleteById(territory.getId());
    }
    if (user != null) {
      userRepository.deleteById(user.getId());
    }
    if (territoryType != null) {
      territoryTypeRepository.deleteById(territoryType.getId());
    }
  }

  @Test
  @DisplayName("POST: Create user position when user configuration is created via REST")
  void shouldCreateUserPositionWhenUserConfigurationIsCreatedViaRest() throws Exception {
    // Ensure no UserPosition exists initially
    List<UserPosition> existingPosition =
        userPositionRepository.findByUserAndTerritory(user, territory);
    Assertions.assertThat(existingPosition).isEmpty();

    // Create UserConfiguration via REST API
    String userConfigurationJson =
        "{\"user\":\"http://localhost/api/users/%d\",\"territory\":\"http://localhost/api/territories/%d\",\"role\":\"http://localhost/api/roles/%d\",\"appliesToChildrenTerritories\":false}"
            .formatted(user.getId(), territory.getId(), role.getId());

    mockMvc
        .perform(
            post("/api/user-configurations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(userConfigurationJson))
        .andExpect(status().isCreated());

    // Verify UserPosition was created by the business logic
    List<UserPosition> createdPosition =
        userPositionRepository.findByUserAndTerritory(user, territory);
    Assertions.assertThat(createdPosition).isNotEmpty();
    UserPosition position = createdPosition.get(0);
    Assertions.assertThat(position.getUser()).isEqualTo(user);
    Assertions.assertThat(position.getTerritory()).isEqualTo(territory);
    createdUserPositions.add(position);
  }

  /**
   * TODO: In runtime, it is possible to create multiple positions via REST
   */
  @Test
  @DisplayName("POST: Prevent duplicate user position when user configuration is updated via REST")
  void shouldNotCreateDuplicateUserPositionWhenUserConfigurationIsUpdatedViaRest()
      throws Exception {
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
    createdUserPositions.add(existingPosition);

    long initialCount = userPositionRepository.count();

    // Update UserConfiguration via REST API (simulate update by creating again)
    String userConfigurationJson =
        "{\"user\":\"http://localhost/api/users/%d\",\"territory\":\"http://localhost/api/territories/%d\",\"role\":\"http://localhost/api/roles/%d\",\"appliesToChildrenTerritories\":true}"
            .formatted(user.getId(), territory.getId(), role.getId());

    mockMvc
        .perform(
            post("/api/user-configurations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(userConfigurationJson))
        .andExpect(status().isCreated());

    // Verify no duplicate UserPosition was created
    long finalCount = userPositionRepository.count();
    Assertions.assertThat(finalCount).isEqualTo(initialCount);

    // Verify the existing position is still there
    List<UserPosition> foundPosition =
        userPositionRepository.findByUserAndTerritory(user, territory);
    Assertions.assertThat(foundPosition).isNotEmpty();
    Assertions.assertThat(foundPosition.get(0).getId()).isEqualTo(existingPosition.getId());
  }
}
