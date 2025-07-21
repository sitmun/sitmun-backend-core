package org.sitmun.authorization.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.test.URIConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("API Authorization and Configuration - Application endpoint")
class ClientConfigurationApplicationControllerTest {

  @Value("${spring.data.rest.default-page-size}")
  private int pageSize;

  @Autowired private MockMvc mvc;

  @Test
  @DisplayName("GET: Page with public user")
  void readPublicUser() throws Exception {
    mvc.perform(get(URIConstants.CONFIG_CLIENT_APPLICATION_URI))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(4)))
        .andExpect(jsonPath("$.content[*].title", hasItem("SITMUN - Municipal")));
  }

  @Test
  @DisplayName("GET: Page with authenticated user")
  void readOtherUser() throws Exception {
    mvc.perform(get(URIConstants.CONFIG_CLIENT_APPLICATION_URI).with(user("internal")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(5)))
        .andExpect(jsonPath("$.size", is(pageSize)))
        .andExpect(jsonPath("$.number", is(0)))
        .andExpect(jsonPath("$.totalPages", is(1)))
        .andExpect(
            jsonPath(
                "$.content[*].title",
                hasItems(
                    "SITMUN - Municipal",
                    "SITMUN - Provincial",
                    "SITMUN - Supramunicipal",
                    "SITMUN - Externa",
                    "SITMUN - Externa protegida")));
  }

  @Test
  @DisplayName("GET: Get specific page")
  void readOtherUserWithPagination() throws Exception {
    mvc.perform(
            get(URIConstants.CONFIG_CLIENT_APPLICATION_URI + "?size=1&page=1")
                .with(user("internal")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.size", is(1)))
        .andExpect(jsonPath("$.number", is(1)))
        .andExpect(jsonPath("$.totalPages", is(5)))
        .andExpect(jsonPath("$.content[*].title", hasItem("SITMUN - Externa protegida")));
  }

  @Test
  @DisplayName("GET: Request out of bounds")
  void readOtherUserWithPaginationOutOfBounds() throws Exception {
    mvc.perform(
            get(URIConstants.CONFIG_CLIENT_APPLICATION_URI + "?size=1&page=5")
                .with(user("internal")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isEmpty())
        .andExpect(jsonPath("$.size", is(1)))
        .andExpect(jsonPath("$.number", is(5)))
        .andExpect(jsonPath("$.totalPages", is(5)));
  }
}
