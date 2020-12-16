package org.sitmun.plugin.core.repository;

import static org.hamcrest.Matchers.hasSize;
import static org.sitmun.plugin.core.security.SecurityConstants.HEADER_STRING;
import static org.sitmun.plugin.core.security.SecurityConstants.TOKEN_PREFIX;
import static org.sitmun.plugin.core.test.TestConstants.SITMUN_ADMIN_USERNAME;
import static org.sitmun.plugin.core.test.TestUtils.withMockSitmunAdmin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sitmun.plugin.core.config.RepositoryRestConfig;
import org.sitmun.plugin.core.security.TokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class DownloadTaskRepositoryDataRestTest {

  private static final String TASK_URI = "http://localhost/api/download-tasks";
  private static final String PUBLIC_USERNAME = "public";
  @Autowired
  TaskRepository taskRepository;
  @Autowired
  TaskAvailabilityRepository taskAvailabilityRepository;
  @Autowired
  TaskParameterRepository taskParameterRepository;
  @Autowired
  TokenProvider tokenProvider;
  @Autowired
  TerritoryRepository territoryRepository;
  @Autowired
  private MockMvc mvc;

  private String token;

  @Before
  public void init() {
    withMockSitmunAdmin(() -> token = tokenProvider.createToken(SITMUN_ADMIN_USERNAME));
  }

  @Test
  public void filterScope() throws Exception {
    mvc.perform(get(TASK_URI + "?scope=U")
        .header(HEADER_STRING, TOKEN_PREFIX + token))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.download-tasks", hasSize(977)))
        .andExpect(jsonPath("$._embedded.download-tasks[?(@.scope == 'U')]", hasSize(977)));
    mvc.perform(get(TASK_URI + "?scope=A")
        .header(HEADER_STRING, TOKEN_PREFIX + token))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.download-tasks", hasSize(38)))
        .andExpect(jsonPath("$._embedded.download-tasks[?(@.scope == 'A')]", hasSize(38)));
    mvc.perform(get(TASK_URI + "?scope=C")
        .header(HEADER_STRING, TOKEN_PREFIX + token))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.download-tasks", hasSize(47)))
        .andExpect(jsonPath("$._embedded.download-tasks[?(@.scope == 'C')]", hasSize(47)));
  }

  @Test
  public void filterScopeOr() throws Exception {
    mvc.perform(get(TASK_URI + "?scope=A&scope=C")
        .header(HEADER_STRING, TOKEN_PREFIX + token))
        .andDo(print())
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
