package org.sitmun.plugin.core.repository.rest;

import com.jayway.jsonpath.JsonPath;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.sitmun.plugin.core.test.URIConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.sitmun.plugin.core.test.TestConstants.SITMUN_ADMIN_USERNAME;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc

public class RepositoryDataRestUpdateTest {

  @Autowired
  private MockMvc mvc;

  @Test
  public void itemResourceUpdate() throws Exception {
    String item = mvc.perform(get(URIConstants.CARTOGRAPHY_PERMISSION_URI, 132)
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
    )
      .andExpect(status().isOk())
      .andReturn().getResponse().getContentAsString();

    JSONObject json = new JSONObject(item);
    String oldName = json.getString("name");
    json.put("name", "New name");

    mvc.perform(put(URIConstants.CARTOGRAPHY_PERMISSION_URI, 132).content(json.toString())
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
    )
      .andExpect(status().isOk());

    mvc.perform(get(URIConstants.CARTOGRAPHY_PERMISSION_URI, 132)
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
    )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.name").value("New name"));

    json.put("name", oldName);

    mvc.perform(put(URIConstants.CARTOGRAPHY_PERMISSION_URI, 132).content(json.toString())
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
    )
      .andExpect(status().isOk());

    mvc.perform(get(URIConstants.CARTOGRAPHY_PERMISSION_URI, 132)
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
    )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.name").value(oldName));
  }

  @Test
  public void associationResourceUpdate() throws Exception {
    String item = mvc.perform(get(URIConstants.CARTOGRAPHY_PERMISSION_ROLES_URI, 132)
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
    )
      .andExpect(status().isOk())
      .andReturn().getResponse().getContentAsString();

    @SuppressWarnings("unchecked")
    List<String> links = (List<String>) JsonPath.parse(item).read("$._embedded.roles[*]._links.self.href", List.class);

    String update = links.stream().skip(1).collect(Collectors.joining("\n"));

    mvc.perform(put(URIConstants.CARTOGRAPHY_PERMISSION_ROLES_URI, 132).content(update).contentType("text/uri-list")
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
    );

    mvc.perform(get(URIConstants.CARTOGRAPHY_PERMISSION_ROLES_URI, 132)
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
    )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.roles[*]._links.self.href", hasSize(links.size() - 1)));

    update = String.join("\n", links);

    mvc.perform(put(URIConstants.CARTOGRAPHY_PERMISSION_ROLES_URI, 132).content(update).contentType("text/uri-list")
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
    );

    mvc.perform(get(URIConstants.CARTOGRAPHY_PERMISSION_ROLES_URI, 132)
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
    )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.roles[*]._links.self.href", hasSize(links.size())))
      .andExpect(jsonPath("$._embedded.roles[*]._links.self.href", containsInAnyOrder(links.toArray())));
  }


}
