package org.sitmun.domain.cartography.filter;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.sitmun.test.URIConstants.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("CartographyFilters Repository Data REST test")
class CartographyFiltersRepositoryDataRestTest {

  @Autowired private MockMvc mvc;

  @Test
  @DisplayName("POST: Create a CartographyFilter")
  @WithMockUser(roles = "ADMIN")
  void newCartographyFilterCanBePosted() throws Exception {
    String content =
        """
        {
        "name":"test",
        "type":"C",
        "required":true,
        "cartography":"http://localhost/api/cartographies/1"
        }
        """;

    String location =
        mvc.perform(post(CARTOGRAPHY_FILTERS_URI).content(content))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("test"))
            .andReturn()
            .getResponse()
            .getHeader("Location");

    assertNotNull(location);

    mvc.perform(get(location + "/cartography"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1));

    mvc.perform(delete(location)).andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("POST: No two default CartographyFilter can be created for the same Cartography")
  @WithMockUser(roles = "ADMIN")
  void newCartographyFilterRequiresCartographyLink() throws Exception {
    String content =
        """
      {
      "name":"test",
      "type":"C",
      "required":true
      }""";

    mvc.perform(post(CARTOGRAPHY_FILTERS_URI + "?lang=EN").content(content))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("cartography"))
        .andExpect(jsonPath("$.errors[0].message").value("must not be null"));
  }
}
