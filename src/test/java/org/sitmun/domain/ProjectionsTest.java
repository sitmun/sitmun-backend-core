package org.sitmun.domain;

import static org.hamcrest.Matchers.*;
import static org.sitmun.test.URIConstants.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Data Projections Integration Test")
class ProjectionsTest {

  @Autowired private MockMvc mvc;

  @Test
  @DisplayName("GET: Retrieve task availability projection with territory and task details")
  @WithMockUser(roles = "ADMIN")
  void taskAvailabilityProjection() throws Exception {
    mvc.perform(get(TASK_AVAILABILITY_PROJECTION_VIEW, 1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.territoryId").value(1))
        .andExpect(jsonPath("$.territoryName").value("Provincia A"))
        .andExpect(jsonPath("$.taskId").value(1))
        .andExpect(jsonPath("$.taskGroupName").value("Basic"));
  }

  @Test
  @DisplayName("GET: Retrieve cartography projection with service information")
  @WithMockUser(roles = "ADMIN")
  void cartographyProjectionView() throws Exception {
    mvc.perform(get(CARTOGRAPHY_URI_PROJECTION, 1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.serviceId").value(1))
        .andExpect(jsonPath("$.serviceName").value("ICC Mapesmultibase"))
        .andExpect(jsonPath("$.serviceId").value(1))
        .andExpect(jsonPath("$.useAllStyles").value(false));
  }

  @Test
  @DisplayName("GET: Retrieve cartography projection with style names")
  @WithMockUser(roles = "ADMIN")
  void cartographyProjectionStylesView() throws Exception {
    mvc.perform(get(CARTOGRAPHY_URI_PROJECTION, 1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.stylesNames", hasSize(3)))
        .andExpect(jsonPath("$.stylesNames", containsInAnyOrder("Style A", "Style B", "Style C")));
  }

  @Test
  @Disabled("Requires additional test data")
  @DisplayName("GET: Retrieve cartography availability projection with detailed information")
  @WithMockUser(roles = "ADMIN")
  void cartographyAvailabiltiesProjectionView() throws Exception {
    mvc.perform(get(CARTOGRAPHY_AVAILABILITY_PROJECTION_VIEW, 1))
        .andExpect(jsonPath("$.cartographyId").value(1208))
        .andExpect(jsonPath("$.cartographyName").value("NGE50 - Noms geogràfics (edificis) (ICGC)"))
        .andExpect(jsonPath("$.cartographyLayers").value(contains("NGE50_111P_EDI")))
        .andExpect(jsonPath("$.territoryId").value(35))
        .andExpect(jsonPath("$.territoryName").value("Aguilar de Segarra"))
        .andExpect(jsonPath("$.territoryCode").value("08002"));
  }

  @Test
  @DisplayName("GET: Retrieve territory projection with extent and type information")
  @WithMockUser(roles = "ADMIN")
  void territoryProjectionView() throws Exception {
    mvc.perform(get(TERRITORY_PROJECTION_VIEW, 1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.extent.maxX").value(481617.0))
        .andExpect(jsonPath("$.typeId").value(8))
        .andExpect(jsonPath("$.typeName").value("Província"))
        .andExpect(jsonPath("$.typeTopType").value(true))
        .andExpect(jsonPath("$.typeBottomType").value(false))
        .andExpect(jsonPath("$.center.x").value(422552))
        .andExpect(jsonPath("$.defaultZoomLevel").value(8));
  }

  @Test
  @DisplayName("GET: Retrieve application background projection with background details")
  @WithMockUser(roles = "ADMIN")
  void applicationBackgroundProjectionView() throws Exception {
    mvc.perform(get(APPLICATION_BACKGROUND_PROJECTION_VIEW, 1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.applicationName").value("SITMUN - Provincial"))
        .andExpect(jsonPath("$.backgroundName").value("Background Map"))
        .andExpect(jsonPath("$.backgroundDescription").value("Background Map"));
  }

  @Test
  @DisplayName("GET: Retrieve user position projection with territory and user information")
  @WithMockUser(roles = "ADMIN")
  void userPositionProjectionView() throws Exception {
    mvc.perform(get(USER_POSITION_PROJECTION_VIEW, 6))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.territoryName").value("Provincia A"))
        .andExpect(jsonPath("$.userId").value(1));
  }

  @Test
  @Disabled("Requires additional test data")
  @DisplayName("GET: Retrieve user configuration projection with filtered results")
  @WithMockUser(roles = "ADMIN")
  void userConfigurationProjectionView() throws Exception {
    mvc.perform(get(USER_CONFIGURATION_PROJECTION_VIEW_PROPERTY_VALUE, "territoryId", "41"))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$._embedded.user-configurations[?(@.territoryId == 41)]", hasSize(34)));

    mvc.perform(get(USER_CONFIGURATION_PROJECTION_VIEW_PROPERTY_VALUE, "userId", "1777"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.user-configurations[?(@.userId == 1777)]", hasSize(2)));

    mvc.perform(get(USER_CONFIGURATION_PROJECTION_VIEW_PROPERTY_VALUE, "roleId", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.user-configurations[?(@.roleId == 10)]", hasSize(7)));

    mvc.perform(get(USER_CONFIGURATION_PROJECTION_VIEW, "0"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.appliesToChildrenTerritories").value(false));
  }

  @Test
  @DisplayName("GET: Retrieve backgrounds projection with cartography group information")
  @WithMockUser(roles = "ADMIN")
  void backgroundProjectionView() throws Exception {
    mvc.perform(get(BACKGROUNDS_URI_PROJECTION_VIEW))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.backgrounds[*].cartographyGroupId", hasSize(1)))
        .andExpect(jsonPath("$._embedded.backgrounds[?(@.cartographyGroupName)]", hasSize(1)));
  }

  @Test
  @DisplayName("GET: Retrieve application projection with situation map information")
  @WithMockUser(roles = "ADMIN")
  void applicationProjectionView() throws Exception {
    mvc.perform(get(APPLICATION_PROJECTION_VIEW, 1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.situationMapId").value(3));
  }

  @Test
  @DisplayName("GET: Retrieve tasks projection with group and type information")
  @WithMockUser(roles = "ADMIN")
  void tasksProjectionView() throws Exception {
    mvc.perform(get(TASK_PROJECTION_VIEW, 2))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.groupName").value("Basic"))
        .andExpect(jsonPath("$.groupId").value(1))
        .andExpect(jsonPath("$.uiId").value(2))
        .andExpect(jsonPath("$.typeId").value(1))
        .andExpect(jsonPath("$.typeName").value("básica"));
  }

  @Test
  @DisplayName("GET: Retrieve tree nodes projection with folder and leaf information")
  @WithMockUser(roles = "ADMIN")
  void treeNodesProjectionView() throws Exception {
    mvc.perform(get(TREE_NODE_URI_PROJECTION, 1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.isFolder").value(true))
        .andExpect(jsonPath("$.cartographyName").isEmpty())
        .andExpect(jsonPath("$.cartographyId").isEmpty());

    mvc.perform(get(TREE_NODE_URI_PROJECTION, 3))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.isFolder").value(false))
        .andExpect(jsonPath("$.cartographyName").value("Toponimia 1:25.000 (ICGC)"))
        .andExpect(jsonPath("$.cartographyId").value(6));
  }

  @Test
  @DisplayName("GET: Retrieve translations projection with language information")
  @WithMockUser(roles = "ADMIN")
  void translationProjectionView() throws Exception {
    mvc.perform(get(TRANSLATION_URI_PROJECTION, 301001))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(301001))
        .andExpect(jsonPath("$.element").value(1))
        .andExpect(jsonPath("$.column").value("Language.name"))
        .andExpect(jsonPath("$.languageName").value("Catalan"))
        .andExpect(jsonPath("$.languageShortname").value("ca"))
        .andExpect(jsonPath("$.translation").value("Anglès"));
  }

  @Test
  @DisplayName("GET: Retrieve permissions projection through cartography")
  @WithMockUser(roles = "ADMIN")
  void permissionsProjectionView() throws Exception {
    mvc.perform(get(CARTOGRAPHY_URI_PERMISSION_URI_PROJECTION, 1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.cartography-groups").exists())
        .andExpect(jsonPath("$._embedded.cartography-groups", hasSize(1)))
        .andExpect(jsonPath("$._embedded.cartography-groups[0].name").value("Background Map"))
        .andExpect(jsonPath("$._embedded.cartography-groups[0].roleNames", hasSize(1)))
        .andExpect(
            jsonPath("$._embedded.cartography-groups[0].roleNames", containsInAnyOrder("Role 1")));
  }

  @Test
  @DisplayName("GET: Retrieve permissions projection through background")
  @WithMockUser(roles = "ADMIN")
  void permissionsProjectionView2() throws Exception {
    mvc.perform(get(BACKGROUND_URI_CARTOGRAPHY_GROUP_PROJECTION, 1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Background Map"))
        .andExpect(jsonPath("$.roleNames", hasSize(1)));
  }

  @Test
  @DisplayName("GET: Retrieve permissions projection type information")
  @WithMockUser(roles = "ADMIN")
  void permissionsProjectionType() throws Exception {
    mvc.perform(get(BACKGROUND_URI_CARTOGRAPHY_GROUP_PROJECTION, 1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.type").value("F"));
  }

  @Test
  @DisplayName("GET: Retrieve user projection with warnings information")
  @WithMockUser(roles = "ADMIN")
  void userProjectionView() throws Exception {
    mvc.perform(get("/api/users/1?projection=view"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.username").exists())
        .andExpect(jsonPath("$.firstName").exists())
        .andExpect(jsonPath("$.lastName").value(Matchers.nullValue()))
        .andExpect(jsonPath("$.email").value(Matchers.nullValue()))
        .andExpect(jsonPath("$.administrator").exists())
        .andExpect(jsonPath("$.blocked").exists())
        .andExpect(jsonPath("$.passwordSet").exists())
        .andExpect(jsonPath("$.warnings").exists());
  }
}
