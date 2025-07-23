package org.sitmun.domain.role;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.test.URIConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Role Repository Data REST test")
class RoleRepositoryDataRestTest {

  @Autowired private MockMvc mvc;

  @Test
  @DisplayName("GET: Retrieve all applications for a role")
  @WithMockUser(roles = "ADMIN")
  void getApplicationsOfARole() throws Exception {
    mvc.perform(get(URIConstants.ROLE_APPLICATIONS_URI, 1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.applications", hasSize(4)))
        .andExpect(jsonPath("$._embedded.applications[0].id").value(1));
  }

  @Test
  @DisplayName("GET: Retrieve all tasks for a role")
  @WithMockUser(roles = "ADMIN")
  void getTasksOfARole() throws Exception {
    mvc.perform(get(URIConstants.ROLE_TASKS_URI, 1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.*.*", hasSize(35)))
        .andExpect(jsonPath("$._embedded.tasks", hasSize(35)));
  }

  @Test
  @DisplayName("GET: Retrieve all permissions for a role")
  @WithMockUser(roles = "ADMIN")
  void getPermissionsOfARole() throws Exception {
    mvc.perform(get(URIConstants.ROLE_PERMISSIONS_URI, 1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.cartography-groups", hasSize(3)));
  }
}
