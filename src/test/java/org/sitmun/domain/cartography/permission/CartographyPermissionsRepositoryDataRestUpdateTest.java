package org.sitmun.domain.cartography.permission;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.sitmun.test.Fixtures.*;
import static org.sitmun.test.URIConstants.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.stream.Collectors;
import org.hamcrest.Matchers;
import org.json.JSONObject;
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
class CartographyPermissionsRepositoryDataRestUpdateTest {

  @Autowired private MockMvc mvc;

  @Test
  @DisplayName("PUT: Update a CartographyPermission")
  @WithMockUser(roles = "ADMIN")
  void itemResourceUpdate() throws Exception {
    String item =
        mvc.perform(get(CARTOGRAPHY_PERMISSION_URI, 1))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    JSONObject json = new JSONObject(item);
    String oldName = json.getString("name");
    json.put("name", "New name");

    mvc.perform(put(CARTOGRAPHY_PERMISSION_URI, 1).content(json.toString()))
        .andExpect(status().isOk());

    mvc.perform(get(CARTOGRAPHY_PERMISSION_URI, 1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("New name"));

    json.put("name", oldName);

    mvc.perform(put(CARTOGRAPHY_PERMISSION_URI, 1).content(json.toString()))
        .andExpect(status().isOk());

    mvc.perform(get(CARTOGRAPHY_PERMISSION_URI, 1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value(oldName));
  }

  @Test
  @DisplayName("PUT: Update a CartographyPermission association to roles")
  @WithMockUser(roles = "ADMIN")
  void associationResourceUpdate() throws Exception {
    String item =
        mvc.perform(get(CARTOGRAPHY_PERMISSION_ROLES_URI, 1))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    @SuppressWarnings("unchecked")
    List<String> links =
        (List<String>)
            JsonPath.parse(item).read("$._embedded.roles[*]._links.self.href", List.class);
    assertEquals(1, links.size());

    String update = links.stream().skip(1).collect(Collectors.joining("\n"));

    mvc.perform(
        put(CARTOGRAPHY_PERMISSION_ROLES_URI, 1).content(update).contentType("text/uri-list"));

    mvc.perform(get(CARTOGRAPHY_PERMISSION_ROLES_URI, 1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.roles[*]._links.self.href", hasSize(links.size() - 1)));

    update = String.join("\n", links);

    mvc.perform(
        put(CARTOGRAPHY_PERMISSION_ROLES_URI, 1).content(update).contentType("text/uri-list"));

    mvc.perform(get(CARTOGRAPHY_PERMISSION_ROLES_URI, 1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.roles[*]._links.self.href", hasSize(links.size())))
        .andExpect(
            jsonPath(
                "$._embedded.roles[*]._links.self.href",
                Matchers.containsInAnyOrder(links.toArray())));
  }
}
