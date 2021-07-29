package org.sitmun.plugin.core.repository.rest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sitmun.plugin.core.test.URIConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.sitmun.plugin.core.test.TestConstants.SITMUN_ADMIN_USERNAME;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("CartographyStyles Repository Data REST test")
public class CartographyStylesRepositoryDataRestTest {

  @Autowired
  private MockMvc mvc;

  @Test
  @DisplayName("POST: Create a CartographyStyle")
  public void newCartographyStyleCanBePosted() throws Exception {
    String content = "{" +
      "\"name\":\"test\"," +
      "\"defaultStyle\":false," +
      "\"cartography\":\"http://localhost/api/cartographies/1228\"" +
      "}";

    String location = mvc.perform(
      post(URIConstants.CARTOGRAPHY_STYLES_URI)
        .content(content)
        .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
    ).andExpect(status().isCreated())
      .andExpect(jsonPath("$.name").value("test"))
      .andReturn().getResponse().getHeader("Location");

    assertNotNull(location);

    mvc.perform(delete(location)
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
    ).andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("POST: No two default CartographyStyle can be created for the same Cartography")
  public void twoDefaultStylesCannotBePosted() throws Exception {
    String content = "{" +
      "\"name\":\"test\"," +
      "\"defaultStyle\":true," +
      "\"cartography\":\"http://localhost/api/cartographies/1228\"" +
      "}";

    String location = mvc.perform(
      post(URIConstants.CARTOGRAPHY_STYLES_URI)
        .content(content)
        .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
    ).andExpect(status().isCreated())
      .andExpect(jsonPath("$.name").value("test"))
      .andReturn().getResponse().getHeader("Location");

    assertNotNull(location);

    mvc.perform(
      post(URIConstants.CARTOGRAPHY_STYLES_URI)
        .content(content)
        .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
    ).andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.errors[0].property").value("defaultStyle"))
      .andExpect(jsonPath("$.errors[0].message").value("Already a default style exists for the cartography."));

    mvc.perform(delete(location)
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
    ).andExpect(status().isNoContent());
  }

}