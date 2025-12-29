package org.sitmun.domain.background;

import static org.sitmun.test.URIConstants.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.annotation.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Background Repository Data REST test")
class BackgroundRepositoryDataRestTest {

  @Autowired private MockMvc mvc;

  @Nullable private MockHttpServletResponse response;

  @Test
  @DisplayName("POST: minimum set of properties")
  @WithMockUser(roles = "ADMIN")
  void createBackground() throws Exception {
    String content =
        """
        {
        "name":"test",
        "description":"test",
        "active":"true"
        }""";

    response =
        mvc.perform(post(BACKGROUNDS_URI).content(content))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse();
  }

  @Test
  @DisplayName("POST: fail creation when cartography group has invalid background map")
  @WithMockUser(roles = "ADMIN")
  void failCreateBackgroundWithInvalidMap() throws Exception {
    String content =
        """
        {
        "name":"test",
        "description":"test",
        "active":"true",
        "cartographyGroup":"http://localhost/api/cartography-group/1"
        }""";
    mvc.perform(post(BACKGROUNDS_URI).content(content))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].rejectedValue").value("C"));
  }

  @Test
  @DisplayName("PUT: fail update of cartography group with invalid background map")
  @WithMockUser(roles = "ADMIN")
  void failUpdateBackgroundWithInvalidMap() throws Exception {
    String content = "http://localhost/api/cartography-group/1";
    mvc.perform(
            put(BACKGROUND_URI_CARTOGRAPHY_GROUP, 1).content(content).contentType("text/uri-list"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].rejectedValue").value("C"));
  }

  @AfterEach
  void cleanup() throws Exception {
    if (response != null) {
      String location = response.getHeader("Location");
      if (location != null) {
        mvc.perform(delete(location)).andExpect(status().isNoContent());
      }
      response = null;
    }
  }
}
