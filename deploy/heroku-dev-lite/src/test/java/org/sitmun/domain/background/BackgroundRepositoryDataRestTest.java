package org.sitmun.domain.background;

import org.junit.jupiter.api.Test;
import org.sitmun.test.Fixtures;
import org.sitmun.test.URIConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
class BackgroundRepositoryDataRestTest {

  @Autowired
  private MockMvc mvc;

  @Test
  void backgroundIsNotDependentOfBackgroundMap() throws Exception {
    String content = "{" +
      "\"name\":\"test\"," +
      "\"description\":\"test\"," +
      "\"active\":\"true\"" +
      "}";

    mvc.perform(post(URIConstants.BACKGROUNDS_URI)
        .content(content)
        .with(user(Fixtures.admin()))
      )
      .andExpect(status().isCreated())
      .andReturn();
  }

  @Test
  void createBackgroundWithMap() throws Exception {
    String content = "{" +
      "\"name\":\"test\"," +
      "\"description\":\"test\"," +
      "\"active\":\"true\"," +
      "\"cartographyGroup\":\"http://localhost/api/cartography-group/129\"" +
      "}";
    String location = mvc.perform(post(URIConstants.BACKGROUNDS_URI)
        .content(content)
        .with(user(Fixtures.admin()))
      )
      .andDo(print())
      .andExpect(status().isCreated())
      .andReturn().getResponse().getHeader("Location");

    assertNotNull(location);
  }

  @Test
  void failCreateBackgroundWithInvalidMap() throws Exception {
    String content = "{" +
      "\"name\":\"test\"," +
      "\"description\":\"test\"," +
      "\"active\":\"true\"," +
      "\"cartographyGroup\":\"http://localhost/api/cartography-group/1\"" +
      "}";
    mvc.perform(post(URIConstants.BACKGROUNDS_URI)
        .content(content)
        .with(user(Fixtures.admin()))
      )
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.errors[0].invalidValue").value("C"));
  }

  @Test
  void failUpdateBackgroundWithInvalidMap() throws Exception {
    String content = "http://localhost/api/cartography-group/1";
    mvc.perform(put(URIConstants.BACKGROUND_URI_CARTOGRAPHY_GROUP, 1)
        .content(content)
        .contentType("text/uri-list")
        .with(user(Fixtures.admin()))
      )
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.errors[0].invalidValue").value("C"));
  }
}
