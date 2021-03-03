package org.sitmun.plugin.core.repository.rest;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sitmun.plugin.core.test.URIConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.sitmun.plugin.core.test.TestConstants.SITMUN_ADMIN_USERNAME;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
public class ApplicationRepositoryDataRestTest {

  @Autowired
  private MockMvc mvc;


  @Test
  public void createApplication() throws Exception {
    String content = "{" +
      "\"name\":\"test\"," +
      "\"jspTemplate\":\"test\"," +
      "\"type\":\"I\"," +
      "\"createdDate\":\"2020-01-01\"," +
      "\"situationMap\":\"http://localhost/api/cartography-group/132\"" +
      "}";
    String location = mvc.perform(post(URIConstants.APPLICATIONS_URI)
      .content(content)
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
    )
      .andExpect(status().isCreated())
      .andReturn().getResponse().getHeader("Location");

    Assert.assertNotNull(location);

    mvc.perform(delete(location)
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
    ).andExpect(status().isNoContent());
  }

  @Test
  public void failApplicationWithInvalidMap() throws Exception {
    String content = "{" +
      "\"name\":\"test\"," +
      "\"jspTemplate\":\"test\"," +
      "\"type\":\"I\"," +
      "\"createdDate\":\"2020-01-01\"," +
      "\"situationMap\":\"http://localhost/api/cartography-group/6\"" +
      "}";
    mvc.perform(post(URIConstants.APPLICATIONS_URI)
      .content(content)
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
    )
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.errors[0].invalidValue").value("C"));
  }

  @Test
  public void failUpdateApplicationWithInvalidMap() throws Exception {
    String content = "http://localhost/api/cartography-group/6";
    mvc.perform(put(URIConstants.APPLICATION_URI_SITUATION_MAP, 1)
      .content(content)
      .contentType("text/uri-list")
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
    )
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.errors[0].invalidValue").value("C"));
  }
}
