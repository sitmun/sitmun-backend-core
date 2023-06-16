package org.sitmun.authorization.controller;

import org.junit.jupiter.api.Test;
import org.sitmun.test.URIConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
class ClientConfigurationTerritoryControllerTest {

  @Value("${spring.data.rest.default-page-size}")
  private int pageSize;

  @Autowired
  private MockMvc mvc;

  @Test
  void readPublicUser() throws Exception {
    mvc.perform(get(URIConstants.CONFIG_CLIENT_TERRITORY_URI))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.content", hasSize(3)))
      .andExpect(jsonPath("$.content[*].name", hasItem("Provincia")));
  }

  @Test
  void readOtherUser() throws Exception {
    mvc.perform(get(URIConstants.CONFIG_CLIENT_TERRITORY_URI)
        .with(user("internal"))
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.content", hasSize(3)))
      .andExpect(jsonPath("$.size", is(pageSize)))
      .andExpect(jsonPath("$.number", is(0)))
      .andExpect(jsonPath("$.totalPages", is(1)))
      .andExpect(jsonPath("$.content[*].name", hasItem("Provincia")));
  }


  @Test
  void readOtherUserWithPagination() throws Exception {
    mvc.perform(get(URIConstants.CONFIG_CLIENT_TERRITORY_URI + "?size=1&page=1")
        .with(user("internal"))
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.content", hasSize(1)))
      .andExpect(jsonPath("$.size", is(1)))
      .andExpect(jsonPath("$.number", is(1)))
      .andExpect(jsonPath("$.totalPages", is(3)))
      .andExpect(jsonPath("$.content[*].name", hasItem("Municipio")));
  }

  @Test
  void readOtherUserWithPaginationOutOfBounds() throws Exception {
    mvc.perform(get(URIConstants.CONFIG_CLIENT_TERRITORY_URI + "?size=1&page=4")
        .with(user("internal"))
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.content").isEmpty())
      .andExpect(jsonPath("$.size", is(1)))
      .andExpect(jsonPath("$.number", is(4)))
      .andExpect(jsonPath("$.totalPages", is(3)));
  }
}
