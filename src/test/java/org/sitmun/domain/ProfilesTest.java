package org.sitmun.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.test.URIConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc

@DisplayName("Profiles test")
class ProfilesTest {

  @Autowired
  private MockMvc mvc;

  @Test
  @DisplayName("Profile view")
  void profileView() throws Exception {
    mvc.perform(get(URIConstants.HAL_PROFILE_URI)
        .accept("application/hal+json")
      ).andExpect(status().isOk())
      .andExpect(jsonPath("$._links").exists());
  }

  @Test
  @DisplayName("Profile entity view")
  void profileEntityView() throws Exception {
    mvc.perform(get(URIConstants.HAL_PROFILE_URI + "/applications")
        .accept("application/hal+json")
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.alps").exists());
  }
}
