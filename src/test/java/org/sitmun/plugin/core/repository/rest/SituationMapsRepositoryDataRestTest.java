package org.sitmun.plugin.core.repository.rest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sitmun.plugin.core.domain.CartographyPermission;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.sitmun.plugin.core.test.URIConstants.CARTOGRAPHY_PERMISSIONS_URI_FILTER;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
public class SituationMapsRepositoryDataRestTest {

  @Autowired
  private MockMvc mvc;

  @Test
  public void retrieveAll() throws Exception {
    mvc.perform(get(CARTOGRAPHY_PERMISSIONS_URI_FILTER, CartographyPermission.TYPE_SITUATION_MAP))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.*.*", hasSize(1)))
      .andExpect(jsonPath("$._embedded.cartography-groups", hasSize(1)));
  }

}
