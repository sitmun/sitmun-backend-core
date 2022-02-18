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

import static org.hamcrest.Matchers.hasSize;
import static org.sitmun.plugin.core.security.SecurityConstants.HEADER_STRING;
import static org.sitmun.plugin.core.security.SecurityConstants.TOKEN_PREFIX;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@Disabled
public class WorkspaceControllerTest {

  @Autowired
  TokenProvider tokenProvider;

  @Autowired
  private MockMvc mvc;

  @Test
  public void readPublicUser() throws Exception {
    mvc.perform(get(URIConstants.WORKSPACE_URI))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.territories[*].userConfigurations[*].role.applications[*]", hasSize(7)));
  }

  @Test
  public void readOtherUser() throws Exception {
    mvc.perform(get(URIConstants.WORKSPACE_URI)
      .with(SecurityMockMvcRequestPostProcessors.user("user12"))
    )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.territories[*].userConfigurations[*].role.applications[*]", hasSize(465)));
  }

  @Test
  public void readOtherUserWithToken() throws Exception {
    mvc.perform(get(URIConstants.WORKSPACE_URI)
      .header(HEADER_STRING, TOKEN_PREFIX + tokenProvider.createToken("user12"))
    ).andExpect(status().isOk())
      .andExpect(jsonPath("$.territories[*].userConfigurations[*].role.applications[*]", hasSize(465)));
  }
}
