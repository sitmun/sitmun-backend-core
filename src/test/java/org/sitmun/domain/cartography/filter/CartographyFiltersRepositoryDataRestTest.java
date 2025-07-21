package org.sitmun.domain.cartography.filter;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.test.Fixtures;
import org.sitmun.test.URIConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("CartographyFilters Repository Data REST test")
class CartographyFiltersRepositoryDataRestTest {

  @Autowired private MockMvc mvc;

  @Test
  @DisplayName("POST: Create a CartographyFilter")
  void newCartographyFilterCanBePosted() throws Exception {
    String content =
        '{'
            + "\"name\":\"test\","
            + "\"type\":\"C\","
            + "\"required\":true,"
            + "\"cartography\":\"http://localhost/api/cartographies/1\""
            + '}';

    String location =
        mvc.perform(
                post(URIConstants.CARTOGRAPHY_FILTERS_URI)
                    .content(content)
                    .with(user(Fixtures.admin())))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("test"))
            .andReturn()
            .getResponse()
            .getHeader("Location");

    assertNotNull(location);

    mvc.perform(get(location + "/cartography").with(user(Fixtures.admin())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1));

    mvc.perform(delete(location).with(user(Fixtures.admin()))).andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("POST: No two default CartographyFilter can be created for the same Cartography")
  void newCartographyFilterRequiresCartographyLink() throws Exception {
    String content = '{' + "\"name\":\"test\"," + "\"type\":\"C\"," + "\"required\":true" + '}';

    mvc.perform(
            post(URIConstants.CARTOGRAPHY_FILTERS_URI + "?lang=EN")
                .content(content)
                .with(user(Fixtures.admin())))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].property").value("cartography"))
        .andExpect(jsonPath("$.errors[0].message").value("must not be null"));
  }
}
