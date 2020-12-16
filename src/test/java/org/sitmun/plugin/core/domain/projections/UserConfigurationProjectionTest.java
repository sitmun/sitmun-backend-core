package org.sitmun.plugin.core.domain.projections;

import static org.hamcrest.Matchers.hasSize;
import static org.sitmun.plugin.core.test.TestConstants.SITMUN_ADMIN_USERNAME;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.sitmun.plugin.core.config.RepositoryRestConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class UserConfigurationProjectionTest {

  @Autowired
  private MockMvc mvc;

  @Test
  @WithMockUser(username = SITMUN_ADMIN_USERNAME)
  public void filterByProjectedProperty() throws Exception {
    mvc.perform(get("/api/user-configurations?projection=view&territory.id=41"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.user-configurations", hasSize(34)))
        .andExpect(
            jsonPath("$._embedded.user-configurations[?(@.['territory.id'] == 41)]", hasSize(34)));

    mvc.perform(get("/api/user-configurations?projection=view&user.id=1777"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.user-configurations", hasSize(9)))
        .andExpect(
            jsonPath("$._embedded.user-configurations[?(@.['user.id'] == 1777)]", hasSize(9)));

    mvc.perform(get("/api/user-configurations?projection=view&role.id=10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.user-configurations", hasSize(7)))
        .andExpect(jsonPath("$._embedded.user-configurations[?(@.['role.id'] == 10)]", hasSize(7)));
  }

  @TestConfiguration
  static class ContextConfiguration {

    @Bean
    public Validator validator() {
      return new LocalValidatorFactoryBean();
    }

    @Bean
    public RepositoryRestConfigurer repositoryRestConfigurer() {
      return new RepositoryRestConfig(validator());
    }
  }

}
