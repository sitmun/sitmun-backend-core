package org.sitmun.domain;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.test.Fixtures;
import org.sitmun.test.URIConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Data Projections Integration Test")
class ProjectionsTest {

  @Autowired private MockMvc mvc;

  @Test
  @DisplayName("GET: Retrieve task availability projection with territory and task details")
  void taskAvailabilityProjection() throws Exception {
    mvc.perform(get(URIConstants.TASK_AVAILABILITY_PROJECTION_VIEW, 1).with(user(Fixtures.admin())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.territoryId").value(1))
        .andExpect(jsonPath("$.territoryName").value("Provincia A"))
        .andExpect(jsonPath("$.taskId").value(1))
        .andExpect(jsonPath("$.taskGroupName").value("Basic"));
  }

  @Test
  @DisplayName("GET: Retrieve cartography projection with service information")
  void cartographyProjectionView() throws Exception {
    mvc.perform(get(URIConstants.CARTOGRAPHY_URI_PROJECTION, 1).with(user(Fixtures.admin())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.serviceId").value(1))
        .andExpect(jsonPath("$.serviceName").value("ICC Mapesmultibase"))
        .andExpect(jsonPath("$.serviceId").value(1))
        .andExpect(jsonPath("$.useAllStyles").value(false));
  }

  @Test
  @DisplayName("GET: Retrieve cartography projection with style names")
  void cartographyProjectionStylesView() throws Exception {
    mvc.perform(get(URIConstants.CARTOGRAPHY_URI_PROJECTION, 1).with(user(Fixtures.admin())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.stylesNames", hasSize(3)))
        .andExpect(jsonPath("$.stylesNames", containsInAnyOrder("Style A", "Style B", "Style C")));
  }

  @Test
  @Disabled("Requires additional test data")
  @DisplayName("GET: Retrieve cartography availability projection with detailed information")
  void cartographyAvailabiltiesProjectionView() throws Exception {
    mvc.perform(
            get(URIConstants.CARTOGRAPHY_AVAILABILITY_PROJECTION_VIEW, 1)
                .with(user(Fixtures.admin())))
        .andExpect(jsonPath("$.cartographyId").value(1208))
        .andExpect(jsonPath("$.cartographyName").value("NGE50 - Noms geogràfics (edificis) (ICGC)"))
        .andExpect(jsonPath("$.cartographyLayers").value(contains("NGE50_111P_EDI")))
        .andExpect(jsonPath("$.territoryId").value(35))
        .andExpect(jsonPath("$.territoryName").value("Aguilar de Segarra"))
        .andExpect(jsonPath("$.territoryCode").value("08002"));
  }

  @Test
  @DisplayName("GET: Retrieve territory projection with extent and type information")
  void territoryProjectionView() throws Exception {
    mvc.perform(get(URIConstants.TERRITORY_PROJECTION_VIEW, 1).with(user(Fixtures.admin())))
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
  void applicationBackgroundProjectionView() throws Exception {
    mvc.perform(
            get(URIConstants.APPLICATION_BACKGROUND_PROJECTION_VIEW, 1)
                .with(user(Fixtures.admin())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.applicationName").value("SITMUN - Provincial"))
        .andExpect(jsonPath("$.backgroundName").value("Background Map"))
        .andExpect(jsonPath("$.backgroundDescription").value("Background Map"));
  }

  @Test
  @DisplayName("GET: Retrieve user position projection with territory and user information")
  void userPositionProjectionView() throws Exception {
    mvc.perform(get(URIConstants.USER_POSITION_PROJECTION_VIEW, 6).with(user(Fixtures.admin())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.territoryName").value("Provincia A"))
        .andExpect(jsonPath("$.userId").value(1));
  }

  @Test
  @Disabled("Requires additional test data")
  @DisplayName("GET: Retrieve user configuration projection with filtered results")
  void userConfigurationProjectionView() throws Exception {
    mvc.perform(
            get(URIConstants.USER_CONFIGURATION_PROJECTION_VIEW_PROPERTY_VALUE, "territoryId", "41")
                .with(user(Fixtures.admin())))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$._embedded.user-configurations[?(@.territoryId == 41)]", hasSize(34)));

    mvc.perform(
            get(URIConstants.USER_CONFIGURATION_PROJECTION_VIEW_PROPERTY_VALUE, "userId", "1777")
                .with(user(Fixtures.admin())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.user-configurations[?(@.userId == 1777)]", hasSize(2)));

    mvc.perform(
            get(URIConstants.USER_CONFIGURATION_PROJECTION_VIEW_PROPERTY_VALUE, "roleId", "10")
                .with(user(Fixtures.admin())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.user-configurations[?(@.roleId == 10)]", hasSize(7)));

    mvc.perform(
            get(URIConstants.USER_CONFIGURATION_PROJECTION_VIEW, "0").with(user(Fixtures.admin())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.appliesToChildrenTerritories").value(false));
  }

  @Test
  @DisplayName("GET: Retrieve backgrounds projection with cartography group information")
  void backgroundProjectionView() throws Exception {
    mvc.perform(get(URIConstants.BACKGROUNDS_URI_PROJECTION_VIEW).with(user(Fixtures.admin())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.backgrounds[*].cartographyGroupId", hasSize(1)))
        .andExpect(jsonPath("$._embedded.backgrounds[?(@.cartographyGroupName)]", hasSize(1)));
  }

  @Test
  @DisplayName("GET: Retrieve application projection with situation map information")
  void applicationProjectionView() throws Exception {
    mvc.perform(get(URIConstants.APPLICATION_PROJECTION_VIEW, 1).with(user(Fixtures.admin())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.situationMapId").value(3));
  }

  @Test
  @DisplayName("GET: Retrieve tasks projection with group and type information")
  void tasksProjectionView() throws Exception {
    mvc.perform(get(URIConstants.TASK_PROJECTION_VIEW, 2).with(user(Fixtures.admin())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.groupName").value("Basic"))
        .andExpect(jsonPath("$.groupId").value(1))
        .andExpect(jsonPath("$.uiId").value(2))
        .andExpect(jsonPath("$.typeId").value(1))
        .andExpect(jsonPath("$.typeName").value("básica"));

    // TODO Add test data
    //  mvc.perform(get(URIConstants.TASK_PROJECTION_VIEW, 3301)
    //    .with(user(Fixtures.admin())))
    //   .andExpect(status().isOk())
    //   .andExpect(jsonPath("$.cartographyName").value("CRE5M - Illes"))
    //   .andExpect(jsonPath("$.cartographyId").value(87));
  }

  @Test
  @DisplayName("GET: Retrieve tree nodes projection with folder and leaf information")
  void treeNodesProjectionView() throws Exception {
    mvc.perform(get(URIConstants.TREE_NODE_URI_PROJECTION, 1).with(user(Fixtures.admin())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.isFolder").value(true))
        .andExpect(jsonPath("$.cartographyName").isEmpty())
        .andExpect(jsonPath("$.cartographyId").isEmpty());

    mvc.perform(get(URIConstants.TREE_NODE_URI_PROJECTION, 3).with(user(Fixtures.admin())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.isFolder").value(false))
        .andExpect(jsonPath("$.cartographyName").value("Toponimia 1:25.000 (ICGC)"))
        .andExpect(jsonPath("$.cartographyId").value(6));
  }

  @Test
  @DisplayName("GET: Retrieve translations projection with language information")
  void translationProjectionView() throws Exception {
    mvc.perform(get(URIConstants.TRANSLATION_URI_PROJECTION, 301001).with(user(Fixtures.admin())))
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
  void permissionsProjectionView() throws Exception {
    mvc.perform(
            get(URIConstants.CARTOGRAPHY_URI_PERMISSION_URI_PROJECTION, 1)
                .with(user(Fixtures.admin())))
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
  void permissionsProjectionView2() throws Exception {
    mvc.perform(
            get(URIConstants.BACKGROUND_URI_CARTOGRAPHY_GROUP_PROJECTION, 1)
                .with(user(Fixtures.admin())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Background Map"))
        .andExpect(jsonPath("$.roleNames", hasSize(1)));
  }

  @Test
  @DisplayName("GET: Retrieve permissions projection type information")
  void permissionsProjectionType() throws Exception {
    mvc.perform(
            get(URIConstants.BACKGROUND_URI_CARTOGRAPHY_GROUP_PROJECTION, 1)
                .with(user(Fixtures.admin())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.type").value("F"));
  }
}
