package org.sitmun.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sitmun.test.TestUtils.asJsonString;
import static org.sitmun.test.URIConstants.USER_URI;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONArray;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("User Resource Integration Test")
class UserResourceIntegrationTest {

  private static final String TERRITORY1_ADMIN_USERNAME = "territory1-admin";
  private static final String NEW_USER_USERNAME = "admin_new";
  private static final String USER_PASSWORD = "admin";
  private static final String USER_FIRSTNAME = "Admin";
  private static final String USER_LASTNAME = "Admin";
  private static final Boolean USER_BLOCKED = false;
  private static final Boolean USER_ADMINISTRATOR = true;
  @Autowired private MockMvc mvc;
  private User organizacionAdmin;

  @BeforeEach
  void init() {
    organizacionAdmin = new User();
    organizacionAdmin.setAdministrator(USER_ADMINISTRATOR);
    organizacionAdmin.setBlocked(USER_BLOCKED);
    organizacionAdmin.setFirstName(USER_FIRSTNAME);
    organizacionAdmin.setLastName(USER_LASTNAME);
    organizacionAdmin.setPassword(USER_PASSWORD);
    organizacionAdmin.setUsername(TERRITORY1_ADMIN_USERNAME);
  }

  @Test
  @DisplayName("POST: Create new user and delete")
  @WithMockUser(roles = "ADMIN")
  void createNewUserAndDelete() throws Exception {
    User newUser = organizacionAdmin.toBuilder().id(null).username(NEW_USER_USERNAME).build();

    MvcResult created =
        mvc.perform(post(USER_URI).contentType(APPLICATION_JSON).content(asJsonString(newUser)))
            .andExpect(status().isCreated())
            .andReturn();
    String location = created.getResponse().getHeader("Location");
    assertThat(location).isNotNull();

    mvc.perform(get(location)).andExpect(status().isOk());
    mvc.perform(delete(location)).andExpect(status().is2xxSuccessful());
    mvc.perform(get(location)).andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("POST: Create new user without login")
  void cannotPostWithoutLogin() throws Exception {
    User newUser = organizacionAdmin.toBuilder().id(null).username(NEW_USER_USERNAME).build();

    mvc.perform(post(USER_URI).contentType(APPLICATION_JSON).content(asJsonString(newUser)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("GET: Get all users")
  @WithMockUser(roles = "ADMIN")
  void getAllUsers() throws Exception {
    String body =
        mvc.perform(get(USER_URI))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(JsonPath.parse(body).read("$._embedded.users[*].username", JSONArray.class))
        .containsExactlyInAnyOrder("admin", "public", "blocked", "internal", "user12");
  }
}
