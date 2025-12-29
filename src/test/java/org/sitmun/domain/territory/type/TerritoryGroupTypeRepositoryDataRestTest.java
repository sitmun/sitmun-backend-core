package org.sitmun.domain.territory.type;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.sitmun.test.TestUtils.*;
import static org.sitmun.test.URIConstants.*;
import static org.springframework.http.MediaType.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@Deprecated
@DisplayName("Territory Group Type Repository Data REST Test")
class TerritoryGroupTypeRepositoryDataRestTest {

  @Autowired private MockMvc mvc;

  @Autowired private TerritoryGroupTypeRepository repository;

  @Test
  @DisplayName("POST: Must not be null")
  @WithMockUser(roles = "ADMIN")
  void mustNotBeNull() throws Exception {
    mvc.perform(
            post(TERRITORY_GROUP_TYPES_URI + "?lang=EN")
                .contentType(APPLICATION_JSON)
                .content(asJsonString(TerritoryGroupType.builder().build())))
        .andExpect(status().is4xxClientError())
        .andExpect(jsonPath("$.errors[0].field").value("name"))
        .andExpect(jsonPath("$.errors[0].message").value("must not be blank"));
  }

  @Test
  @DisplayName("POST: Must not be blank")
  @WithMockUser(roles = "ADMIN")
  void mustNotBeBlank() throws Exception {
    mvc.perform(
            post(TERRITORY_GROUP_TYPES_URI + "?lang=EN")
                .contentType(APPLICATION_JSON)
                .content(asJsonString(TerritoryGroupType.builder().name("   ").build())))
        .andExpect(status().is4xxClientError())
        .andExpect(jsonPath("$.errors[0].field").value("name"))
        .andExpect(jsonPath("$.errors[0].message").value("must not be blank"));
  }

  @Test
  @DisplayName("POST/DELETE: Group can be created and deleted")
  @WithMockUser(roles = "ADMIN")
  void groupCanBeCreatedAndDeleted() throws Exception {
    long count = repository.count();
    MvcResult result =
        mvc.perform(
                post(TERRITORY_GROUP_TYPES_URI)
                    .contentType(APPLICATION_JSON)
                    .content(asJsonString(TerritoryGroupType.builder().name("Example").build())))
            .andExpect(status().isCreated())
            .andReturn();
    assertThat(repository.count()).isEqualTo(count + 1);
    String location = result.getResponse().getHeader("Location");
    assertThat(location).isNotNull();
    Assertions.assertNotNull(location);
    mvc.perform(delete(location)).andExpect(status().isNoContent()).andReturn();
    assertThat(repository.count()).isEqualTo(count);
  }
}
