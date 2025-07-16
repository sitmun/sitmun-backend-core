package org.sitmun.authorization.controller;

import org.junit.jupiter.api.DisplayName;
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
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("API Authorization and Configuration - Application territories endpoint")
class ClientConfigurationApplicationTerritoryControllerTest {

  @Value("${spring.data.rest.default-page-size}")
  private int pageSize;

  @Autowired
  private MockMvc mvc;

  @Test
  @DisplayName("GET: Page with public user")
  void readPublicUser() throws Exception {
    mvc.perform(get(URIConstants.CONFIG_CLIENT_APPLICATION_TERRITORIES_URI, 1))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.content", hasSize(3)))
      .andExpect(jsonPath("$.content[*].name", hasItem("Municipio B")));
  }

  @Test
  @DisplayName("GET: Page with authenticated user")
  void readOtherUser() throws Exception {
    mvc.perform(get(URIConstants.CONFIG_CLIENT_APPLICATION_TERRITORIES_URI, 1)
        .with(user("internal"))
      )
      .andDo(print())
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.content", hasSize(3)))
      .andExpect(jsonPath("$.size", is(pageSize)))
      .andExpect(jsonPath("$.number", is(0)))
      .andExpect(jsonPath("$.totalPages", is(1)))
      .andExpect(jsonPath("$.content[*].name", hasItems(
        "Provincia A",
        "Municipio B",
        "Otro C"
      )));
  }

  @Test
  @DisplayName("GET: Territories in an application are sorted in alphabetic order")
  void territoriesAreInAlphabeticOrder() throws Exception {
    mvc.perform(get(URIConstants.CONFIG_CLIENT_APPLICATION_TERRITORIES_URI, 1)
        .with(user("internal"))
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.content[*].name", containsInRelativeOrder(
        "Municipio B",
        "Otro C",
        "Provincia A"
      )));
  }

  @Test
  @DisplayName("GET: Get specific page")
  void readOtherUserWithPagination() throws Exception {
    mvc.perform(get(URIConstants.CONFIG_CLIENT_APPLICATION_TERRITORIES_URI + "?size=1&page=1", 1)
        .with(user("internal"))
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.content", hasSize(1)))
      .andExpect(jsonPath("$.size", is(1)))
      .andExpect(jsonPath("$.number", is(1)))
      .andExpect(jsonPath("$.totalPages", is(3)))
      .andExpect(jsonPath("$.content[*].name", hasItem("Otro C")));
  }

  @Test
  @DisplayName("GET: Request out of bounds")
  void readOtherUserWithPaginationOutOfBounds() throws Exception {
    mvc.perform(get(URIConstants.CONFIG_CLIENT_APPLICATION_TERRITORIES_URI + "?size=1&page=5", 1)
        .with(user("internal"))
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.content").isEmpty())
      .andExpect(jsonPath("$.size", is(1)))
      .andExpect(jsonPath("$.number", is(5)))
      .andExpect(jsonPath("$.totalPages", is(3)));
  }
}
