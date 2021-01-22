package org.sitmun.plugin.core.domain.projections;

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

import java.io.FileWriter;

import static org.hamcrest.Matchers.hasSize;
import static org.sitmun.plugin.core.test.TestConstants.SITMUN_ADMIN_USERNAME;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class ProjectionsTest {

  private static final String APPLICATION_PROJECTION_VIEW =
    "/api/applications/{0}?projection=view";

  private static final String USER_CONFIGURATION_PROJECTION_VIEW_PROPERTY_VALUE =
    "/api/user-configurations?projection=view&{0}={1}";

  private static final String USER_POSITION_PROJECTION_VIEW =
    "/api/user-positions/{0}?projection=view";

  @Autowired
  private MockMvc mvc;

  @Test
  @WithMockUser(username = SITMUN_ADMIN_USERNAME)
  public void projectionHasTerritoryNames() throws Exception {
    mvc.perform(get(USER_POSITION_PROJECTION_VIEW, 2124))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.territoryName").value("-  Província de Barcelona -"));
  }

  @Test
  @WithMockUser(username = SITMUN_ADMIN_USERNAME)
  public void filterByProjectedProperty() throws Exception {
    mvc.perform(get(USER_CONFIGURATION_PROJECTION_VIEW_PROPERTY_VALUE, "territoryId", "41"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.user-configurations", hasSize(34)))
      .andExpect(
        jsonPath("$._embedded.user-configurations[?(@.territoryId == 41)]", hasSize(34)));

    mvc.perform(get(USER_CONFIGURATION_PROJECTION_VIEW_PROPERTY_VALUE, "userId", "1777"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.user-configurations", hasSize(9)))
      .andExpect(
        jsonPath("$._embedded.user-configurations[?(@.userId == 1777)]", hasSize(9)));

    mvc.perform(get(USER_CONFIGURATION_PROJECTION_VIEW_PROPERTY_VALUE, "roleId", "10"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.user-configurations", hasSize(7)))
      .andExpect(jsonPath("$._embedded.user-configurations[?(@.roleId == 10)]", hasSize(7)));
  }

  @Test
  @WithMockUser(username = SITMUN_ADMIN_USERNAME)
  public void ensureIdIsPresent() throws Exception {
    mvc.perform(get(USER_CONFIGURATION_PROJECTION_VIEW_PROPERTY_VALUE, "territoryId", "41"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.user-configurations", hasSize(34)))
      .andExpect(
        jsonPath("$._embedded.user-configurations[*].id", hasSize(34)));
  }


  @Test
  @WithMockUser(username = SITMUN_ADMIN_USERNAME)
  public void checkCartographyGroupNameExistsInProjection() throws Exception {
    mvc.perform(get("/api/backgrounds?projection=view"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.backgrounds[?(@.cartographyGroupName)]", hasSize(6)));
  }

  @Test
  @WithMockUser(username = SITMUN_ADMIN_USERNAME)
  public void checkCartographyGroupIdExistsInProjection() throws Exception {
    mvc.perform(get("/api/backgrounds?projection=view"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.backgrounds[*].cartographyGroupId", hasSize(6)));
  }

  @Test
  @WithMockUser(username = SITMUN_ADMIN_USERNAME)
  public void projectionHasSituationMapId() throws Exception {
    mvc.perform(get(APPLICATION_PROJECTION_VIEW, 1))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.situationMapId").value(132));
  }

  @Test
  @WithMockUser(username = SITMUN_ADMIN_USERNAME)
  public void checkTaskGroupNameExistsInTasksProjection() throws Exception {
    mvc.perform(get("/api/tasks?projection=view"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.*.*", hasSize(1756)))
      .andExpect(jsonPath("$._embedded.*.[?(@.groupName)]", hasSize(1756)));
  }

  @Test
  @WithMockUser(username = SITMUN_ADMIN_USERNAME)
  public void checkCartographyGroupIdExistsInTasksProjection() throws Exception {
    mvc.perform(get("/api/tasks?projection=view"))
      .andDo(print(new FileWriter("out.txt")))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.*[*]", hasSize(1756)))
      .andExpect(jsonPath("$._embedded.*[?(@.groupId)]", hasSize(1756)));
  }

  @Test
  @WithMockUser(username = SITMUN_ADMIN_USERNAME)
  public void checkTerritoryIdExistsInAvailabilityProjection() throws Exception {
    mvc.perform(get("/api/cartographies/85/availabilities?projection=view"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.cartography-availabilities", hasSize(32)))
      .andExpect(jsonPath("$._embedded.cartography-availabilities[*].territoryCode", hasSize(32)))
      .andExpect(jsonPath("$._embedded.cartography-availabilities[*].territoryName", hasSize(32)));
  }

  @Test
  @WithMockUser(username = SITMUN_ADMIN_USERNAME)
  public void checkServiceNamesInCartographiesProjection() throws Exception {
    mvc.perform(get("/api/cartographies?projection=view"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.cartographies", hasSize(730)))
      .andExpect(jsonPath("$._embedded.cartographies[?(@.serviceName)]", hasSize(730)));
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
