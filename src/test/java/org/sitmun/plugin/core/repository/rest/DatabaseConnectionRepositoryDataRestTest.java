package org.sitmun.plugin.core.repository.rest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.sitmun.plugin.core.config.RepositoryRestConfig;
import org.sitmun.plugin.core.test.URIConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
public class DatabaseConnectionRepositoryDataRestTest {

  @Autowired
  private MockMvc mvc;

  @Test
  public void tasksLinksExist() throws Exception {
    mvc.perform(get(URIConstants.CONNECTIONS_URI))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.connections.*", hasSize(16)))
      .andExpect(jsonPath("$._embedded.connections[*]._links.tasks", hasSize(16)));
  }

  @Test
  public void cartographiesLinksExist() throws Exception {
    mvc.perform(get(URIConstants.CONNECTIONS_URI))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.connections.*", hasSize(16)))
      .andExpect(jsonPath("$._embedded.connections[*]._links.cartographies", hasSize(16)));
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
