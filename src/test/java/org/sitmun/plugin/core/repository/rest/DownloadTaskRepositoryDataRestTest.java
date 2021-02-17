package org.sitmun.plugin.core.repository.rest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.sitmun.plugin.core.config.RepositoryRestConfig;
import org.sitmun.plugin.core.repository.TaskAvailabilityRepository;
import org.sitmun.plugin.core.repository.TaskParameterRepository;
import org.sitmun.plugin.core.repository.TaskRepository;
import org.sitmun.plugin.core.repository.TerritoryRepository;
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
public class DownloadTaskRepositoryDataRestTest {

  private static final String PUBLIC_USERNAME = "public";
  @Autowired
  TaskRepository taskRepository;
  @Autowired
  TaskAvailabilityRepository taskAvailabilityRepository;
  @Autowired
  TaskParameterRepository taskParameterRepository;
  @Autowired
  TerritoryRepository territoryRepository;
  @Autowired
  private MockMvc mvc;

  @Test
  public void filterScope() throws Exception {
    mvc.perform(get(URIConstants.DOWNLOAD_TASKS_URI + "?scope=U&size=10"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.download-tasks", hasSize(10)))
      .andExpect(jsonPath("$._embedded.download-tasks[?(@.scope == 'U')]", hasSize(10)));
    mvc.perform(get(URIConstants.DOWNLOAD_TASKS_URI + "?scope=A"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.download-tasks", hasSize(38)))
      .andExpect(jsonPath("$._embedded.download-tasks[?(@.scope == 'A')]", hasSize(38)));
    mvc.perform(get(URIConstants.DOWNLOAD_TASKS_URI + "?scope=C"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.download-tasks", hasSize(47)))
      .andExpect(jsonPath("$._embedded.download-tasks[?(@.scope == 'C')]", hasSize(47)));
  }

  @Test
  public void filterScopeOr() throws Exception {
    mvc.perform(get(URIConstants.DOWNLOAD_TASKS_URI + "?scope=A&scope=C"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.download-tasks", hasSize(85)))
      .andExpect(jsonPath("$._embedded.download-tasks[?(@.scope == 'A')]", hasSize(38)))
      .andExpect(jsonPath("$._embedded.download-tasks[?(@.scope == 'C')]", hasSize(47)))
      .andExpect(jsonPath("$._embedded.download-tasks[?(@.scope == 'C' || @.scope == 'A')]",
        hasSize(85)));
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
