package org.sitmun.authorization.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.test.URIConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("API Authorization and Configuration - Profile endpoint")
class ClientConfigurationProfileControllerTest {

  @Autowired
  private MockMvc mvc;

  @Test
  @DisplayName("Get application details")
  void applicationDetails() throws Exception {
    mvc.perform(get(URIConstants.CONFIG_CLIENT_PROFILE_URI, 1, 1))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.application.theme", is("sitmun-base")))
      .andExpect(jsonPath("$.application.srs", is("EPSG:25831")))
      .andExpect(jsonPath("$.application.initialExtent", hasItems(363487.0, 4561229.0, 481617.0, 4686464.0)));
  }

  @Test
  @DisplayName("Get layers details")
  void layers() throws Exception {
    mvc.perform(get(URIConstants.CONFIG_CLIENT_PROFILE_URI, 1, 1))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.layers[?(@.id=='layer/1')].title", hasItem("WMTS Bases - ICGC- Topo")))
      .andExpect(jsonPath("$.layers[?(@.id=='layer/1')].layers[0]", hasItem("topo")))
      .andExpect(jsonPath("$.layers[?(@.id=='layer/1')].service", hasItem("service/1")))
      .andExpect(jsonPath("$.layers[?(@.id=='layer/4')].title", hasItem("Límites administrativos")));
  }

  @Test
  @DisplayName("Get services details")
  void services() throws Exception {
    mvc.perform(get(URIConstants.CONFIG_CLIENT_PROFILE_URI, 1, 1))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.services[?(@.id=='service/1')].url", hasItem("https://geoserveis.icgc.cat/icc_mapesmultibase/utm/wmts/service")))
      .andExpect(jsonPath("$.services[?(@.id=='service/1')].type", hasItem("WMTS")))
      .andExpect(jsonPath("$.services[?(@.id=='service/1')].parameters.format", hasItem("image/jpeg")))
      .andExpect(jsonPath("$.services[?(@.id=='service/1')].parameters.matrixSet", hasItem("UTM25831")));
  }

  @Test
  @DisplayName("Get groups details")
  void groups() throws Exception {
    mvc.perform(get(URIConstants.CONFIG_CLIENT_PROFILE_URI, 1, 1))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.groups[?(@.id=='group/2')].title", hasItem("Background Map")))
      .andExpect(jsonPath("$.groups[?(@.id=='group/2')].layers.*", hasItems("layer/1", "layer/2")))
      .andExpect(jsonPath("$.groups[?(@.id=='group/1')].title", hasItem("Cartography Group")))
      .andExpect(jsonPath("$.groups[?(@.id=='group/1')].layers[0]", hasItem("layer/3")))
      .andExpect(jsonPath("$.groups[?(@.id=='group/3')].title", hasItem("Situation Map")))
      .andExpect(jsonPath("$.groups[?(@.id=='group/3')].layers[0]", hasItem("layer/4")));
  }

  @Test
  @DisplayName("Get backgrounds details")
  void backgrounds() throws Exception {
    mvc.perform(get(URIConstants.CONFIG_CLIENT_PROFILE_URI, 1, 1))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.backgrounds").isArray())
      .andExpect(jsonPath("$.backgrounds[?(@.id=='group/2')].title", hasItem("Background Map")))
      .andExpect(jsonPath("$.groups[?(@.id=='group/2')].title", hasItem("Background Map")));
  }

  @Test
  @DisplayName("Get situation map details")
  void situationMap() throws Exception {
    mvc.perform(get(URIConstants.CONFIG_CLIENT_PROFILE_URI, 1, 1))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.application.situation-map", is("group/3")));
  }

  @Test
  @DisplayName("Get tasks details")
  void tasks() throws Exception {
    mvc.perform(get(URIConstants.CONFIG_CLIENT_PROFILE_URI, 1, 1))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.tasks[?(@.id=='task/1')].ui-control", hasItem("sitna.attribution")));
  }

  @Test
  @DisplayName("Get task parameters details")
  void taskParameters() throws Exception {
    mvc.perform(get(URIConstants.CONFIG_CLIENT_PROFILE_URI, 1, 1))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.tasks[?(@.id=='task/20')].parameters.div", hasItem("print")))
      .andExpect(jsonPath("$.tasks[?(@.id=='task/20')].parameters.legend.visible", hasItem(true)));
  }

  @Test
  @DisplayName("Get tree details")
  void tree() throws Exception {
    mvc.perform(get(URIConstants.CONFIG_CLIENT_PROFILE_URI, 1, 1))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.trees[?(@.id=='tree/1')].title", hasItem("Provincial")))
      .andExpect(jsonPath("$.trees[?(@.id=='tree/1')].rootNode", hasItem("node/1")))
      .andExpect(jsonPath("$.trees[?(@.id=='tree/1')].nodes['node/9'].resource", hasItem("layer/9")));
  }
}
