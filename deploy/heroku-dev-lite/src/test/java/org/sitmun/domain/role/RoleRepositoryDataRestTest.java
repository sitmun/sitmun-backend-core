package org.sitmun.domain.role;

import org.junit.jupiter.api.Test;
import org.sitmun.test.Fixtures;
import org.sitmun.test.URIConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
class RoleRepositoryDataRestTest {

  @Autowired
  private MockMvc mvc;

  @Test
  void getApplicationsOfARole() throws Exception {
    mvc.perform(get(URIConstants.ROLE_APPLICATIONS_URI, 10)
        .with(user(Fixtures.admin())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.applications", hasSize(1)))
      .andExpect(jsonPath("$._embedded.applications[0].id").value(1));
  }

  @Test
  void getTasksOfARole() throws Exception {
    mvc.perform(get(URIConstants.ROLE_TASKS_URI, 10)
        .with(user(Fixtures.admin())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.*.*", hasSize(220)))
      .andExpect(jsonPath("$._embedded.tasks", hasSize(220)));
  }

  @Test
  void getPermissionsOfARole() throws Exception {
    mvc.perform(get(URIConstants.ROLE_PERMISSIONS_URI, 10)
        .with(user(Fixtures.admin())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.cartography-groups", hasSize(35)));
  }
}
