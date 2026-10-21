package org.sitmun.domain.territory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sitmun.test.URIConstants.TERRITORIES_URI;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONArray;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class TerritoryRepositoryIntegrationTest {

  @Autowired private MockMvc mvc;

  @Test
  @WithMockUser(roles = "ADMIN")
  void requestMembers() throws Exception {
    String body =
        mvc.perform(get(TERRITORIES_URI + "/1/members"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    DocumentContext context = JsonPath.parse(body);

    assertThat(context.read("$._embedded.territories[*]", JSONArray.class)).hasSize(2);
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void requestMemberOf() throws Exception {
    String body =
        mvc.perform(get(TERRITORIES_URI + "/2/memberOf"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    DocumentContext context = JsonPath.parse(body);

    assertThat(context.read("$._embedded.territories[*]", JSONArray.class)).hasSize(1);
  }
}
