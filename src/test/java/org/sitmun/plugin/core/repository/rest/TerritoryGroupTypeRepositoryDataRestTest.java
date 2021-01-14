package org.sitmun.plugin.core.repository.rest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.sitmun.plugin.core.config.RepositoryRestConfig;
import org.sitmun.plugin.core.domain.TerritoryGroupType;
import org.sitmun.plugin.core.repository.TerritoryGroupTypeRepository;
import org.sitmun.plugin.core.test.TestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.sitmun.plugin.core.test.TestConstants.SITMUN_ADMIN_USERNAME;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class TerritoryGroupTypeRepositoryDataRestTest {

  private static final String TERRITORY_URI = "http://localhost/api/territory-group-types";

  @Autowired
  private MockMvc mvc;

  @Autowired
  private TerritoryGroupTypeRepository repository;

  @Test
  public void mustNotBeNull() throws Exception {
    mvc.perform(post(TERRITORY_URI)
      .contentType(MediaType.APPLICATION_JSON)
      .content(TestUtils.asJsonString(TerritoryGroupType.builder().build()))
    ).andExpect(status().is4xxClientError())
        .andExpect(jsonPath("$.errors[0].property").value("name"))
        .andExpect(jsonPath("$.errors[0].message").value("must not be blank"));
  }

  @Test
  public void mustNotBeBlank() throws Exception {
    mvc.perform(post(TERRITORY_URI)
      .contentType(MediaType.APPLICATION_JSON)
      .content(TestUtils.asJsonString(TerritoryGroupType.builder().setName("   ").build()))
    ).andExpect(status().is4xxClientError())
        .andExpect(jsonPath("$.errors[0].property").value("name"))
        .andExpect(jsonPath("$.errors[0].message").value("must not be blank"));
  }

  @Test
  @WithMockUser(username = SITMUN_ADMIN_USERNAME)
  public void groupCanBeCreatedAndDeleted() throws Exception {
    long count = repository.count();
    MvcResult result = mvc.perform(post(TERRITORY_URI)
      .contentType(MediaType.APPLICATION_JSON)
      .content(TestUtils.asJsonString(TerritoryGroupType.builder().setName("Example").build()))
    ).andExpect(status().isCreated())
        .andReturn();
    assertThat(repository.count()).isEqualTo(count + 1);
    String location = result.getResponse().getHeader("Location");
    assertThat(location).isNotNull();
    mvc.perform(delete(location))
        .andExpect(status().isNoContent())
        .andReturn();
    assertThat(repository.count()).isEqualTo(count);
  }

  @TestConfiguration
  static class ContextConfiguration {
    @Bean
    public Validator validator() {
      return new LocalValidatorFactoryBean();
    }

    @Bean
    RepositoryRestConfigurer repositoryRestConfigurer() {
      return new RepositoryRestConfig(validator());
    }
  }
}
