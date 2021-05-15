package org.sitmun.plugin.core.repository.rest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sitmun.plugin.core.test.URIConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.sitmun.plugin.core.test.TestConstants.SITMUN_ADMIN_USERNAME;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc

public class RoleRepositoryDataRestTest {

  @Autowired
  private MockMvc mvc;

  @Test
  public void getApplicationsOfARole() throws Exception {
    mvc.perform(get(URIConstants.ROLE_APPLICATIONS_URI, 10)
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME)))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.applications", hasSize(1)))
      .andExpect(jsonPath("$._embedded.applications[0].id").value(1));
  }

  @Test
  public void getTasksOfARole() throws Exception {
    mvc.perform(get(URIConstants.ROLE_TASKS_URI, 10)
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME)))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.*.*", hasSize(220)))
      .andExpect(jsonPath("$._embedded.tasks", hasSize(220)));
  }

  @Test
  public void getPermissionsOfARole() throws Exception {
    mvc.perform(get(URIConstants.ROLE_PERMISSIONS_URI, 10)
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME)))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.cartography-groups", hasSize(35)));
  }
}
