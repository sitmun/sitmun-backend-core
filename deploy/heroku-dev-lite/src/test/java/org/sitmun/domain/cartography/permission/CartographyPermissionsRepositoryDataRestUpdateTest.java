package org.sitmun.domain.cartography.permission;

import com.jayway.jsonpath.JsonPath;
import org.hamcrest.Matchers;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.test.Fixtures;
import org.sitmun.test.URIConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("CartographyPermissions Repository Data REST test")
class CartographyPermissionsRepositoryDataRestUpdateTest {

  @Autowired
  private MockMvc mvc;

  @Test
  @DisplayName("PUT: Update a CartographyPermission")
  void itemResourceUpdate() throws Exception {
    String item = mvc.perform(get(URIConstants.CARTOGRAPHY_PERMISSION_URI, 1)
        .with(user(Fixtures.admin()))
      )
      .andExpect(status().isOk())
      .andReturn().getResponse().getContentAsString();

    JSONObject json = new JSONObject(item);
    String oldName = json.getString("name");
    json.put("name", "New name");

    mvc.perform(put(URIConstants.CARTOGRAPHY_PERMISSION_URI, 1).content(json.toString())
        .with(user(Fixtures.admin()))
      )
      .andExpect(status().isOk());

    mvc.perform(get(URIConstants.CARTOGRAPHY_PERMISSION_URI, 1)
        .with(user(Fixtures.admin()))
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.name").value("New name"));

    json.put("name", oldName);

    mvc.perform(put(URIConstants.CARTOGRAPHY_PERMISSION_URI, 1).content(json.toString())
        .with(user(Fixtures.admin()))
      )
      .andExpect(status().isOk());

    mvc.perform(get(URIConstants.CARTOGRAPHY_PERMISSION_URI, 1)
        .with(user(Fixtures.admin()))
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.name").value(oldName));
  }

  @Test
  @DisplayName("PUT: Update a CartographyPermission association to roles")
  void associationResourceUpdate() throws Exception {
    String item = mvc.perform(get(URIConstants.CARTOGRAPHY_PERMISSION_ROLES_URI, 1)
        .with(user(Fixtures.admin()))
      )
      .andExpect(status().isOk())
      .andReturn().getResponse().getContentAsString();

    @SuppressWarnings("unchecked")
    List<String> links = (List<String>) JsonPath.parse(item).read("$._embedded.roles[*]._links.self.href", List.class);
    assertEquals(1, links.size());

    String update = links.stream().skip(1).collect(Collectors.joining("\n"));

    mvc.perform(put(URIConstants.CARTOGRAPHY_PERMISSION_ROLES_URI, 1).content(update).contentType("text/uri-list")
      .with(user(Fixtures.admin()))
    );

    mvc.perform(get(URIConstants.CARTOGRAPHY_PERMISSION_ROLES_URI, 1)
        .with(user(Fixtures.admin()))
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.roles[*]._links.self.href", hasSize(links.size() - 1)));

    update = String.join("\n", links);

    mvc.perform(put(URIConstants.CARTOGRAPHY_PERMISSION_ROLES_URI, 1).content(update).contentType("text/uri-list")
      .with(user(Fixtures.admin()))
    );

    mvc.perform(get(URIConstants.CARTOGRAPHY_PERMISSION_ROLES_URI, 1)
        .with(user(Fixtures.admin()))
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.roles[*]._links.self.href", hasSize(links.size())))
      .andExpect(jsonPath("$._embedded.roles[*]._links.self.href", Matchers.containsInAnyOrder(links.toArray())));
  }


}
