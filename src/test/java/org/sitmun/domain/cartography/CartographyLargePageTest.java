package org.sitmun.domain.cartography;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Hibernate 7 + {@code @EntityGraph} including the {@code styles} bag on paged {@code findAll}
 * threw {@code FetchNotFoundException} for an existing {@code Service}, which blanked admin MIA
 * create (cartography autocomplete never loaded).
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Cartography large page Data REST")
class CartographyLargePageTest {

  @Autowired private MockMvc mvc;

  @Test
  @DisplayName("GET size 10000 includes seed cartography WMTS Bases - ICGC- Topo")
  @WithMockUser(roles = "ADMIN")
  void getSize10000IncludesSeedWmtsBases() throws Exception {
    mvc.perform(get("/api/cartographies").param("size", "10000"))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$._embedded.cartographies[*].name", hasItem("WMTS Bases - ICGC- Topo")));
  }
}
