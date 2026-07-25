package org.sitmun.domain.user;

import static org.junit.jupiter.api.Assertions.*;
import static org.sitmun.test.TestUtils.asJsonString;
import static org.sitmun.test.URIConstants.*;
import static org.springframework.http.MediaType.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.domain.role.Role;
import org.sitmun.domain.role.RoleRepository;
import org.sitmun.domain.territory.Territory;
import org.sitmun.domain.territory.TerritoryRepository;
import org.sitmun.domain.user.configuration.UserConfiguration;
import org.sitmun.domain.user.configuration.UserConfigurationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.hateoas.MediaTypes;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * HTTP / Data REST contracts for users. Handler rules live in {@link UserEventHandlerTest}.
 * Fixtures are committed with a pre-encoded password (plain {@code save} does not fire
 * {@code @HandleBeforeCreate}); cleaned in {@code @AfterEach}. No class {@code @Transactional} or
 * {@code @DirtiesContext} for DB cleanup.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("User Resource Test")
class UserResourceTest {

  private static final String USER_PASSWORD = "admin";
  private static final String USER_FIRSTNAME = "Admin";
  private static final String USER_CHANGEDFIRSTNAME = "Administrator";
  private static final String USER_LASTNAME = "Admin";
  private static final String USER_CHANGEDLASTNAME = "Territory 1";
  private static final Boolean USER_BLOCKED = false;
  private static final Boolean USER_ADMINISTRATOR = true;
  @Autowired UserRepository userRepository;
  @Autowired UserConfigurationRepository userConfigurationRepository;
  @Autowired RoleRepository roleRepository;
  @Autowired TerritoryRepository territoryRepository;
  @Autowired PasswordEncoder passwordEncoder;
  @Autowired private MockMvc mockMvc;
  private User organizacionAdmin;

  private Role organizacionAdminRole;
  private Role territorialRole;

  private ArrayList<Territory> territories;
  private ArrayList<User> users;
  private ArrayList<UserConfiguration> userConfigurations;

  private String territory1AdminUsername;
  private String territory1UserUsername;
  private String territory2UserUsername;

  @BeforeEach
  void init() {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    territory1AdminUsername = "territory1-admin-" + suffix;
    territory1UserUsername = "territory1-user-" + suffix;
    territory2UserUsername = "territory2-user-" + suffix;
    String encodedPassword = passwordEncoder.encode(USER_PASSWORD);

    organizacionAdminRole = Role.builder().name("ADMIN_ORGANIZACION-" + suffix).build();
    roleRepository.save(organizacionAdminRole);

    territorialRole = Role.builder().name("USUARIO_TERRITORIAL-" + suffix).build();
    roleRepository.save(territorialRole);

    territories = new ArrayList<>();
    users = new ArrayList<>();
    Territory territory1 =
        Territory.builder()
            .name("Territorio 1-" + suffix)
            .code("TERRITORY1-" + suffix)
            .blocked(false)
            .build();

    Territory territory2 =
        Territory.builder()
            .name("Territorio 2-" + suffix)
            .code("TERRITORY2-" + suffix)
            .blocked(false)
            .build();
    territories.add(territory1);
    territories.add(territory2);

    territoryRepository.saveAll(territories);

    organizacionAdmin =
        User.builder()
            .administrator(USER_ADMINISTRATOR)
            .blocked(USER_BLOCKED)
            .firstName(USER_FIRSTNAME)
            .lastName(USER_LASTNAME)
            .password(encodedPassword)
            .username(territory1AdminUsername)
            .build();

    organizacionAdmin = userRepository.save(organizacionAdmin);
    users.add(organizacionAdmin);

    User territory1User =
        User.builder()
            .administrator(false)
            .blocked(USER_BLOCKED)
            .firstName(USER_FIRSTNAME)
            .lastName(USER_LASTNAME)
            .password(encodedPassword)
            .username(territory1UserUsername)
            .build();

    territory1User = userRepository.save(territory1User);
    users.add(territory1User);

    User territory2User =
        User.builder()
            .administrator(false)
            .blocked(USER_BLOCKED)
            .firstName(USER_FIRSTNAME)
            .lastName(USER_LASTNAME)
            .password(encodedPassword)
            .username(territory2UserUsername)
            .build();
    territory2User = userRepository.save(territory2User);
    users.add(territory2User);

    userConfigurations = new ArrayList<>();

    UserConfiguration userConf =
        UserConfiguration.builder()
            .territory(territory1)
            .role(organizacionAdminRole)
            .user(organizacionAdmin)
            .appliesToChildrenTerritories(false)
            .build();
    userConfigurations.add(userConf);

    userConf =
        UserConfiguration.builder()
            .territory(territory1)
            .role(territorialRole)
            .user(territory1User)
            .appliesToChildrenTerritories(false)
            .build();
    userConfigurations.add(userConf);

    userConf =
        UserConfiguration.builder()
            .territory(territory2)
            .role(territorialRole)
            .user(territory2User)
            .appliesToChildrenTerritories(false)
            .build();
    userConfigurations.add(userConf);

    userConfigurationRepository.saveAll(userConfigurations);
  }

  @AfterEach
  void cleanup() {
    if (userConfigurations != null) {
      userConfigurations.forEach(
          conf -> {
            if (conf.getId() != null) {
              userConfigurationRepository.deleteById(conf.getId());
            }
          });
    }
    if (users != null) {
      users.forEach(
          user -> {
            if (user.getId() != null) {
              userRepository.deleteById(user.getId());
            }
          });
    }
    if (organizacionAdminRole != null && organizacionAdminRole.getId() != null) {
      roleRepository.deleteById(organizacionAdminRole.getId());
    }
    if (territorialRole != null && territorialRole.getId() != null) {
      roleRepository.deleteById(territorialRole.getId());
    }
    if (territories != null) {
      territories.forEach(
          territory -> {
            if (territory.getId() != null) {
              territoryRepository.deleteById(territory.getId());
            }
          });
    }
  }

  @Test
  @DisplayName("POST: Create a new user")
  @WithMockUser(roles = "ADMIN")
  void createUser() throws Exception {
    String username = "new-user-" + UUID.randomUUID().toString().substring(0, 8);
    String content =
        """
        {
        "username":"%s",
        "firstName":"new name",
        "lastName":"new name",
        "password":"new password",
        "administrator": false,
        "blocked": false
        }"""
            .formatted(username);

    String uri =
        mockMvc
            .perform(post(USER_URI).contentType(APPLICATION_JSON).content(content))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getHeader("Location");

    Assertions.assertThat(uri).isNotNull();

    mockMvc
        .perform(get(uri))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaTypes.HAL_JSON))
        .andExpect(jsonPath("$.username", CoreMatchers.equalTo(username)))
        .andExpect(jsonPath("$.passwordSet").value(true));

    mockMvc.perform(delete(uri)).andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("PUT: Reject empty password on an existing user")
  @WithMockUser(roles = "ADMIN")
  void rejectEmptyPasswordOnUpdate() throws Exception {
    String username = "new-user-" + UUID.randomUUID().toString().substring(0, 8);
    String content =
        """
        {
        "username":"%s",
        "firstName":"new name",
        "lastName":"new name",
        "password":"new password",
        "administrator": false,
        "blocked": false
        }"""
            .formatted(username);

    String uri =
        mockMvc
            .perform(post(USER_URI).contentType(APPLICATION_JSON).content(content))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getHeader("Location");

    Assertions.assertThat(uri).isNotNull();

    mockMvc
        .perform(get(uri))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaTypes.HAL_JSON))
        .andExpect(jsonPath("$.passwordSet").value(true));

    String withNullPassword =
        """
        {
        "username":"%s",
        "firstName":"new name",
        "lastName":"new name",
        "password": null,
        "administrator": false,
        "blocked": false
        }"""
            .formatted(username);

    mockMvc
        .perform(put(uri).content(withNullPassword))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaTypes.HAL_JSON))
        .andExpect(jsonPath("$.passwordSet").value(true));

    String withoutField =
        """
        {
        "username":"%s",
        "firstName":"new name",
        "lastName":"new name",
        "administrator": false,
        "blocked": false
        }"""
            .formatted(username);

    mockMvc
        .perform(put(uri).content(withoutField))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaTypes.HAL_JSON))
        .andExpect(jsonPath("$.passwordSet").value(true));

    String withEmptyPassword =
        """
        {
        "username":"%s",
        "firstName":"new name",
        "lastName":"new name",
        "password": "",
        "administrator": false,
        "blocked": false
        }"""
            .formatted(username);

    mockMvc.perform(put(uri).content(withEmptyPassword)).andExpect(status().isBadRequest());

    mockMvc
        .perform(get(uri))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.passwordSet").value(true));

    String withNonEmptyPassword =
        """
        {
        "username":"%s",
        "firstName":"new name",
        "lastName":"new name",
        "password": "some value",
        "administrator": false,
        "blocked": false
        }"""
            .formatted(username);

    mockMvc
        .perform(put(uri).content(withNonEmptyPassword))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaTypes.HAL_JSON))
        .andExpect(jsonPath("$.passwordSet").value(true));

    mockMvc.perform(delete(uri)).andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("POST: Create a duplicated user fails")
  @WithMockUser(roles = "ADMIN")
  void createDuplicatedUserFails() throws Exception {
    User newUser = organizacionAdmin.toBuilder().id(null).build();

    mockMvc
        .perform(post(USER_URI).contentType(APPLICATION_JSON).content(asJsonString(newUser)))
        .andExpect(status().isConflict());
  }

  @Test
  @DisplayName("POST: User update")
  @WithMockUser(roles = "ADMIN")
  void updateUser() throws Exception {
    String content =
        """
        {
        "username":"user",
        "firstName":"%s",
        "lastName":"%s",
        "administrator": false,
        "blocked": false
        }
        """
            .formatted(USER_CHANGEDFIRSTNAME, USER_CHANGEDLASTNAME);

    mockMvc
        .perform(
            put(USER_URI + "/" + organizacionAdmin.getId())
                .contentType(APPLICATION_JSON)
                .content(content))
        .andExpect(status().isOk());

    mockMvc
        .perform(get(USER_URI + "/" + organizacionAdmin.getId()))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaTypes.HAL_JSON))
        .andExpect(jsonPath("$.firstName", CoreMatchers.equalTo(USER_CHANGEDFIRSTNAME)))
        .andExpect(jsonPath("$.lastName", CoreMatchers.equalTo(USER_CHANGEDLASTNAME)))
        .andExpect(jsonPath("$.passwordSet").value(true));
  }

  @Test
  @DisplayName("GET: Get users as SITMUN administrator")
  @WithMockUser(roles = "ADMIN")
  void getUsersAsSitmunAdmin() throws Exception {
    mockMvc
        .perform(get(USER_URI + "?size=100"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaTypes.HAL_JSON))
        .andExpect(
            jsonPath(
                "$._embedded.users[*].username",
                Matchers.hasItems(
                    territory1AdminUsername, territory1UserUsername, territory2UserUsername)));
  }

  @Test
  @DisplayName("PUT: Update password")
  @WithMockUser(roles = "ADMIN")
  void updatePassword() throws Exception {
    String content =
        """
        {
        "username":"user",
        "firstName":"NameChanged",
        "lastName":"NameChanged",
        "password":"new-password",
        "administrator": false,
        "blocked": false
        }
        """;

    mockMvc
        .perform(
            put(USER_URI + "/" + organizacionAdmin.getId())
                .contentType(APPLICATION_JSON)
                .content(content))
        .andExpect(status().isOk());

    String oldPassword = organizacionAdmin.getPassword();
    assertNotNull(oldPassword);
    Optional<User> updatedUser = userRepository.findById(organizacionAdmin.getId());
    assertTrue(updatedUser.isPresent());
    assertNotEquals(oldPassword, updatedUser.get().getPassword());
  }

  @Test
  @DisplayName("PUT: Keep password")
  @WithMockUser(roles = "ADMIN")
  void keepPassword() throws Exception {
    String content =
        """
        {
        "username":"user",
        "firstName":"NameChanged",
        "lastName":"NameChanged",
        "administrator": false,
        "blocked": false
        }""";

    mockMvc
        .perform(
            put(USER_URI + "/" + organizacionAdmin.getId())
                .contentType(APPLICATION_JSON)
                .content(content))
        .andExpect(status().isOk());

    String oldPassword = organizacionAdmin.getPassword();
    assertNotNull(oldPassword);
    Optional<User> updatedUser = userRepository.findById(organizacionAdmin.getId());
    assertTrue(updatedUser.isPresent());
    assertEquals(oldPassword, updatedUser.get().getPassword());
  }
}
