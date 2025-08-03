package org.sitmun.domain.territory;

import static org.sitmun.test.DateTimeMatchers.isIso8601DateAndTime;
import static org.sitmun.test.TestUtils.*;
import static org.sitmun.test.URIConstants.*;
import static org.springframework.http.MediaType.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Territory Repository Data REST test")
class TerritoryRepositoryDataRestTest {

  @Autowired private MockMvc mvc;

  private MockHttpServletResponse response;

  @BeforeEach
  void setup() {
    response = null;
  }

  @Test
  @DisplayName("POST: minimum set of properties")
  @WithMockUser(roles = "ADMIN")
  void create() throws Exception {
    response =
        mvc.perform(
                MockMvcRequestBuilders.post(TERRITORIES_URI)
                    .content(
                        """
                        {
                        "name":"test",
                        "code":"test",
                        "blocked":false
                        }
                        """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.createdDate", isIso8601DateAndTime()))
            .andReturn()
            .getResponse();
  }

  @Test
  @DisplayName("POST: center without extension")
  @WithMockUser(roles = "ADMIN")
  void centerWithoutExtension() throws Exception {
    response =
        mvc.perform(
                post(TERRITORIES_URI)
                    .content(
                        """
                        {
                        "name":"test",
                        "code":"test",
                        "blocked":false,
                        "center": {"x": 10, "y": 20}
                        }"""))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.center.x").value(10))
            .andExpect(jsonPath("$.center.y").value(20))
            .andReturn()
            .getResponse();
  }

  @Test
  @DisplayName("GET: can list as admin")
  @WithMockUser(roles = "ADMIN")
  void getTerritoriesAsPublic() throws Exception {
    mvc.perform(get(TERRITORIES_URI)).andExpect(status().isOk());
  }

  @Test
  @DisplayName("POST: cannot access as public")
  void createTerritoriesAsPublicFails() throws Exception {
    mvc.perform(
            post(TERRITORIES_URI)
                .contentType(APPLICATION_JSON)
                .content(
                    asJsonString(
                        """
                  {"name":"test"}
                  """)))
        .andExpect(status().isUnauthorized())
        .andReturn();
  }

  @Test
  @DisplayName("GET: has link to task availabilities")
  @WithMockUser(roles = "ADMIN")
  void hasLinkToTaskAvailability() throws Exception {
    mvc.perform(get(TERRITORY_URI, 1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._links.taskAvailabilities").exists());
  }

  @Test
  @DisplayName("GET: has computed center")
  @WithMockUser(roles = "ADMIN")
  void hasComputedCenter() throws Exception {
    mvc.perform(get(TERRITORY_URI, 1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.center.x").value(422552.0))
        .andExpect(jsonPath("$.center.y").value(4623846.0));
  }

  @Test
  @DisplayName("GET: has link to cartography availabilities")
  @WithMockUser(roles = "ADMIN")
  void hasLinkToCartographyAvailabilities() throws Exception {
    mvc.perform(get(TERRITORY_URI, 1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._links.cartographyAvailabilities").exists());
  }

  @AfterEach
  @WithMockUser(roles = "ADMIN")
  void cleanup() throws Exception {
    if (response != null) {
      String location = response.getHeader("Location");
      if (location != null) {
        mvc.perform(MockMvcRequestBuilders.delete(location)).andExpect(status().isNoContent());
      }
    }
  }
}
