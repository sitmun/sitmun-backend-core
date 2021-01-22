package org.sitmun.plugin.core.repository.rest;

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

import static org.hamcrest.Matchers.hasSize;
import static org.sitmun.plugin.core.test.TestConstants.SITMUN_ADMIN_USERNAME;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class CartographyPermissionsRepositoryDataRestTest {

  private static final String CARTOGRAPHY_PERMISSIONS_URI =
    "http://localhost/api/cartography-groups";

  private static final String CARTOGRAPHY_PERMISSIONS_URI_FILTER =
    CARTOGRAPHY_PERMISSIONS_URI + "?type={0}";
  private static final String CARTOGRAPHY_PERMISSIONS_URI_OR_FILTER =
    CARTOGRAPHY_PERMISSIONS_URI + "?type={0}&type={1}";

  private static final String CARTOGRAPHY_PERMISSION_ROLES_URI =
    CARTOGRAPHY_PERMISSIONS_URI + "/{0}/roles";

  @Autowired
  private MockMvc mvc;

  @Test
  @WithMockUser(username = SITMUN_ADMIN_USERNAME)
  public void filterType() throws Exception {
    mvc.perform(get(CARTOGRAPHY_PERMISSIONS_URI_FILTER, "M"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.*.*", hasSize(1)))
        .andExpect(jsonPath("$._embedded.situation-maps[?(@.type == 'M')]", hasSize(1)));
    mvc.perform(get(CARTOGRAPHY_PERMISSIONS_URI_FILTER, "F"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.*.*", hasSize(6)))
        .andExpect(jsonPath("$._embedded.background-maps[?(@.type == 'F')]", hasSize(6)));
    mvc.perform(get(CARTOGRAPHY_PERMISSIONS_URI_FILTER, "C"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.*.*", hasSize(112)))
        .andExpect(jsonPath("$._embedded.cartography-groups[?(@.type == 'C')]", hasSize(112)));
  }

  @Test
  @WithMockUser(username = SITMUN_ADMIN_USERNAME)
  public void filterOrType() throws Exception {
    mvc.perform(get(CARTOGRAPHY_PERMISSIONS_URI_OR_FILTER, "M", "C"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.*.*", hasSize(113)))
      .andExpect(jsonPath("$._embedded.situation-maps[?(@.type == 'M')]", hasSize(1)))
      .andExpect(jsonPath("$._embedded.cartography-groups[?(@.type == 'C')]", hasSize(112)));
  }

  @Test
  @WithMockUser(username = SITMUN_ADMIN_USERNAME)
  public void rolesOfAPermissions() throws Exception {
    mvc.perform(get(CARTOGRAPHY_PERMISSION_ROLES_URI, 6))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.*.*", hasSize(9)));
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
