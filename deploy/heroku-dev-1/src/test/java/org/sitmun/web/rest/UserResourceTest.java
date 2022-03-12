package org.sitmun.web.rest;

import org.assertj.core.api.Assertions;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sitmun.common.domain.role.Role;
import org.sitmun.common.domain.role.RoleRepository;
import org.sitmun.common.domain.territory.Territory;
import org.sitmun.common.domain.territory.TerritoryRepository;
import org.sitmun.common.domain.user.User;
import org.sitmun.common.domain.user.UserEventHandler;
import org.sitmun.common.domain.user.UserRepository;
import org.sitmun.common.domain.user.configuration.UserConfiguration;
import org.sitmun.common.domain.user.configuration.UserConfigurationRepository;
import org.sitmun.test.Fixtures;
import org.sitmun.test.URIConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.sitmun.test.TestUtils.asJsonString;
import static org.sitmun.test.TestUtils.withMockSitmunAdmin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc

public class UserResourceTest {

  private static final String TERRITORY1_ADMIN_USERNAME = "territory1-admin";
  private static final String TERRITORY1_USER_USERNAME = "territory1-user";
  private static final String TERRITORY2_USER_USERNAME = "territory2-user";
  private static final String USER_PASSWORD = "admin";
  private static final String USER_FIRSTNAME = "Admin";
  private static final String USER_CHANGEDFIRSTNAME = "Administrator";
  private static final String USER_LASTNAME = "Admin";
  private static final String USER_CHANGEDLASTNAME = "Territory 1";
  private static final Boolean USER_BLOCKED = false;
  private static final Boolean USER_ADMINISTRATOR = true;
  @Autowired
  UserRepository userRepository;
  @Autowired
  UserConfigurationRepository userConfigurationRepository;
  @Autowired
  RoleRepository roleRepository;
  @Autowired
  TerritoryRepository territoryRepository;
  @Autowired
  private MockMvc mockMvc;
  private User organizacionAdmin;
  private User territory1User;
  private User territory2User;

  private Territory territory1;
  private Territory territory2;


  private Role organizacionAdminRole;
  private Role territorialRole;

  private ArrayList<Territory> territories;
  private ArrayList<User> users;
  private ArrayList<UserConfiguration> userConfigurations;

  @Autowired
  private UserEventHandler userEventHandler;

  @BeforeEach
  public void init() {
    withMockSitmunAdmin(() -> {

      organizacionAdminRole =
        Role.builder().name("ADMIN_ORGANIZACION").build();
      roleRepository.save(organizacionAdminRole);

      territorialRole = Role.builder().name("USUARIO_TERRITORIAL").build();
      roleRepository.save(territorialRole);

      territories = new ArrayList<>();
      users = new ArrayList<>();
      territory1 = Territory.builder()
        .name("Territorio 1")
        .code("")
        .blocked(false)
        .build();

      territory2 = Territory.builder()
        .name("Territorio 2")
        .code("")
        .blocked(false)
        .build();
      territories.add(territory1);
      territories.add(territory2);

      territoryRepository.saveAll(territories);

      // Territory 1 Admin
      organizacionAdmin = User.builder()
        .administrator(USER_ADMINISTRATOR)
        .blocked(USER_BLOCKED)
        .firstName(USER_FIRSTNAME)
        .lastName(USER_LASTNAME)
        .password(USER_PASSWORD)
        .username(TERRITORY1_ADMIN_USERNAME)
        .build();

      userEventHandler.handleUserCreate(organizacionAdmin);
      organizacionAdmin = userRepository.save(organizacionAdmin);
      users.add(organizacionAdmin);

      // Territory 1 user
      territory1User = User.builder()
        .administrator(false)
        .blocked(USER_BLOCKED)
        .firstName(USER_FIRSTNAME)
        .lastName(USER_LASTNAME)
        .password(USER_PASSWORD)
        .username(TERRITORY1_USER_USERNAME)
        .build();

      territory1User = userRepository.save(territory1User);
      users.add(territory1User);

      // Territory 2 user
      territory2User = User.builder()
        .administrator(false)
        .blocked(USER_BLOCKED)
        .firstName(USER_FIRSTNAME)
        .lastName(USER_LASTNAME)
        .password(USER_PASSWORD)
        .username(TERRITORY2_USER_USERNAME)
        .build();
      territory2User = userRepository.save(territory2User);
      users.add(territory2User);


      userConfigurations = new ArrayList<>();

      UserConfiguration userConf = UserConfiguration.builder()
        .territory(territory1)
        .role(organizacionAdminRole)
        .user(organizacionAdmin)
        .appliesToChildrenTerritories(false)
        .build();
      userConfigurations.add(userConf);
      this.userConfigurationRepository.save(userConf);

      userConf = UserConfiguration.builder()
        .territory(territory1)
        .role(territorialRole)
        .user(territory1User)
        .appliesToChildrenTerritories(false)
        .build();
      userConfigurations.add(userConf);
      this.userConfigurationRepository.save(userConf);

      userConf = UserConfiguration.builder()
        .territory(territory2)
        .role(territorialRole)
        .user(territory2User)
        .appliesToChildrenTerritories(false)
        .build();
      userConfigurations.add(userConf);

      userConfigurationRepository.saveAll(userConfigurations);
    });
  }

  @AfterEach
  public void cleanup() {
    withMockSitmunAdmin(() -> {
      userConfigurationRepository.deleteAll(userConfigurations);
      roleRepository.delete(territorialRole);
      roleRepository.delete(organizacionAdminRole);
      userRepository.deleteAll(users);
      territoryRepository.deleteAll(territories);
    });
  }


  @Test
  @Disabled("Potential freeze of active connections. @Transactional may be required in REST controllers.")
  public void createNewUserAndDelete() throws Exception {
    String content = "{" +
      "\"username\":\"new user\"," +
      "\"firstName\":\"new name\"," +
      "\"lastName\":\"new name\"," +
      "\"password\":\"new password\"," +
      "\"administrator\": false," +
      "\"blocked\": false}";

    String uri = mockMvc.perform(post(URIConstants.USER_URI)
        .contentType(MediaType.APPLICATION_JSON)
        .content(content)
        .with(user(Fixtures.admin()))
      ).andExpect(status().isCreated())
      .andReturn().getResponse().getHeader("Location");

    Assertions.assertThat(uri).isNotNull();

    mockMvc.perform(get(uri)
        .with(user(Fixtures.admin()))).andExpect(status().isOk())
      .andExpect(content().contentType(MediaTypes.HAL_JSON))
      .andExpect(jsonPath("$.username", CoreMatchers.equalTo("new user")))
      .andExpect(jsonPath("$.passwordSet").value(true));

    mockMvc.perform(delete(uri)
      .with(user(Fixtures.admin()))
    ).andExpect(status().isNoContent());
  }

  @Test
  @Disabled("Potential freeze of active connections. @Transactional may be required in REST controllers.")
  public void clearPassword() throws Exception {
    String content = "{" +
      "\"username\":\"new user\"," +
      "\"firstName\":\"new name\"," +
      "\"lastName\":\"new name\"," +
      "\"password\":\"new password\"," +
      "\"administrator\": false," +
      "\"blocked\": false}";

    String uri = mockMvc.perform(post(URIConstants.USER_URI)
        .contentType(MediaType.APPLICATION_JSON)
        .content(content)
        .with(user(Fixtures.admin()))
      ).andExpect(status().isCreated())
      .andReturn().getResponse().getHeader("Location");

    Assertions.assertThat(uri).isNotNull();

    mockMvc.perform(get(uri)
        .with(user(Fixtures.admin()))).andExpect(status().isOk())
      .andExpect(content().contentType(MediaTypes.HAL_JSON))
      .andExpect(jsonPath("$.passwordSet").value(true));

    String withNullPassword = "{" +
      "\"username\":\"new user\"," +
      "\"firstName\":\"new name\"," +
      "\"lastName\":\"new name\"," +
      "\"password\": null," +
      "\"administrator\": false," +
      "\"blocked\": false}";

    mockMvc.perform(put(uri)
        .content(withNullPassword)
        .with(user(Fixtures.admin()))).andExpect(status().isOk())
      .andExpect(content().contentType(MediaTypes.HAL_JSON))
      .andExpect(jsonPath("$.passwordSet").value(true));

    String withoutField = "{" +
      "\"username\":\"new user\"," +
      "\"firstName\":\"new name\"," +
      "\"lastName\":\"new name\"," +
      "\"administrator\": false," +
      "\"blocked\": false}";

    mockMvc.perform(put(uri)
        .content(withoutField)
        .with(user(Fixtures.admin()))).andExpect(status().isOk())
      .andExpect(content().contentType(MediaTypes.HAL_JSON))
      .andExpect(jsonPath("$.passwordSet").value(true));

    String withEmptyPassword = "{" +
      "\"username\":\"new user\"," +
      "\"firstName\":\"new name\"," +
      "\"lastName\":\"new name\"," +
      "\"password\": \"\"," +
      "\"administrator\": false," +
      "\"blocked\": false}";

    mockMvc.perform(put(uri)
        .content(withEmptyPassword)
        .with(user(Fixtures.admin()))).andExpect(status().isOk())
      .andExpect(content().contentType(MediaTypes.HAL_JSON))
      .andExpect(jsonPath("$.passwordSet").value(false));

    String withNonEmptyPassword = "{" +
      "\"username\":\"new user\"," +
      "\"firstName\":\"new name\"," +
      "\"lastName\":\"new name\"," +
      "\"password\": \"some value\"," +
      "\"administrator\": false," +
      "\"blocked\": false}";

    mockMvc.perform(put(uri)
        .content(withNonEmptyPassword)
        .with(user(Fixtures.admin()))).andExpect(status().isOk())
      .andExpect(content().contentType(MediaTypes.HAL_JSON))
      .andExpect(jsonPath("$.passwordSet").value(true));

    mockMvc.perform(delete(uri)
      .with(user(Fixtures.admin()))
    ).andExpect(status().isNoContent());
  }

  @Test
  @Disabled("Potential freeze of active connections. @Transactional may be required in REST controllers.")
  public void createDuplicatedUserFails() throws Exception {
    User newUser = organizacionAdmin.toBuilder().id(null).build();

    mockMvc.perform(post(URIConstants.USER_URI)
        .contentType(MediaType.APPLICATION_JSON)
        .content(asJsonString(newUser))
        .with(user(Fixtures.admin()))
      )
      .andExpect(status().isConflict());
  }

  @Test
  @Disabled("Potential freeze of active connections. @Transactional may be required in REST controllers.")
  public void updateUser() throws Exception {
    String content = "{" +
      "\"username\":\"user\"," +
      "\"firstName\":\"" + USER_CHANGEDFIRSTNAME + "\"," +
      "\"lastName\":\"" + USER_CHANGEDLASTNAME + "\"," +
      "\"administrator\": false," +
      "\"blocked\": false}";

    mockMvc.perform(put(URIConstants.USER_URI + "/" + organizacionAdmin.getId())
      .contentType(MediaType.APPLICATION_JSON)
      .content(content)
      .with(user(Fixtures.admin()))
    ).andExpect(status().isOk());

    mockMvc.perform(get(URIConstants.USER_URI + "/" + organizacionAdmin.getId())
        .with(user(Fixtures.admin())))
      .andExpect(status().isOk())
      .andExpect(content().contentType(MediaTypes.HAL_JSON))
      .andExpect(jsonPath("$.firstName", CoreMatchers.equalTo(USER_CHANGEDFIRSTNAME)))
      .andExpect(jsonPath("$.lastName", CoreMatchers.equalTo(USER_CHANGEDLASTNAME)))
      .andExpect(jsonPath("$.passwordSet").value(true));
  }

  @Test
  @Disabled("Potential freeze of active connections. @Transactional may be required in REST controllers.")
  public void getUsersAsSitmunAdmin() throws Exception {
    mockMvc.perform(get(URIConstants.USER_URI + "?size=10")
        .with(user(Fixtures.admin()))
      )
      .andExpect(status().isOk())
      .andExpect(content().contentType(MediaTypes.HAL_JSON))
      .andExpect(jsonPath("$._embedded.users", Matchers.hasSize(10)));
  }

  @Deprecated
  @Test
  @Disabled
  public void getUsersAsOrganizationAdmin() throws Exception {
    mockMvc.perform(get(URIConstants.USER_URI))
      .andExpect(status().isOk())
      .andExpect(content().contentType(MediaTypes.HAL_JSON))
      .andExpect(jsonPath("$._embedded.users", Matchers.hasSize(5)));
  }

  @Test
  @Disabled("Potential freeze of active connections. @Transactional may be required in REST controllers.")
  public void updateUserPassword() throws Exception {
    String content = "{" +
      "\"username\":\"user\"," +
      "\"firstName\":\"NameChanged\"," +
      "\"lastName\":\"NameChanged\"," +
      "\"password\":\"new-password\"," +
      "\"administrator\": false," +
      "\"blocked\": false}";

    mockMvc.perform(put(URIConstants.USER_URI + "/" + organizacionAdmin.getId())
      .contentType(MediaType.APPLICATION_JSON)
      .content(content)
      .with(user(Fixtures.admin()))
    ).andExpect(status().isOk());

    String oldPassword = organizacionAdmin.getPassword();
    assertNotNull(oldPassword);
    withMockSitmunAdmin(() -> {
      Optional<User> updatedUser = userRepository.findById(organizacionAdmin.getId());
      assertTrue(updatedUser.isPresent());
      assertNotEquals(oldPassword, updatedUser.get().getPassword());
    });
  }

  @Test
  @Disabled("Potential freeze of active connections. @Transactional may be required in REST controllers.")
  public void keepPassword() throws Exception {
    String content = "{" +
      "\"username\":\"user\"," +
      "\"firstName\":\"NameChanged\"," +
      "\"lastName\":\"NameChanged\"," +
      "\"administrator\": false," +
      "\"blocked\": false}";

    mockMvc.perform(put(URIConstants.USER_URI + "/" + organizacionAdmin.getId())
      .contentType(MediaType.APPLICATION_JSON)
      .content(content)
      .with(user(Fixtures.admin()))
    ).andExpect(status().isOk());

    String oldPassword = organizacionAdmin.getPassword();
    assertNotNull(oldPassword);
    withMockSitmunAdmin(() -> {
      Optional<User> updatedUser = userRepository.findById(organizacionAdmin.getId());
      assertTrue(updatedUser.isPresent());
      assertEquals(oldPassword, updatedUser.get().getPassword());
    });
  }

  @Test
  @Disabled
  public void createNewUserAsOrganizationAdmin() {
    // TODO: Create new user by an organization admin user (ADMIN DE ORGANIZACION)
    // ok is expected. The new user has roles linked to my organization territory
  }

  @Test
  @Disabled
  public void assignRoleToUserAsOrganizationAdmin() {
    // TODO
    // ok is expected. The new user has roles linked to my organization territory
  }

  @Test
  @Disabled
  public void updateUserAsOrganizationAdmin() {
    // TODO
    // Update user (linked to the same organization) by an organization admin user
    // (ADMIN DE ORGANIZACION)
    // ok is expected
  }

  @Test
  @Disabled
  public void updateUserPasswordAsOrganizationAdmin() {
    // TODO
    // Update user password (linked to the same organization) by an organization
    // admin user (ADMIN DE ORGANIZACION)
    // ok is expected
  }

  @Test
  @Disabled
  public void assignRoleToUserAsOtherOrganizationAdminFails() {
    // TODO
    // fail is expected. No permission to assign territory role to user if don't
    // have territory role
  }

  @Test
  @Disabled
  public void updateUserAsOtherOrganizationAdminFails() {
    // TODO
    // Update user (linked to another organization) by an organization admin user
    // (ADMIN DE ORGANIZACION)
    // fail is expected (no permission)
  }

  @Test
  @Disabled
  public void updateUserPasswordAsOtherOrganizationAdminFails() {
    // TODO
    // Update user password (linked to another organization) by an organization
    // admin user (ADMIN DE ORGANIZACION)
    // fail is expected (no permission)
  }

}
