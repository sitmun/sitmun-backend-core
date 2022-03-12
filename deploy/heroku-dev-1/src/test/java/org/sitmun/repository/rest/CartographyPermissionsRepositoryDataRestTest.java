package org.sitmun.repository.rest;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sitmun.common.domain.cartography.permission.CartographyPermission;
import org.sitmun.test.Fixtures;
import org.sitmun.test.URIConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc

public class CartographyPermissionsRepositoryDataRestTest {

  @Autowired
  private MockMvc mvc;

  @Test
  public void filterType() throws Exception {
    mvc.perform(get(URIConstants.CARTOGRAPHY_PERMISSIONS_URI_FILTER, CartographyPermission.TYPE_SITUATION_MAP)
        .with(SecurityMockMvcRequestPostProcessors.user(Fixtures.admin())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.*.*", hasSize(1)))
      .andExpect(jsonPath("$._embedded.cartography-groups[?(@.type == '" + CartographyPermission.TYPE_SITUATION_MAP + "')]", hasSize(1)));
    mvc.perform(get(URIConstants.CARTOGRAPHY_PERMISSIONS_URI_FILTER, CartographyPermission.TYPE_BACKGROUND_MAP)
        .with(SecurityMockMvcRequestPostProcessors.user(Fixtures.admin())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.*.*", hasSize(6)))
      .andExpect(jsonPath("$._embedded.cartography-groups[?(@.type == '" + CartographyPermission.TYPE_BACKGROUND_MAP + "')]", hasSize(6)));
    mvc.perform(get(URIConstants.CARTOGRAPHY_PERMISSIONS_URI_FILTER, "C")
        .with(SecurityMockMvcRequestPostProcessors.user(Fixtures.admin())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.*.*", hasSize(112)))
      .andExpect(jsonPath("$._embedded.cartography-groups[?(@.type == 'C')]", hasSize(112)));
  }

  @Test
  public void filterOrType() throws Exception {
    mvc.perform(get(URIConstants.CARTOGRAPHY_PERMISSIONS_URI_OR_FILTER, CartographyPermission.TYPE_SITUATION_MAP, "C")
        .with(SecurityMockMvcRequestPostProcessors.user(Fixtures.admin())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.*.*", hasSize(113)))
      .andExpect(jsonPath("$._embedded.cartography-groups[?(@.type == '" + CartographyPermission.TYPE_SITUATION_MAP + "')]", hasSize(1)))
      .andExpect(jsonPath("$._embedded.cartography-groups[?(@.type == 'C')]", hasSize(112)));
  }

  @Test
  public void rolesOfAPermissions() throws Exception {
    mvc.perform(get(URIConstants.CARTOGRAPHY_PERMISSION_ROLES_URI, 6)
        .with(SecurityMockMvcRequestPostProcessors.user(Fixtures.admin()))
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.*.*", hasSize(9)));
  }

  @Test
  @Disabled("Potential freeze of active connections. @Transactional may be required in REST controllers.")
  public void createPermission() throws Exception {
    String content = "{" +
      "\"name\":\"test\"," +
      "\"type\":\"C\"" +
      "}";
    String location = mvc.perform(post(URIConstants.CARTOGRAPHY_PERMISSIONS_URI)
        .content(content)
        .with(SecurityMockMvcRequestPostProcessors.user(Fixtures.admin()))
      )
      .andExpect(status().isCreated())
      .andReturn().getResponse().getHeader("Location");

    assertNotNull(location);

    String changedContent = "{" +
      "\"name\":\"test2\"," +
      "\"type\":\"M\"" +
      "}";

    mvc.perform(put(location).content(changedContent)
      .with(SecurityMockMvcRequestPostProcessors.user(Fixtures.admin()))
    ).andExpect(status().isOk());

    mvc.perform(delete(location)
      .with(SecurityMockMvcRequestPostProcessors.user(Fixtures.admin()))
    ).andExpect(status().isNoContent());
  }

  @Test
  public void cantChangeTypeWhenInUseAsSituationMap() throws Exception {
    String changedContent = "{" +
      "\"name\":\"test\"," +
      "\"type\":\"C\"" +
      "}";
    mvc.perform(put(URIConstants.CARTOGRAPHY_PERMISSION_URI, 132).content(changedContent)
        .with(SecurityMockMvcRequestPostProcessors.user(Fixtures.admin()))
      ).andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.errors[0].invalidValue").value("C"));
  }

  @Test
  public void cantChangeTypeWhenInUseAsBackgroundMap() throws Exception {
    String changedContent = "{" +
      "\"name\":\"test\"," +
      "\"type\":\"C\"" +
      "}";
    mvc.perform(put(URIConstants.CARTOGRAPHY_PERMISSION_URI, 129).content(changedContent)
        .with(SecurityMockMvcRequestPostProcessors.user(Fixtures.admin()))
      ).andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.errors[0].invalidValue").value("C"));
  }
}
