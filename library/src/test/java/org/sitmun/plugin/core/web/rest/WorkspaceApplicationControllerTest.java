package org.sitmun.plugin.core.web.rest;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sitmun.plugin.core.security.TokenProvider;
import org.sitmun.plugin.core.test.URIConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc

public class WorkspaceApplicationControllerTest {

  @Autowired
  TokenProvider tokenProvider;

  @Autowired
  private MockMvc mvc;

  @Test
  @Disabled("Potential freeze of active connections. @Transactional may be required in REST controllers.")
  public void readPublicUser() throws Exception {
    mvc.perform(get(URIConstants.WORKSPACE_APPLICATION_URI, 1, 41))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.territory.name").value("Argentona"))
      .andExpect(jsonPath("$.application.title").value("SITMUN - Consulta municipal"))
      .andExpect(jsonPath("$.roles[*].id", containsInAnyOrder(10)));
  }

  @Test
  @Disabled("Potential freeze of active connections. @Transactional may be required in REST controllers.")
  public void readOtherUser() throws Exception {
    mvc.perform(get(URIConstants.WORKSPACE_APPLICATION_URI, 1, 41)
      .with(SecurityMockMvcRequestPostProcessors.user("user12"))
    )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.territory.name").value("Argentona"))
      .andExpect(jsonPath("$.application.title").value("SITMUN - Consulta municipal"))
      .andExpect(jsonPath("$.roles[*].id", containsInAnyOrder(14, 17)));
  }

}
