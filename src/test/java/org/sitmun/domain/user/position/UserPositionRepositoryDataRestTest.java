package org.sitmun.domain.user.position;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Date;
import java.util.Optional;
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

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

    // Create and persist TerritoryType with unique name
    String uniqueTypeName = "Test Type " + java.util.UUID.randomUUID();
    territoryType =
        TerritoryType.builder()
            .name(uniqueTypeName)
            .official(false)
            .topType(false)
            .bottomType(false)
            .build();
    territoryType = territoryTypeRepository.save(territoryType);

    // Create test user with unique username (shortened to fit email constraint)
    String uniqueUsername =
        "tu" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 20);
    user = new User();
    user.setFirstName("Test");
    user.setLastName("User");
    user.setAdministrator(false);
    user.setBlocked(false);
    user.setPassword("password");
    user.setUsername(uniqueUsername);
    user.setEmail(uniqueUsername + "@ex.com"); // total length will be <= 50
    user = userRepository.save(user);

    // Create test territory with unique name
    String uniqueTerritoryName =
        "Test Territory "
            + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    territory =
        Territory.builder()
            .name(uniqueTerritoryName)
            .code("Test code")
            .blocked(false)
            .territorialAuthorityEmail("admin@example.com")
            .createdDate(new Date())
            .territorialAuthorityName("Test Authority")
            .type(territoryType)
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
    // Clean up test data
    if (user != null) {
      userRepository.deleteById(user.getId());
    }
    if (territory != null) {
      territoryRepository.deleteById(territory.getId());
    }
    if (role != null) {
      roleRepository.deleteById(role.getId());
    }
    if (territoryType != null) {
      territoryTypeRepository.deleteById(territoryType.getId());
    }
    // Clean up any UserPosition entities created during the test
    userPositionRepository.deleteAll();
  }

  @Test
  @DisplayName("POST: Create user position when user configuration is created via REST")
  void shouldCreateUserPositionWhenUserConfigurationIsCreatedViaRest() throws Exception {
    // Ensure no UserPosition exists initially
    Optional<UserPosition> existingPosition =
        userPositionRepository.findByUserAndTerritory(user, territory);
    Assertions.assertThat(existingPosition).isEmpty();

    // Create UserConfiguration via REST API
    String userConfigurationJson =
        String.format(
            "{\"user\":\"http://localhost/api/users/%d\",\"territory\":\"http://localhost/api/territories/%d\",\"role\":\"http://localhost/api/roles/%d\",\"appliesToChildrenTerritories\":false}",
            user.getId(), territory.getId(), role.getId());

    mockMvc
        .perform(
            post("/api/user-configurations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(userConfigurationJson))
        .andExpect(status().isCreated());

    // Verify UserPosition was created by the business logic
    Optional<UserPosition> createdPosition =
        userPositionRepository.findByUserAndTerritory(user, territory);
    Assertions.assertThat(createdPosition).isPresent();
    UserPosition position = createdPosition.get();
    Assertions.assertThat(position.getUser()).isEqualTo(user);
    Assertions.assertThat(position.getTerritory()).isEqualTo(territory);
  }

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

    long initialCount = userPositionRepository.count();

    // Update UserConfiguration via REST API (simulate update by creating again)
    String userConfigurationJson =
        String.format(
            "{\"user\":\"http://localhost/api/users/%d\",\"territory\":\"http://localhost/api/territories/%d\",\"role\":\"http://localhost/api/roles/%d\",\"appliesToChildrenTerritories\":true}",
            user.getId(), territory.getId(), role.getId());

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
    Optional<UserPosition> foundPosition =
        userPositionRepository.findByUserAndTerritory(user, territory);
    Assertions.assertThat(foundPosition).isPresent();
    Assertions.assertThat(foundPosition.get().getId()).isEqualTo(existingPosition.getId());
  }
}
