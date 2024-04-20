package org.sitmun.domain.cartography.permission;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.test.Fixtures;
import org.sitmun.test.URIConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("CartographyPermissions Repository Data REST test")
class CartographyPermissionsRepositoryDataRestTest {

  @Autowired
  private MockMvc mvc;

  @Test
  @DisplayName("GET: Retrieve CartographyPermissions by type")
  void filterType() throws Exception {
    mvc.perform(get(URIConstants.CARTOGRAPHY_PERMISSIONS_URI_FILTER, CartographyPermission.TYPE_SITUATION_MAP)
        .with(SecurityMockMvcRequestPostProcessors.user(Fixtures.admin())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.*.*", hasSize(1)))
      .andExpect(jsonPath("$._embedded.cartography-groups[?(@.type == '" + CartographyPermission.TYPE_SITUATION_MAP + "')]", hasSize(1)));
    mvc.perform(get(URIConstants.CARTOGRAPHY_PERMISSIONS_URI_FILTER, CartographyPermission.TYPE_BACKGROUND_MAP)
        .with(SecurityMockMvcRequestPostProcessors.user(Fixtures.admin())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.*.*", hasSize(1)))
      .andExpect(jsonPath("$._embedded.cartography-groups[?(@.type == '" + CartographyPermission.TYPE_BACKGROUND_MAP + "')]", hasSize(1)));
    mvc.perform(get(URIConstants.CARTOGRAPHY_PERMISSIONS_URI_FILTER, "C")
        .with(SecurityMockMvcRequestPostProcessors.user(Fixtures.admin())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.*.*", hasSize(1)))
      .andExpect(jsonPath("$._embedded.cartography-groups[?(@.type == 'C')]", hasSize(1)));
  }

  @Test
  @DisplayName("GET: Retrieve CartographyPermissions by type")
  void filterOrType() throws Exception {
    mvc.perform(get(URIConstants.CARTOGRAPHY_PERMISSIONS_URI_OR_FILTER, CartographyPermission.TYPE_SITUATION_MAP, "C")
        .with(SecurityMockMvcRequestPostProcessors.user(Fixtures.admin())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.*.*", hasSize(2)))
      .andExpect(jsonPath("$._embedded.cartography-groups[?(@.type == '" + CartographyPermission.TYPE_SITUATION_MAP + "')]", hasSize(1)))
      .andExpect(jsonPath("$._embedded.cartography-groups[?(@.type == 'C')]", hasSize(1)));
  }

  @Test
  @DisplayName("GET: Retrieve roles associated to CartographyPermissions")
  void rolesOfAPermissions() throws Exception {
    mvc.perform(get(URIConstants.CARTOGRAPHY_PERMISSION_ROLES_URI, 1)
        .with(SecurityMockMvcRequestPostProcessors.user(Fixtures.admin()))
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.*.*", hasSize(1)));
  }

  @Test
  @DisplayName("POST: Create a CartographyPermission")
  void createPermission() throws Exception {
    String content = '{' +
      "\"name\":\"test\"," +
      "\"type\":\"C\"" +
            '}';
    String location = mvc.perform(post(URIConstants.CARTOGRAPHY_PERMISSIONS_URI)
        .content(content)
        .with(SecurityMockMvcRequestPostProcessors.user(Fixtures.admin()))
      )
      .andExpect(status().isCreated())
      .andReturn().getResponse().getHeader("Location");

    assertNotNull(location);

    String changedContent = '{' +
      "\"name\":\"test2\"," +
      "\"type\":\"M\"" +
            '}';

    mvc.perform(put(location).content(changedContent)
      .with(SecurityMockMvcRequestPostProcessors.user(Fixtures.admin()))
    ).andExpect(status().isOk());

    mvc.perform(delete(location)
      .with(SecurityMockMvcRequestPostProcessors.user(Fixtures.admin()))
    ).andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("PUT: cartography permission type cannot be updated when it is a situation map")
  void cantChangeTypeWhenInUseAsSituationMap() throws Exception {
    String changedContent = '{' +
      "\"name\":\"test\"," +
      "\"type\":\"C\"" +
            '}';
    mvc.perform(put(URIConstants.CARTOGRAPHY_PERMISSION_URI, 3).content(changedContent)
        .with(SecurityMockMvcRequestPostProcessors.user(Fixtures.admin()))
      ).andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.errors[0].invalidValue").value("C"));
  }

  @Test
  @DisplayName("PUT: cartography permission type cannot be updated when it is a background map")
  void cantChangeTypeWhenInUseAsBackgroundMap() throws Exception {
    String changedContent = '{' +
      "\"name\":\"test\"," +
      "\"type\":\"C\"" +
            '}';
    mvc.perform(put(URIConstants.CARTOGRAPHY_PERMISSION_URI, 2).content(changedContent)
        .with(SecurityMockMvcRequestPostProcessors.user(Fixtures.admin()))
      ).andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.errors[0].invalidValue").value("C"));
  }

  @Test
  @DisplayName("GET: Retrieve all background maps")
  void retrieveAllBackgroundMaps() throws Exception {
    mvc.perform(get(URIConstants.CARTOGRAPHY_PERMISSIONS_URI_FILTER, CartographyPermission.TYPE_BACKGROUND_MAP)
        .with(user(Fixtures.admin())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.*.*", hasSize(1)))
      .andExpect(jsonPath("$._embedded.cartography-groups", hasSize(1)));
  }

  @Test
  @DisplayName("GET: Retrieve all situation maps")
  void retrieveAllSituationMaps() throws Exception {
    mvc.perform(get(URIConstants.CARTOGRAPHY_PERMISSIONS_URI_FILTER, CartographyPermission.TYPE_SITUATION_MAP)
        .with(user(Fixtures.admin())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.*.*", hasSize(1)))
      .andExpect(jsonPath("$._embedded.cartography-groups", hasSize(1)));
  }
}
