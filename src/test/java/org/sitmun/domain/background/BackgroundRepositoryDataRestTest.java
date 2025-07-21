package org.sitmun.domain.background;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.annotation.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.test.Fixtures;
import org.sitmun.test.URIConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Background Repository Data REST test")
class BackgroundRepositoryDataRestTest {

  @Autowired private MockMvc mvc;

  @Nullable private MockHttpServletResponse response;

  @Test
  @DisplayName("POST: minimum set of properties")
  void createBackground() throws Exception {
    String content =
        '{' + "\"name\":\"test\"," + "\"description\":\"test\"," + "\"active\":\"true\"" + '}';

    response =
        mvc.perform(
                post(URIConstants.BACKGROUNDS_URI).content(content).with(user(Fixtures.admin())))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse();
  }

  @Test
  @DisplayName("POST: fail creation when cartography group has invalid background map")
  void failCreateBackgroundWithInvalidMap() throws Exception {
    String content =
        '{'
            + "\"name\":\"test\","
            + "\"description\":\"test\","
            + "\"active\":\"true\","
            + "\"cartographyGroup\":\"http://localhost/api/cartography-group/1\""
            + '}';
    mvc.perform(post(URIConstants.BACKGROUNDS_URI).content(content).with(user(Fixtures.admin())))
        .andDo(print())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].invalidValue").value("C"));
  }

  @Test
  @DisplayName("PUT: fail update of cartography group with invalid background map")
  void failUpdateBackgroundWithInvalidMap() throws Exception {
    String content = "http://localhost/api/cartography-group/1";
    mvc.perform(
            put(URIConstants.BACKGROUND_URI_CARTOGRAPHY_GROUP, 1)
                .content(content)
                .contentType("text/uri-list")
                .with(user(Fixtures.admin())))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].invalidValue").value("C"));
  }

  @AfterEach
  void cleanup() throws Exception {
    if (response != null) {
      String location = response.getHeader("Location");
      if (location != null) {
        mvc.perform(delete(location).with(user(Fixtures.admin())))
            .andExpect(status().isNoContent());
      }
      response = null;
    }
  }
}
