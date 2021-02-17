package org.sitmun.plugin.core.repository.rest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.sitmun.plugin.core.test.URIConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class RoleRepositoryDataRestTest {

  @Autowired
  private MockMvc mvc;

  @Test
  public void getApplicationsOfARole() throws Exception {
    mvc.perform(get(URIConstants.ROLE_APPLICATIONS_URI, 10))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.applications", hasSize(1)))
      .andExpect(jsonPath("$._embedded.applications[0].id").value(1));
  }

  @Test
  public void getTasksOfARole() throws Exception {
    mvc.perform(get(URIConstants.ROLE_TASKS_URI, 10))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.*.*", hasSize(220)))
      .andExpect(jsonPath("$._embedded.tasks", hasSize(23)))
      .andExpect(jsonPath("$._embedded.download-tasks", hasSize(136)))
      .andExpect(jsonPath("$._embedded.query-tasks", hasSize(61)));
  }

  @Test
  public void getPermissionsOfARole() throws Exception {
    mvc.perform(get(URIConstants.ROLE_PERMISSIONS_URI, 10))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.cartography-groups", hasSize(35)));
  }
}
