package org.sitmun.plugin.core.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.sitmun.plugin.core.security.SecurityConstants.HEADER_STRING;
import static org.sitmun.plugin.core.security.SecurityConstants.TOKEN_PREFIX;
import static org.sitmun.plugin.core.test.TestConstants.SITMUN_ADMIN_USERNAME;
import static org.sitmun.plugin.core.test.TestUtils.asJsonString;
import static org.sitmun.plugin.core.test.TestUtils.withMockSitmunAdmin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


import java.util.ArrayList;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sitmun.plugin.core.config.RepositoryRestConfig;
import org.sitmun.plugin.core.domain.Role;
import org.sitmun.plugin.core.domain.Territory;
import org.sitmun.plugin.core.domain.User;
import org.sitmun.plugin.core.domain.UserConfiguration;
import org.sitmun.plugin.core.repository.RoleRepository;
import org.sitmun.plugin.core.repository.TerritoryRepository;
import org.sitmun.plugin.core.repository.UserConfigurationRepository;
import org.sitmun.plugin.core.repository.UserRepository;
import org.sitmun.plugin.core.security.AuthoritiesConstants;
import org.sitmun.plugin.core.security.TokenProvider;
import org.sitmun.plugin.core.service.UserService;
import org.sitmun.plugin.core.service.dto.UserDTO;
import org.sitmun.plugin.core.web.rest.dto.PasswordDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class UserResourceTest {

  private static final String TERRITORY1_ADMIN_USERNAME = "territory1-admin";
  private static final String TERRITORY1_USER_USERNAME = "territory1-user";
  private static final String TERRITORY2_USER_USERNAME = "territory2-user";
  private static final String NEW_USER_USERNAME = "admin_new";
  private static final String USER_PASSWORD = "admin";
  private static final String USER_CHANGEDPASSWORD = "nimda";
  private static final String USER_FIRSTNAME = "Admin";
  private static final String USER_CHANGEDFIRSTNAME = "Administrator";
  private static final String USER_LASTNAME = "Admin";
  private static final String USER_CHANGEDLASTNAME = "Territory 1";
  private static final Boolean USER_BLOCKED = false;
  private static final Boolean USER_ADMINISTRATOR = true;
  private static final String USER_URI = "http://localhost/api/users";
  @Autowired
  UserRepository userRepository;
  @Autowired
  UserConfigurationRepository userConfigurationRepository;
  @Autowired
  RoleRepository roleRepository;
  @Autowired
  TerritoryRepository territoryRepository;
  @Autowired
  UserService userService;
  @Autowired
  TokenProvider tokenProvider;
  @Autowired
  private MockMvc mvc;
  private String token;
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

  @Before
  public void init() {
    withMockSitmunAdmin(() -> {

      token = tokenProvider.createToken(SITMUN_ADMIN_USERNAME);

      organizacionAdminRole =
          Role.builder().setName(AuthoritiesConstants.ADMIN_ORGANIZACION).build();
      roleRepository.save(organizacionAdminRole);

      territorialRole = Role.builder().setName(AuthoritiesConstants.USUARIO_TERRITORIAL).build();
      roleRepository.save(territorialRole);

      territories = new ArrayList<>();
      users = new ArrayList<>();
      territory1 = Territory.builder()
          .setName("Territorio 1")
          .setCode("")
          .setBlocked(false)
          .build();

      territory2 = Territory.builder()
          .setName("Territorio 2")
          .setCode("")
          .setBlocked(false)
          .build();
      territories.add(territory1);
      territories.add(territory2);

      territoryRepository.saveAll(territories);

      // Territory 1 Admin
      organizacionAdmin = new User();
      organizacionAdmin.setAdministrator(USER_ADMINISTRATOR);
      organizacionAdmin.setBlocked(USER_BLOCKED);
      organizacionAdmin.setFirstName(USER_FIRSTNAME);
      organizacionAdmin.setLastName(USER_LASTNAME);
      organizacionAdmin.setPassword(USER_PASSWORD);
      organizacionAdmin.setUsername(TERRITORY1_ADMIN_USERNAME);
      users.add(organizacionAdmin);

      // Territory 1 user
      territory1User = new User();
      territory1User.setAdministrator(false);
      territory1User.setBlocked(USER_BLOCKED);
      territory1User.setFirstName(USER_FIRSTNAME);
      territory1User.setLastName(USER_LASTNAME);
      territory1User.setPassword(USER_PASSWORD);
      territory1User.setUsername(TERRITORY1_USER_USERNAME);
      users.add(territory1User);

      // Territory 2 user
      territory2User = new User();
      territory2User.setAdministrator(false);
      territory2User.setBlocked(USER_BLOCKED);
      territory2User.setFirstName(USER_FIRSTNAME);
      territory2User.setLastName(USER_LASTNAME);
      territory2User.setPassword(USER_PASSWORD);
      territory2User.setUsername(TERRITORY2_USER_USERNAME);
      users.add(territory2User);

      userRepository.saveAll(users);

      userConfigurations = new ArrayList<>();

      UserConfiguration userConf = new UserConfiguration();
      userConf.setTerritory(territory1);
      userConf.setRole(organizacionAdminRole);
      userConf.setUser(organizacionAdmin);
      userConfigurations.add(userConf);
      this.userConfigurationRepository.save(userConf);

      userConf = new UserConfiguration();
      userConf.setTerritory(territory1);
      userConf.setRole(territorialRole);
      userConf.setUser(territory1User);
      userConfigurations.add(userConf);
      this.userConfigurationRepository.save(userConf);

      userConf = new UserConfiguration();
      userConf.setTerritory(territory2);
      userConf.setRole(territorialRole);
      userConf.setUser(territory2User);
      userConfigurations.add(userConf);

      userConfigurationRepository.saveAll(userConfigurations);
    });
  }

  @After
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
  @WithMockUser(username = SITMUN_ADMIN_USERNAME)
  public void createNewUserAndDelete() throws Exception {
    UserDTO newUser = new UserDTO(organizacionAdmin);
    newUser.setId(null);
    newUser.setUsername(NEW_USER_USERNAME);

    String uri = mvc.perform(post("/api/users")
        .header(HEADER_STRING, TOKEN_PREFIX + token)
        .contentType(MediaType.APPLICATION_JSON)
        .content(asJsonString(newUser))
    ).andExpect(status().isCreated())
        .andReturn().getResponse().getHeader("Location");

    assertThat(uri).isNotNull();

    mvc.perform(get(uri)
        .header(HEADER_STRING, TOKEN_PREFIX + token)
    ).andExpect(status().isOk())
        .andExpect(content().contentType(MediaTypes.HAL_JSON))
        .andExpect(jsonPath("$.username", equalTo(NEW_USER_USERNAME)));

    mvc.perform(delete(uri)
        .header(HEADER_STRING, TOKEN_PREFIX + token)
    ).andExpect(status().isNoContent());
  }

  @Test
  @WithMockUser(username = SITMUN_ADMIN_USERNAME)
  public void createDuplicatedUserFails() throws Exception {
    UserDTO newUser = new UserDTO(organizacionAdmin);
    newUser.setId(null);

    mvc.perform(post("/api/users")
        .header(HEADER_STRING, TOKEN_PREFIX + token)
        .contentType(MediaType.APPLICATION_JSON)
        .content(asJsonString(newUser)))
        .andExpect(status().isConflict());
  }

  @Test
  @WithMockUser(username = SITMUN_ADMIN_USERNAME)
  public void updateUser() throws Exception {
    UserDTO userDTO = new UserDTO(organizacionAdmin);
    userDTO.setFirstName(USER_CHANGEDFIRSTNAME);
    userDTO.setLastName(USER_CHANGEDLASTNAME);

    mvc.perform(put(USER_URI + "/" + organizacionAdmin.getId())
        .header(HEADER_STRING, TOKEN_PREFIX + token)
        .contentType(MediaType.APPLICATION_JSON)
        .content(asJsonString(userDTO))
    ).andExpect(status().isNoContent());

    mvc.perform(get(USER_URI + "/" + organizacionAdmin.getId()))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaTypes.HAL_JSON))
        .andExpect(jsonPath("$.firstName", equalTo(USER_CHANGEDFIRSTNAME)))
        .andExpect(jsonPath("$.lastName", equalTo(USER_CHANGEDLASTNAME)));
  }

  @Test
  @WithMockUser(username = SITMUN_ADMIN_USERNAME)
  public void getUsersAsSitmunAdmin() throws Exception {
    mvc.perform(get(USER_URI)
        .header(HEADER_STRING, TOKEN_PREFIX + token))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaTypes.HAL_JSON))
        .andExpect(jsonPath("$._embedded.users", hasSize(1335)));
  }

  @Deprecated
  @Ignore
  @WithMockUser(username = TERRITORY1_ADMIN_USERNAME)
  public void getUsersAsOrganizationAdmin() throws Exception {
    mvc.perform(get(USER_URI).header(HEADER_STRING, TOKEN_PREFIX + token))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaTypes.HAL_JSON))
        .andExpect(jsonPath("$._embedded.users", hasSize(5)));
  }

  @Test
  @WithMockUser(username = SITMUN_ADMIN_USERNAME)
  public void updateUserPassword() throws Exception {
    PasswordDTO passwordDTO = new PasswordDTO();
    passwordDTO.setPassword(USER_CHANGEDPASSWORD);

    mvc.perform(post(USER_URI + "/" + organizacionAdmin.getId() + "/change-password")
        .header(HEADER_STRING, TOKEN_PREFIX + token)
        .contentType(MediaType.APPLICATION_JSON)
        .content(asJsonString(passwordDTO))
    ).andExpect(status().isOk());
  }

  @Test
  @WithMockUser(username = SITMUN_ADMIN_USERNAME)
  public void updateUserPasswordAsSitmunAdmin() throws Exception {
    PasswordDTO passwordDTO = new PasswordDTO();
    passwordDTO.setPassword(USER_CHANGEDPASSWORD);

    mvc.perform(post(USER_URI + "/" + organizacionAdmin.getId() + "/change-password")
        .header(HEADER_STRING, TOKEN_PREFIX + token)
        .contentType(MediaType.APPLICATION_JSON)
        .content(asJsonString(passwordDTO))
    ).andExpect(status().isOk());
  }

  @Ignore
  public void createNewUserAsOrganizationAdmin() {
    // TODO: Create new user by an organization admin user (ADMIN DE ORGANIZACION)
    // ok is expected. The new user has roles linked to my organization territory
  }

  @Ignore
  public void assignRoleToUserAsOrganizationAdmin() {
    // TODO
    // ok is expected. The new user has roles linked to my organization territory
  }

  @Ignore
  public void updateUserAsOrganizationAdmin() {
    // TODO
    // Update user (linked to the same organization) by an organization admin user
    // (ADMIN DE ORGANIZACION)
    // ok is expected
  }

  @Ignore
  public void updateUserPasswordAsOrganizationAdmin() {
    // TODO
    // Update user password (linked to the same organization) by an organization
    // admin user (ADMIN DE ORGANIZACION)
    // ok is expected
  }

  @Ignore
  public void assignRoleToUserAsOtherOrganizationAdminFails() {
    // TODO
    // fail is expected. No permission to assign territory role to user if don't
    // have territory role
  }

  @Ignore
  public void updateUserAsOtherOrganizationAdminFails() {
    // TODO
    // Update user (linked to another organization) by an organization admin user
    // (ADMIN DE ORGANIZACION)
    // fail is expected (no permission)
  }

  @Ignore
  public void updateUserPasswordAsOtherOrganizationAdminFails() {
    // TODO
    // Update user password (linked to another organization) by an organization
    // admin user (ADMIN DE ORGANIZACION)
    // fail is expected (no permission)
  }

  @TestConfiguration
  static class ContextConfiguration {
    @Bean
    public Validator validator() {
      return new LocalValidatorFactoryBean();
    }

    @Bean
    RepositoryRestConfigurer repositoryRestConfigurer() {
      return new RepositoryRestConfig(validator());
    }
  }
}
