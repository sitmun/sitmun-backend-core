package org.sitmun.plugin.core.repository.rest;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sitmun.plugin.core.test.URIConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.sitmun.plugin.core.test.TestConstants.SITMUN_ADMIN_USERNAME;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc

public class BackgroundRepositoryDataRestTest {

  @Autowired
  private MockMvc mvc;

  @Test
  public void backgroundIsNotDependentOfBackgroundMap() throws Exception {
    String content = "{" +
      "\"name\":\"test\"," +
      "\"description\":\"test\"," +
      "\"active\":\"true\"" +
      "}";

    MvcResult result = mvc.perform(post(URIConstants.BACKGROUNDS_URI)
      .content(content)
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
    )
      .andExpect(status().isCreated())
      .andReturn();
    String response = result.getResponse().getContentAsString();
    mvc.perform(delete(URIConstants.BACKGROUND_URI, JsonPath.parse(response).read("$.id", Integer.class))
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
    )
      .andExpect(status().isNoContent())
      .andReturn();
  }

  @Test
  public void createBackgroundWithMap() throws Exception {
    String content = "{" +
      "\"name\":\"test\"," +
      "\"description\":\"test\"," +
      "\"active\":\"true\"," +
      "\"cartographyGroup\":\"http://localhost/api/cartography-group/129\"" +
      "}";
    String location = mvc.perform(post(URIConstants.BACKGROUNDS_URI)
      .content(content)
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
    )
      .andExpect(status().isCreated())
      .andReturn().getResponse().getHeader("Location");

    assertNotNull(location);

    mvc.perform(delete(location)
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
    ).andExpect(status().isNoContent());
  }

  @Test
  public void failCreateBackgroundWithInvalidMap() throws Exception {
    String content = "{" +
      "\"name\":\"test\"," +
      "\"description\":\"test\"," +
      "\"active\":\"true\"," +
      "\"cartographyGroup\":\"http://localhost/api/cartography-group/193\"" +
      "}";
    mvc.perform(post(URIConstants.BACKGROUNDS_URI)
      .content(content)
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
    )
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.errors[0].invalidValue").value("C"));
  }

  @Test
  public void failUpdateBackgroundWithInvalidMap() throws Exception {
    String content = "http://localhost/api/cartography-group/193";
    mvc.perform(put(URIConstants.BACKGROUND_URI_CARTOGRAPHY_GROUP, 1)
      .content(content)
      .contentType("text/uri-list")
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
    )
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.errors[0].invalidValue").value("C"));
  }
}
