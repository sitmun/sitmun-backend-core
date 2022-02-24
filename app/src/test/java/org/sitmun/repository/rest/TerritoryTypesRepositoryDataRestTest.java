package org.sitmun.repository.rest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sitmun.test.Fixtures;
import org.sitmun.test.URIConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("TerritoryTypesRepository Data REST test")
public class TerritoryTypesRepositoryDataRestTest {

  @Autowired
  private MockMvc mvc;

  @Test
  @DisplayName("GET: all types")
  public void allTypes() throws Exception {
    mvc.perform(MockMvcRequestBuilders.get(URIConstants.TERRITORY_TYPES_URI)
      .contentType(MediaType.APPLICATION_JSON)
      .with(SecurityMockMvcRequestPostProcessors.user(Fixtures.admin()))
    ).andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.territory-types", hasSize(8)));
  }

  @Test
  @DisplayName("GET: a type")
  public void oneType() throws Exception {
    mvc.perform(get(URIConstants.TERRITORY_TYPE_URI, 2)
      .contentType(MediaType.APPLICATION_JSON)
      .with(SecurityMockMvcRequestPostProcessors.user(Fixtures.admin()))
    ).andExpect(status().isOk())
      .andExpect(jsonPath("$.id").value(2))
      .andExpect(jsonPath("$.name").value("Consell Comarcal"))
      .andExpect(jsonPath("$.official").value(true))
      .andExpect(jsonPath("$.topType").value(false))
      .andExpect(jsonPath("$.bottomType").value(false))
    ;
  }
}
