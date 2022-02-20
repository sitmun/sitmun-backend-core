package org.sitmun.plugin.core.repository.rest;

import org.junit.jupiter.api.Disabled;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc

public class CartographyFiltersRepositoryDataRestTest {

  @Autowired
  private MockMvc mvc;

  @Test
  @Disabled("Potential freeze of active connections. @Transactional may be required in REST controllers.")
  public void newCartographyFilterCanBePosted() throws Exception {
    String content = "{" +
      "\"name\":\"test\"," +
      "\"type\":\"C\"," +
      "\"required\":true," +
      "\"cartography\":\"http://localhost/api/cartographies/1228\"" +
      "}";

    String location = mvc.perform(
      post(URIConstants.CARTOGRAPHY_FILTERS_URI)
        .content(content)
        .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
    ).andExpect(status().isCreated())
      .andExpect(jsonPath("$.name").value("test"))
      .andReturn().getResponse().getHeader("Location");

    assertNotNull(location);

    mvc.perform(get(location + "/cartography")
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME)))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.id").value(1228));

    mvc.perform(delete(location)
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
    ).andExpect(status().isNoContent());
  }

  @Test
  public void newCartographyFilterRequiresCartographyLink() throws Exception {
    String content = "{" +
      "\"name\":\"test\"," +
      "\"type\":\"C\"," +
      "\"required\":true" +
      "}";

    mvc.perform(
      post(URIConstants.CARTOGRAPHY_FILTERS_URI)
        .content(content)
        .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
    ).andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.errors[0].property").value("cartography"))
      .andExpect(jsonPath("$.errors[0].message").value("must not be null"));
  }

}
