package org.sitmun.domain.background;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.sitmun.test.Fixtures;
import org.sitmun.test.URIConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
class BackgroundRepositoryDataRestTest {

  @Autowired
  private MockMvc mvc;

  @Test
  @Disabled("Potential freeze of active connections. @Transactional may be required in REST controllers.")
  void backgroundIsNotDependentOfBackgroundMap() throws Exception {
    String content = "{" +
      "\"name\":\"test\"," +
      "\"description\":\"test\"," +
      "\"active\":\"true\"" +
      "}";

    MvcResult result = mvc.perform(post(URIConstants.BACKGROUNDS_URI)
        .content(content)
        .with(user(Fixtures.admin()))
      )
      .andExpect(status().isCreated())
      .andReturn();
    String response = result.getResponse().getContentAsString();
    mvc.perform(delete(URIConstants.BACKGROUND_URI, JsonPath.parse(response).read("$.id", Integer.class))
        .with(user(Fixtures.admin()))
      )
      .andExpect(status().isNoContent())
      .andReturn();
  }

  @Test
  @Disabled("Potential freeze of active connections. @Transactional may be required in REST controllers.")
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
      .andExpect(status().isCreated())
      .andReturn().getResponse().getHeader("Location");

    assertNotNull(location);

    mvc.perform(delete(location)
      .with(user(Fixtures.admin()))
    ).andExpect(status().isNoContent());
  }

  @Test
  void failCreateBackgroundWithInvalidMap() throws Exception {
    String content = "{" +
      "\"name\":\"test\"," +
      "\"description\":\"test\"," +
      "\"active\":\"true\"," +
      "\"cartographyGroup\":\"http://localhost/api/cartography-group/193\"" +
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
    String content = "http://localhost/api/cartography-group/193";
    mvc.perform(put(URIConstants.BACKGROUND_URI_CARTOGRAPHY_GROUP, 1)
        .content(content)
        .contentType("text/uri-list")
        .with(user(Fixtures.admin()))
      )
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.errors[0].invalidValue").value("C"));
  }
}
