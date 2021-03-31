package org.sitmun.plugin.core.repository.rest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sitmun.plugin.core.domain.TerritoryGroupType;
import org.sitmun.plugin.core.repository.TerritoryGroupTypeRepository;
import org.sitmun.plugin.core.test.TestUtils;
import org.sitmun.plugin.core.test.URIConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.sitmun.plugin.core.test.TestConstants.SITMUN_ADMIN_USERNAME;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
public class TerritoryGroupTypeRepositoryDataRestTest {

  @Autowired
  private MockMvc mvc;

  @Autowired
  private TerritoryGroupTypeRepository repository;

  @Test
  public void mustNotBeNull() throws Exception {
    mvc.perform(post(URIConstants.TERRITORY_GROUP_TYPES_URI)
      .contentType(MediaType.APPLICATION_JSON)
      .content(TestUtils.asJsonString(TerritoryGroupType.builder().build()))
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
    ).andExpect(status().is4xxClientError())
      .andExpect(jsonPath("$.errors[0].property").value("name"))
      .andExpect(jsonPath("$.errors[0].message").value("must not be blank"));
  }

  @Test
  public void mustNotBeBlank() throws Exception {
    mvc.perform(post(URIConstants.TERRITORY_GROUP_TYPES_URI)
      .contentType(MediaType.APPLICATION_JSON)
      .content(TestUtils.asJsonString(TerritoryGroupType.builder().name("   ").build()))
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
    ).andExpect(status().is4xxClientError())
      .andExpect(jsonPath("$.errors[0].property").value("name"))
      .andExpect(jsonPath("$.errors[0].message").value("must not be blank"));
  }

  @Test
  public void groupCanBeCreatedAndDeleted() throws Exception {
    long count = repository.count();
    MvcResult result = mvc.perform(post(URIConstants.TERRITORY_GROUP_TYPES_URI)
      .contentType(MediaType.APPLICATION_JSON)
      .content(TestUtils.asJsonString(TerritoryGroupType.builder().name("Example").build()))
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
    ).andExpect(status().isCreated())
      .andReturn();
    assertThat(repository.count()).isEqualTo(count + 1);
    String location = result.getResponse().getHeader("Location");
    assertThat(location).isNotNull();
    mvc.perform(delete(location)
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
    )
      .andExpect(status().isNoContent())
      .andReturn();
    assertThat(repository.count()).isEqualTo(count);
  }

}
