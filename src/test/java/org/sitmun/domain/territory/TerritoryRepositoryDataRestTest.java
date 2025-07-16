package org.sitmun.domain.territory;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.test.Fixtures;
import org.sitmun.test.TestUtils;
import org.sitmun.test.URIConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.sitmun.test.DateTimeMatchers.isIso8601DateAndTime;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Territory Repository Data REST test")
class TerritoryRepositoryDataRestTest {

  @Autowired
  private MockMvc mvc;

  private MockHttpServletResponse response;

  @BeforeEach
  void setup() {
    response = null;
  }

  @Test
  @DisplayName("POST: minimum set of properties")
  void create() throws Exception {
    response = mvc.perform(MockMvcRequestBuilders.post(URIConstants.TERRITORIES_URI)
        .content("{" +
          "\"name\":\"test\"," +
          "\"code\":\"test\"," +
          "\"blocked\":false" +
          "}")
        .with(user(Fixtures.admin()))
      )
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.createdDate", isIso8601DateAndTime()))
      .andReturn().getResponse();
  }

  @Test
  @DisplayName("POST: center without extension")
  void centerWithoutExtension() throws Exception {
    response = mvc.perform(post(URIConstants.TERRITORIES_URI)
        .content("{" +
          "\"name\":\"test\"," +
          "\"code\":\"test\"," +
          "\"blocked\":false," +
          "\"center\": {\"x\": 10, \"y\": 20}" +
          "}")
        .with(user(Fixtures.admin()))
      )
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.center.x").value(10))
      .andExpect(jsonPath("$.center.y").value(20))
      .andReturn().getResponse();
  }

  @Test
  @DisplayName("GET: can list as admin")
  void getTerritoriesAsPublic() throws Exception {
    mvc.perform(get(URIConstants.TERRITORIES_URI)
        .with(user(Fixtures.admin())))
      .andExpect(status().isOk());
  }

  @Test
  @DisplayName("POST: cannot access as public")
  void createTerritoriesAsPublicFails() throws Exception {
    mvc.perform(post(URIConstants.TERRITORIES_URI)
      .contentType(MediaType.APPLICATION_JSON)
      .content(TestUtils.asJsonString("{" +
        "\"name\":\"test\"," +
        "}"))
    ).andExpect(status().isUnauthorized()).andReturn();

  }

  @Test
  @DisplayName("GET: has link to task availabilities")
  void hasLinkToTaskAvailability() throws Exception {
    mvc.perform(get(URIConstants.TERRITORY_URI, 1)
        .with(user(Fixtures.admin())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._links.taskAvailabilities").exists());
  }

  @Test
  @DisplayName("GET: has computed center")
  void hasComputedCenter() throws Exception {
    mvc.perform(get(URIConstants.TERRITORY_URI, 1)
        .with(user(Fixtures.admin())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.center.x").value(422552.0))
      .andExpect(jsonPath("$.center.y").value(4623846.0))
    ;
  }

  @Test
  @DisplayName("GET: has link to cartography availabilities")
  void hasLinkToCartographyAvailabilities() throws Exception {
    mvc.perform(get(URIConstants.TERRITORY_URI, 1)
        .with(user(Fixtures.admin())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._links.cartographyAvailabilities").exists());
  }

  @AfterEach
  void cleanup() throws Exception {
    if (response != null) {
      String location = response.getHeader("Location");
      if (location != null) {
        mvc.perform(MockMvcRequestBuilders.delete(location)
          .with(user(Fixtures.admin()))
        ).andExpect(status().isNoContent());
      }
    }
  }
}
