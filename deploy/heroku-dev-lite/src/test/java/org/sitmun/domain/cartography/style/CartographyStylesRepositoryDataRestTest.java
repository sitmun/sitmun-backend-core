package org.sitmun.domain.cartography.style;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.test.Fixtures;
import org.sitmun.test.URIConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("CartographyStyles Repository Data REST test")
class CartographyStylesRepositoryDataRestTest {

  @Autowired
  private MockMvc mvc;

  @Test
  @DisplayName("POST: Create a CartographyStyle")
  @Disabled("Potential freeze of active connections. @Transactional may be required in REST controllers.")
  void newCartographyStyleCanBePosted() throws Exception {
    String content = "{" +
      "\"name\":\"test\"," +
      "\"defaultStyle\":false," +
      "\"cartography\":\"http://localhost/api/cartographies/1228\"" +
      "}";

    String location = mvc.perform(
        post(URIConstants.CARTOGRAPHY_STYLES_URI)
          .content(content)
          .with(user(Fixtures.admin()))
      ).andExpect(status().isCreated())
      .andExpect(jsonPath("$.name").value("test"))
      .andReturn().getResponse().getHeader("Location");

    assertNotNull(location);

    mvc.perform(delete(location)
      .with(user(Fixtures.admin()))
    ).andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("POST: No two default CartographyStyle can be created for the same Cartography")
  @Disabled("Potential freeze of active connections. @Transactional may be required in REST controllers.")
  void twoDefaultStylesCannotBePosted() throws Exception {
    String content = "{" +
      "\"name\":\"test\"," +
      "\"defaultStyle\":true," +
      "\"cartography\":\"http://localhost/api/cartographies/1228\"" +
      "}";

    String location = mvc.perform(
        post(URIConstants.CARTOGRAPHY_STYLES_URI)
          .content(content)
          .with(user(Fixtures.admin()))
      ).andExpect(status().isCreated())
      .andExpect(jsonPath("$.name").value("test"))
      .andReturn().getResponse().getHeader("Location");

    assertNotNull(location);

    mvc.perform(
        post(URIConstants.CARTOGRAPHY_STYLES_URI)
          .content(content)
          .with(user(Fixtures.admin()))
      ).andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.errors[0].property").value("defaultStyle"))
      .andExpect(jsonPath("$.errors[0].message").value("Already a default style exists for the cartography."));

    mvc.perform(delete(location)
      .with(user(Fixtures.admin()))
    ).andExpect(status().isNoContent());
  }

}
