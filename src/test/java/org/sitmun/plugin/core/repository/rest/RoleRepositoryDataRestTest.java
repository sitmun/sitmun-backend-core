package org.sitmun.plugin.core.repository.rest;

import org.junit.Test;
import org.junit.runner.RunWith;
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

  private static final String ROLES_URI = "http://localhost/api/roles";
  private static final String ROLE_URI = ROLES_URI + "/{0}";
  private static final String ROLE_APPLICATIONS_URI = ROLE_URI + "/applications";

  @Autowired
  private MockMvc mvc;

  @Test
  public void getApplicationsOfARole() throws Exception {
    mvc.perform(get(ROLE_APPLICATIONS_URI, 10))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.applications", hasSize(1)))
      .andExpect(jsonPath("$._embedded.applications[0].id").value(1));
  }
}
