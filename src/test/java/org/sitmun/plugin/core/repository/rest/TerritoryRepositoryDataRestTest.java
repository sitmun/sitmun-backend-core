package org.sitmun.plugin.core.repository.rest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sitmun.plugin.core.repository.TerritoryRepository;
import org.sitmun.plugin.core.test.TestUtils;
import org.sitmun.plugin.core.test.URIConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.sitmun.plugin.core.test.DateTimeMatchers.isIso8601DateAndTime;
import static org.sitmun.plugin.core.test.TestConstants.SITMUN_ADMIN_USERNAME;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Territory Repository Data REST test")
public class TerritoryRepositoryDataRestTest {

  @Autowired
  TerritoryRepository territoryRepository;
  @Autowired
  private MockMvc mvc;

  private MockHttpServletResponse response;

  @BeforeEach
  public void setup() {
    response = null;
  }

  @Test
  @DisplayName("POST: minimum set of properties")
  public void create() throws Exception {
    response = mvc.perform(post(URIConstants.TERRITORIES_URI)
      .content("{" +
        "\"name\":\"test\"," +
        "\"code\":\"test\"," +
        "\"blocked\":false" +
        "}")
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
    )
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.createdDate", isIso8601DateAndTime()))
      .andReturn().getResponse();
  }

  @Test
  @DisplayName("POST: center without extension")
  public void centerWithoutExtension() throws Exception {
    response = mvc.perform(post(URIConstants.TERRITORIES_URI)
      .content("{" +
        "\"name\":\"test\"," +
        "\"code\":\"test\"," +
        "\"blocked\":false," +
        "\"center\": {\"x\": 10, \"y\": 20}" +
        "}")
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
    )
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.center.x").value(10))
      .andExpect(jsonPath("$.center.y").value(20))
      .andReturn().getResponse();
  }

  @Test
  @DisplayName("GET: can list as admin")
  public void getTerritoriesAsPublic() throws Exception {
    mvc.perform(get(URIConstants.TERRITORIES_URI)
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME)))
      .andExpect(status().isOk());
  }

  @Test
  @DisplayName("GET: cannot access a public")
  public void createTerritoriesAsPublicFails() throws Exception {
    mvc.perform(post(URIConstants.TERRITORIES_URI)
      .contentType(MediaType.APPLICATION_JSON)
      .content(TestUtils.asJsonString("{" +
        "\"name\":\"test\"," +
        "}"))
    ).andExpect(status().isUnauthorized()).andReturn();

  }

  @Test
  @DisplayName("GET: has link to task availabilities")
  public void hasLinkToTaskAvailability() throws Exception {
    mvc.perform(get(URIConstants.TERRITORY_URI, 0)
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME)))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._links.taskAvailabilities").exists());
  }

  @Test
  @DisplayName("GET: has computed center")
  public void hasComputedCenter() throws Exception {
    mvc.perform(get(URIConstants.TERRITORY_URI, 0)
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME)))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.center.x").value(433957.5))
      .andExpect(jsonPath("$.center.y").value(4615120.5))
    ;
  }

  @Test
  @DisplayName("GET: has link to cartography availabilities")
  public void hasLinkToCartographyAvailabilities() throws Exception {
    mvc.perform(get(URIConstants.TERRITORY_URI, 0)
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME)))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._links.cartographyAvailabilities").exists());
  }

  @AfterEach
  public void cleanup() throws Exception {
    if (response != null) {
      String location = response.getHeader("Location");
      if (location != null) {
        mvc.perform(delete(location)
          .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
        ).andExpect(status().isNoContent());
      }
    }
  }
}
