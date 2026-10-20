package org.sitmun.domain.cartography;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
  @DisplayName("GET /api/cartographies?size=10000 returns 200 with liquibase seed")
  @WithMockUser(roles = "ADMIN")
  void listWithSize10000() throws Exception {
    mvc.perform(get("/api/cartographies").param("size", "10000")).andExpect(status().isOk());
  }
}
