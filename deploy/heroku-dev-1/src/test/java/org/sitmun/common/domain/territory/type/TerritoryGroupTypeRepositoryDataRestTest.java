package org.sitmun.common.domain.territory.type;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sitmun.common.domain.territory.type.TerritoryGroupType;
import org.sitmun.common.domain.territory.type.TerritoryGroupTypeRepository;
import org.sitmun.test.Fixtures;
import org.sitmun.test.TestUtils;
import org.sitmun.test.URIConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@Deprecated
public class TerritoryGroupTypeRepositoryDataRestTest {

  @Autowired
  private MockMvc mvc;

  @Autowired
  private TerritoryGroupTypeRepository repository;

  @Test
  public void mustNotBeNull() throws Exception {
    mvc.perform(post(URIConstants.TERRITORY_GROUP_TYPES_URI+"?lang=EN")
        .contentType(MediaType.APPLICATION_JSON)
        .content(TestUtils.asJsonString(TerritoryGroupType.builder().build()))
        .with(user(Fixtures.admin()))
      ).andExpect(status().is4xxClientError())
      .andExpect(jsonPath("$.errors[0].property").value("name"))
      .andExpect(jsonPath("$.errors[0].message").value("must not be blank"));
  }

  @Test
  public void mustNotBeBlank() throws Exception {
    mvc.perform(post(URIConstants.TERRITORY_GROUP_TYPES_URI+"?lang=EN")
        .contentType(MediaType.APPLICATION_JSON)
        .content(TestUtils.asJsonString(TerritoryGroupType.builder().name("   ").build()))
        .with(user(Fixtures.admin()))
      ).andExpect(status().is4xxClientError())
      .andExpect(jsonPath("$.errors[0].property").value("name"))
      .andExpect(jsonPath("$.errors[0].message").value("must not be blank"));
  }

  @Test
  @Disabled("Potential freeze of active connections. @Transactional may be required in REST controllers.")
  public void groupCanBeCreatedAndDeleted() throws Exception {
    long count = repository.count();
    MvcResult result = mvc.perform(post(URIConstants.TERRITORY_GROUP_TYPES_URI)
        .contentType(MediaType.APPLICATION_JSON)
        .content(TestUtils.asJsonString(TerritoryGroupType.builder().name("Example").build()))
        .with(user(Fixtures.admin()))
      ).andExpect(status().isCreated())
      .andReturn();
    assertThat(repository.count()).isEqualTo(count + 1);
    String location = result.getResponse().getHeader("Location");
    assertThat(location).isNotNull();
    mvc.perform(delete(location)
        .with(user(Fixtures.admin()))
      )
      .andExpect(status().isNoContent())
      .andReturn();
    assertThat(repository.count()).isEqualTo(count);
  }

}
