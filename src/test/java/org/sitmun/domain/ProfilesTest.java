package org.sitmun.domain;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.test.URIConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("HAL Profiles Integration Test")
class ProfilesTest {

  @Autowired private MockMvc mvc;

  @Test
  @DisplayName("GET: Retrieve HAL profile root with links")
  void profileView() throws Exception {
    mvc.perform(get(URIConstants.HAL_PROFILE_URI).accept("application/hal+json"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._links").exists());
  }

  @Test
  @DisplayName("GET: Retrieve HAL profile entity documentation for applications")
  void profileEntityView() throws Exception {
    mvc.perform(get(URIConstants.HAL_PROFILE_URI + "/applications").accept("application/hal+json"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.alps").exists());
  }
}
