package org.sitmun.domain.cartography.permission;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.sitmun.domain.cartography.permission.CartographyPermission.*;
import static org.sitmun.test.Fixtures.*;
import static org.sitmun.test.URIConstants.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
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
@DisplayName("CartographyPermissions Repository Data REST test")
class CartographyPermissionsRepositoryDataRestTest {

  @Autowired private MockMvc mvc;

  @Test
  @DisplayName("GET: Retrieve CartographyPermissions by type")
  @WithMockUser(roles = "ADMIN")
  void filterType() throws Exception {
    mvc.perform(get(CARTOGRAPHY_PERMISSIONS_URI_FILTER, TYPE_SITUATION_MAP))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.*.*", hasSize(1)))
        .andExpect(
            jsonPath(
                "$._embedded.cartography-groups[?(@.type == '" + TYPE_SITUATION_MAP + "')]",
                hasSize(1)));
    mvc.perform(get(CARTOGRAPHY_PERMISSIONS_URI_FILTER, TYPE_BACKGROUND_MAP))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.*.*", hasSize(1)))
        .andExpect(
            jsonPath(
                "$._embedded.cartography-groups[?(@.type == '" + TYPE_BACKGROUND_MAP + "')]",
                hasSize(1)));
    mvc.perform(get(CARTOGRAPHY_PERMISSIONS_URI_FILTER, "C"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.*.*", hasSize(1)))
        .andExpect(jsonPath("$._embedded.cartography-groups[?(@.type == 'C')]", hasSize(1)));
  }

  @Test
  @DisplayName("GET: Retrieve CartographyPermissions by type")
  @WithMockUser(roles = "ADMIN")
  void filterOrType() throws Exception {
    mvc.perform(get(CARTOGRAPHY_PERMISSIONS_URI_OR_FILTER, TYPE_SITUATION_MAP, "C"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.*.*", hasSize(2)))
        .andExpect(
            jsonPath(
                "$._embedded.cartography-groups[?(@.type == '" + TYPE_SITUATION_MAP + "')]",
                hasSize(1)))
        .andExpect(jsonPath("$._embedded.cartography-groups[?(@.type == 'C')]", hasSize(1)));
  }

  @Test
  @DisplayName("GET: Retrieve roles associated to CartographyPermissions")
  @WithMockUser(roles = "ADMIN")
  void rolesOfAPermissions() throws Exception {
    mvc.perform(get(CARTOGRAPHY_PERMISSION_ROLES_URI, 1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.*.*", hasSize(1)));
  }

  @Test
  @DisplayName("POST: Create a CartographyPermission")
  @WithMockUser(roles = "ADMIN")
  void createPermission() throws Exception {
    String content =
        """
      {
      "name":"test",
      "type":"C"
      }""";
    String location =
        mvc.perform(post(CARTOGRAPHY_PERMISSIONS_URI).content(content))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getHeader("Location");

    assertNotNull(location);

    String changedContent =
        """
      {
      "name":"test2",
      "type":"M"
      }
      """;

    mvc.perform(put(location).content(changedContent)).andExpect(status().isOk());

    mvc.perform(delete(location)).andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("PUT: cartography permission type cannot be updated when it is a situation map")
  @WithMockUser(roles = "ADMIN")
  void cantChangeTypeWhenInUseAsSituationMap() throws Exception {
    String changedContent =
        """
      {
      "name":"test",
      "type":"C"
      }""";
    mvc.perform(put(CARTOGRAPHY_PERMISSION_URI, 3).content(changedContent))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].invalidValue").value("C"));
  }

  @Test
  @DisplayName("PUT: cartography permission type cannot be updated when it is a background map")
  @WithMockUser(roles = "ADMIN")
  void cantChangeTypeWhenInUseAsBackgroundMap() throws Exception {
    String changedContent =
        """
      {
      "name":"test",
      "type":"C"
      }
      """;
    mvc.perform(put(CARTOGRAPHY_PERMISSION_URI, 2).content(changedContent))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].invalidValue").value("C"));
  }

  @Test
  @DisplayName("GET: Retrieve all background maps")
  @WithMockUser(roles = "ADMIN")
  void retrieveAllBackgroundMaps() throws Exception {
    mvc.perform(get(CARTOGRAPHY_PERMISSIONS_URI_FILTER, TYPE_BACKGROUND_MAP))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.*.*", hasSize(1)))
        .andExpect(jsonPath("$._embedded.cartography-groups", hasSize(1)));
  }

  @Test
  @DisplayName("GET: Retrieve all situation maps")
  @WithMockUser(roles = "ADMIN")
  void retrieveAllSituationMaps() throws Exception {
    mvc.perform(get(CARTOGRAPHY_PERMISSIONS_URI_FILTER, TYPE_SITUATION_MAP))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.*.*", hasSize(1)))
        .andExpect(jsonPath("$._embedded.cartography-groups", hasSize(1)));
  }
}
