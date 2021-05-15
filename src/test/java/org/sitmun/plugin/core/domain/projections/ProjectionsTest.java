package org.sitmun.plugin.core.domain.projections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sitmun.plugin.core.test.URIConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.sitmun.plugin.core.test.TestConstants.SITMUN_ADMIN_USERNAME;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc

public class ProjectionsTest {

  @Autowired
  private MockMvc mvc;

  @Test
  public void taskAvailabilityProjectionView() throws Exception {
    mvc.perform(get(URIConstants.TASK_AVAILABILITY_PROJECTION_VIEW, 1)
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
    ).andExpect(status().isOk())
      .andExpect(jsonPath("$.territoryId").value(7))
      .andExpect(jsonPath("$.territoryName").value("Teià"))
      .andExpect(jsonPath("$.taskId").value(31963))
      .andExpect(jsonPath("$.taskGroupName").value("NOMENCLÀTOR"));
  }

  @Test
  public void cartographyProjectionView() throws Exception {
    mvc.perform(get(URIConstants.CARTOGRAPHY_URI_PROJECTION, 87)
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
    )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.serviceId").value(43))
      .andExpect(jsonPath("$.serviceName").value("DIBA PRIVAT MUNI_DB_BASE"))
      .andExpect(jsonPath("$.spatialSelectionServiceId").value(47))
      .andExpect(jsonPath("$.spatialSelectionServiceName").value("DIBA WFS Geoserver"));
  }

  @Test
  public void cartographyAvailabiltiesProjectionView() throws Exception {
    mvc.perform(get(URIConstants.CARTOGRAPHY_AVAILABILTIY_PROJECTION_VIEW, 9999)
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
    ).andExpect(jsonPath("$.cartographyId").value(1208))
      .andExpect(jsonPath("$.cartographyName").value("NGE50 - Noms geogràfics (edificis) (ICGC)"))
      .andExpect(jsonPath("$.cartographyLayers").value(contains("NGE50_111P_EDI")))
      .andExpect(jsonPath("$.territoryId").value(35))
      .andExpect(jsonPath("$.territoryName").value("Aguilar de Segarra"))
      .andExpect(jsonPath("$.territoryCode").value("08002"));
  }

  @Test
  public void territoryProjectionView() throws Exception {
    mvc.perform(get(URIConstants.TERRITORY_PROJECTION_VIEW, 322)
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME)))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.groupTypeId").value(1))
      .andExpect(jsonPath("$.groupTypeName").value("Consell Comarcal"));
  }

  @Test
  public void applicationBackgroundProjectionView() throws Exception {
    mvc.perform(get(URIConstants.APPLICATION_BACKGROUND_PROJECTION_VIEW, 1)
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
    )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.backgroundName").value("Imatge Nomenclàtor"))
      .andExpect(jsonPath("$.backgroundDescription").value("NOMENCLÀTOR - Ortofoto ICC"));
  }

  @Test
  public void userPositionProjectionView() throws Exception {
    mvc.perform(get(URIConstants.USER_POSITION_PROJECTION_VIEW, 2124)
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME)))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.territoryName").value("-  Província de Barcelona -"))
      .andExpect(jsonPath("$.userId").value(6));
  }

  @Test
  public void userConfigurationProjectionView() throws Exception {
    mvc.perform(get(URIConstants.USER_CONFIGURATION_PROJECTION_VIEW_PROPERTY_VALUE, "territoryId", "41")
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME)))
      .andExpect(status().isOk())
      .andExpect(
        jsonPath("$._embedded.user-configurations[?(@.territoryId == 41)]", hasSize(34)));

    mvc.perform(get(URIConstants.USER_CONFIGURATION_PROJECTION_VIEW_PROPERTY_VALUE, "userId", "1777")
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME)))
      .andExpect(status().isOk())
      .andExpect(
        jsonPath("$._embedded.user-configurations[?(@.userId == 1777)]", hasSize(2)));

    mvc.perform(get(URIConstants.USER_CONFIGURATION_PROJECTION_VIEW_PROPERTY_VALUE, "roleId", "10")
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME)))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.user-configurations[?(@.roleId == 10)]", hasSize(7)));

    mvc.perform(get(URIConstants.USER_CONFIGURATION_PROJECTION_VIEW, "0")
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME)))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.appliesToChildrenTerritories").value(false));
  }

  @Test
  public void backgroundProjectionView() throws Exception {
    mvc.perform(get("/api/backgrounds?projection=view")
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME)))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.backgrounds[*].cartographyGroupId", hasSize(6)))
      .andExpect(jsonPath("$._embedded.backgrounds[?(@.cartographyGroupName)]", hasSize(6)));
  }

  @Test
  public void applicationProjectionView() throws Exception {
    mvc.perform(get(URIConstants.APPLICATION_PROJECTION_VIEW, 1)
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
    )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.situationMapId").value(132));
  }

  @Test
  public void tasksProjectionView() throws Exception {
    mvc.perform(get(URIConstants.TASK_PROJECTION_VIEW, 2)
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME)))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.groupName").value("SITXELL"))
      .andExpect(jsonPath("$.groupId").value(28))
      .andExpect(jsonPath("$.uiId").value(13));
  }


  @Test
  public void treeNodesProjectionView() throws Exception {
    mvc.perform(get("/api/tree-nodes/208?projection=view")
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME)))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.isFolder").value(true))
      .andExpect(jsonPath("$.cartographyName").isEmpty())
      .andExpect(jsonPath("$.cartographyId").isEmpty());

    mvc.perform(get("/api/tree-nodes/2546?projection=view")
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME)))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.isFolder").value(false))
      .andExpect(jsonPath("$.cartographyName").value("BUE1M - Planimetria (punts)"))
      .andExpect(jsonPath("$.cartographyId").value(178));
  }
}
