package org.sitmun.domain.territory.type;

import static org.hamcrest.Matchers.hasSize;
import static org.sitmun.test.URIConstants.*;
import static org.springframework.http.MediaType.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
@DisplayName("TerritoryTypesRepository Data REST test")
class TerritoryTypesRepositoryDataRestTest {

  @Autowired private MockMvc mvc;

  @Test
  @DisplayName("GET: all types")
  @WithMockUser(roles = "ADMIN")
  void allTypes() throws Exception {
    mvc.perform(get(TERRITORY_TYPES_URI).contentType(APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.territory-types", hasSize(8)));
  }

  @Test
  @DisplayName("GET: a type")
  @WithMockUser(roles = "ADMIN")
  void oneType() throws Exception {
    mvc.perform(get(TERRITORY_TYPE_URI, 2).contentType(APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(2))
        .andExpect(jsonPath("$.name").value("Consell Comarcal"))
        .andExpect(jsonPath("$.official").value(true))
        .andExpect(jsonPath("$.topType").value(false))
        .andExpect(jsonPath("$.bottomType").value(false));
  }
}
