package org.sitmun.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sitmun.test.Fixtures;
import org.sitmun.test.URIConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc

@DisplayName("Projections test")
class ProjectionsTest {

  @Autowired
  private MockMvc mvc;

  @Test
  @DisplayName("TaskAvailability view")
  void taskAvailabilityProjectionView() throws Exception {
    mvc.perform(get(URIConstants.TASK_AVAILABILITY_PROJECTION_VIEW, 1)
        .with(user(Fixtures.admin()))
      ).andExpect(status().isOk())
      .andExpect(jsonPath("$.territoryId").value(1))
      .andExpect(jsonPath("$.territoryName").value("Provincia"))
      .andExpect(jsonPath("$.taskId").value(1))
      .andExpect(jsonPath("$.taskGroupName").value("Basic"));
  }

  @Test
  @DisplayName("Cartography view")
  void cartographyProjectionView() throws Exception {
    mvc.perform(get(URIConstants.CARTOGRAPHY_URI_PROJECTION, 87)
        .with(user(Fixtures.admin()))
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.serviceId").value(43))
      .andExpect(jsonPath("$.serviceName").value("DIBA PRIVAT MUNI_DB_BASE"))
      .andExpect(jsonPath("$.spatialSelectionServiceId").value(47))
      .andExpect(jsonPath("$.spatialSelectionServiceName").value("DIBA WFS Geoserver"))
      .andExpect(jsonPath("$.useAllStyles").value(false));
  }

  @Test
  @DisplayName("Cartography view - styles")
  void cartographyProjectionStylesView() throws Exception {
    mvc.perform(get(URIConstants.CARTOGRAPHY_URI_PROJECTION, 0)
        .with(user(Fixtures.admin()))
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.stylesNames", hasSize(3)))
      .andExpect(jsonPath("$.stylesNames", containsInAnyOrder("Style A", "Style B", "Style C")));
  }

  @Test
  @DisplayName("CartographyAvailability view")
  void cartographyAvailabiltiesProjectionView() throws Exception {
    mvc.perform(get(URIConstants.CARTOGRAPHY_AVAILABILTIY_PROJECTION_VIEW, 9999)
        .with(user(Fixtures.admin()))
      ).andExpect(jsonPath("$.cartographyId").value(1208))
      .andExpect(jsonPath("$.cartographyName").value("NGE50 - Noms geogràfics (edificis) (ICGC)"))
      .andExpect(jsonPath("$.cartographyLayers").value(contains("NGE50_111P_EDI")))
      .andExpect(jsonPath("$.territoryId").value(35))
      .andExpect(jsonPath("$.territoryName").value("Aguilar de Segarra"))
      .andExpect(jsonPath("$.territoryCode").value("08002"));
  }

  @Test
  @DisplayName("Territory view")
  void territoryProjectionView() throws Exception {
    mvc.perform(get(URIConstants.TERRITORY_PROJECTION_VIEW, 322)
        .with(user(Fixtures.admin())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.extent.maxX").value(467600.0))
      .andExpect(jsonPath("$.typeId").value(2))
      .andExpect(jsonPath("$.typeName").value("Consell Comarcal"))
      .andExpect(jsonPath("$.typeTopType").value(false))
      .andExpect(jsonPath("$.typeBottomType").value(false))
      .andExpect(jsonPath("$.center.x").value(442875.0))
      .andExpect(jsonPath("$.defaultZoomLevel", is(nullValue())));
  }

  @Test
  @DisplayName("ApplicationBackground view")
  void applicationBackgroundProjectionView() throws Exception {
    mvc.perform(get(URIConstants.APPLICATION_BACKGROUND_PROJECTION_VIEW, 1)
        .with(user(Fixtures.admin()))
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.applicationName").value("TEST"))
      .andExpect(jsonPath("$.backgroundName").value("Imatge Nomenclàtor"))
      .andExpect(jsonPath("$.backgroundDescription").value("NOMENCLÀTOR - Ortofoto ICC"));
  }

  @Test
  @DisplayName("UserPosition view")
  void userPositionProjectionView() throws Exception {
    mvc.perform(get(URIConstants.USER_POSITION_PROJECTION_VIEW, 2124)
        .with(user(Fixtures.admin())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.territoryName").value("-  Província de Barcelona -"))
      .andExpect(jsonPath("$.userId").value(6));
  }

  @Test
  @DisplayName("UserConfiguration view")
  void userConfigurationProjectionView() throws Exception {
    mvc.perform(get(URIConstants.USER_CONFIGURATION_PROJECTION_VIEW_PROPERTY_VALUE, "territoryId", "41")
        .with(user(Fixtures.admin())))
      .andExpect(status().isOk())
      .andExpect(
        jsonPath("$._embedded.user-configurations[?(@.territoryId == 41)]", hasSize(34)));

    mvc.perform(get(URIConstants.USER_CONFIGURATION_PROJECTION_VIEW_PROPERTY_VALUE, "userId", "1777")
        .with(user(Fixtures.admin())))
      .andExpect(status().isOk())
      .andExpect(
        jsonPath("$._embedded.user-configurations[?(@.userId == 1777)]", hasSize(2)));

    mvc.perform(get(URIConstants.USER_CONFIGURATION_PROJECTION_VIEW_PROPERTY_VALUE, "roleId", "10")
        .with(user(Fixtures.admin())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.user-configurations[?(@.roleId == 10)]", hasSize(7)));

    mvc.perform(get(URIConstants.USER_CONFIGURATION_PROJECTION_VIEW, "0")
        .with(user(Fixtures.admin())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.appliesToChildrenTerritories").value(false));
  }

  @Test
  @DisplayName("Backgrounds view")
  void backgroundProjectionView() throws Exception {
    mvc.perform(get(URIConstants.BACKGROUNDS_URI_PROJECTION_VIEW)
        .with(user(Fixtures.admin())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.backgrounds[*].cartographyGroupId", hasSize(6)))
      .andExpect(jsonPath("$._embedded.backgrounds[?(@.cartographyGroupName)]", hasSize(6)));
  }

  @Test
  @DisplayName("Application view")
  void applicationProjectionView() throws Exception {
    mvc.perform(get(URIConstants.APPLICATION_PROJECTION_VIEW, 1)
        .with(user(Fixtures.admin()))
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.situationMapId").value(132));
  }

  @Test
  @DisplayName("Tasks view")
  void tasksProjectionView() throws Exception {
    mvc.perform(get(URIConstants.TASK_PROJECTION_VIEW, 2)
        .with(user(Fixtures.admin())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.groupName").value("SITXELL"))
      .andExpect(jsonPath("$.groupId").value(28))
      .andExpect(jsonPath("$.uiId").value(13))
      .andExpect(jsonPath("$.typeId").value(2))
      .andExpect(jsonPath("$.typeName").value("descarga"));

    mvc.perform(get(URIConstants.TASK_PROJECTION_VIEW, 3301)
        .with(user(Fixtures.admin())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.cartographyName").value("CRE5M - Illes"))
      .andExpect(jsonPath("$.cartographyId").value(87));
  }

  @Test
  @DisplayName("Tree Nodes view")
  void treeNodesProjectionView() throws Exception {
    mvc.perform(get(URIConstants.TREE_NODE_URI_PROJECTION, 288)
        .with(user(Fixtures.admin())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.isFolder").value(true))
      .andExpect(jsonPath("$.cartographyName").isEmpty())
      .andExpect(jsonPath("$.cartographyId").isEmpty());

    mvc.perform(get(URIConstants.TREE_NODE_URI_PROJECTION, 2546)
        .with(user(Fixtures.admin())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.isFolder").value(false))
      .andExpect(jsonPath("$.cartographyName").value("BUE1M - Planimetria (punts)"))
      .andExpect(jsonPath("$.cartographyId").value(178));
  }

  @Test
  @DisplayName("Translations view")
  void translationProjectionView() throws Exception {
    mvc.perform(get(URIConstants.TRANSLATION_URI_PROJECTION, 301001)
        .with(user(Fixtures.admin())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.id").value(301001))
      .andExpect(jsonPath("$.element").value(1))
      .andExpect(jsonPath("$.column").value("name"))
      .andExpect(jsonPath("$.languageName").value("Catalan"))
      .andExpect(jsonPath("$.languageShortname").value("ca"))
      .andExpect(jsonPath("$.translation").value("Anglès"));
  }

  @Test
  @DisplayName("Permissions projection view through cartography")
  void permissionsProjectionView() throws Exception {
    mvc.perform(get(URIConstants.CARTOGRAPHY_URI_PERMISSION_URI_PROJECTION, 90)
        .with(user(Fixtures.admin())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.cartography-groups").exists())
      .andExpect(jsonPath("$._embedded.cartography-groups", hasSize(1)))
      .andExpect(jsonPath("$._embedded.cartography-groups[0].name").value("Cartography Permisions Group 6"))
      .andExpect(jsonPath("$._embedded.cartography-groups[0].roleNames", hasSize(9)))
      .andExpect(jsonPath("$._embedded.cartography-groups[0].roleNames", containsInAnyOrder("Name of Role 10", "Name of Role 14", "Name of Role 17", "Name of Role 18", "Name of Role 19", "Name of Role 21", "Name of Role 23", "Name of Role 31", "Name of Role 147")));
  }

  @Test
  @DisplayName("Permissions projection view through background")
  void permissionsProjectionView2() throws Exception {
    mvc.perform(get(URIConstants.BACKGROUND_URI_CARTOGRAPHY_GROUP_PROJECTION, 2)
        .with(user(Fixtures.admin())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.name").value("Background Map 130"))
      .andExpect(jsonPath("$.roleNames", hasSize(0)));
  }

  @Test
  @DisplayName("Permissions projection view type")
  void permissionsProjectionType() throws Exception {
    mvc.perform(get(URIConstants.BACKGROUND_URI_CARTOGRAPHY_GROUP_PROJECTION, 2)
        .with(user(Fixtures.admin())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.type").value("F"));
  }
}
