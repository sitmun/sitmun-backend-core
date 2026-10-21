package org.sitmun.domain.territory.type;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sitmun.test.URIConstants.TERRITORY_GROUP_TYPES_URI;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import java.util.ArrayList;
import java.util.stream.Collectors;
import net.minidev.json.JSONArray;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Deprecated
class TerritoryGroupTypeRepositoryIntegrationTest {

  @Autowired TerritoryGroupTypeRepository territoryGroupTypeRepository;
  @Autowired private MockMvc mvc;
  private ArrayList<TerritoryGroupType> territoryGroupTypes;

  @BeforeEach
  @WithMockUser(roles = "ADMIN")
  void setup() {
    territoryGroupTypes = new ArrayList<>();
    territoryGroupTypes.add(
        territoryGroupTypeRepository.save(
            TerritoryGroupType.builder().name("TerritoryGroupTypeTest_1").build()));
    territoryGroupTypes.add(
        territoryGroupTypeRepository.save(
            TerritoryGroupType.builder().name("TerritoryGroupTypeTest_2").build()));
  }

  @AfterEach
  @WithMockUser(roles = "ADMIN")
  void cleanup() {
    territoryGroupTypeRepository.deleteAll(territoryGroupTypes);
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void requestRoles() throws Exception {
    String body =
        mvc.perform(get(TERRITORY_GROUP_TYPES_URI))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    DocumentContext context = JsonPath.parse(body);

    assertThat(context.read("$._embedded.territory-group-types[*].id", JSONArray.class))
        .containsAll(
            territoryGroupTypes.stream()
                .map(TerritoryGroupType::getId)
                .collect(Collectors.toList()));
    assertThat(context.read("$._embedded.territory-group-types[*].name", JSONArray.class))
        .containsAll(
            territoryGroupTypes.stream()
                .map(TerritoryGroupType::getName)
                .collect(Collectors.toList()));
  }
}
