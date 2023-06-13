package org.sitmun.authorization.controller;

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
class ClientConfigurationProfileControllerTest {

  @Autowired
  private MockMvc mvc;

  @Test
  void applicationDetails() throws Exception {
    mvc.perform(get(URIConstants.CONFIG_CLIENT_PROFILE_URI, 1, 1))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.application.theme", is("sitmun-base")))
      .andExpect(jsonPath("$.application.srs", is("EPSG:25831")))
      .andExpect(jsonPath("$.application.initialExtent", hasItems(363487.0, 4561229.0, 481617.0, 4686464.0)));
  }

  @Test
  void layers() throws Exception {
    mvc.perform(get(URIConstants.CONFIG_CLIENT_PROFILE_URI, 1, 1))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.layers[?(@.id=='layer/1')].title", hasItem("WMTS Bases - ICGC- Topo")))
      .andExpect(jsonPath("$.layers[?(@.id=='layer/1')].layers[0]", hasItem("topo")))
      .andExpect(jsonPath("$.layers[?(@.id=='layer/1')].service.url", hasItem("https://geoserveis.icgc.cat/icc_mapesmultibase/utm/wmts/service")))
      .andExpect(jsonPath("$.layers[?(@.id=='layer/1')].service.type", hasItem("WMTS")))
      .andExpect(jsonPath("$.layers[?(@.id=='layer/1')].service.parameters.format", hasItem("image/jpeg")))
      .andExpect(jsonPath("$.layers[?(@.id=='layer/1')].service.parameters.matrixSet", hasItem("UTM25831")))
      .andExpect(jsonPath("$.layers[?(@.id=='layer/4')].title", hasItem("Límites administrativos")));
  }

  @Test
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
  void backgrounds() throws Exception {
    mvc.perform(get(URIConstants.CONFIG_CLIENT_PROFILE_URI, 1, 1))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.backgrounds").isArray())
      .andExpect(jsonPath("$.backgrounds[?(@.id=='group/2')].title", hasItem("Background Map")))
      .andExpect(jsonPath("$.groups[?(@.id=='group/2')].title", hasItem("Background Map")));
  }

  @Test
  void situationMap() throws Exception {
    mvc.perform(get(URIConstants.CONFIG_CLIENT_PROFILE_URI, 1, 1))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.application.situation-map", is("group/3")));
  }

  @Test
  void tasks() throws Exception {
    mvc.perform(get(URIConstants.CONFIG_CLIENT_PROFILE_URI, 1, 1))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.tasks[?(@.id=='task/1')].ui-control", hasItem("sitna.attribution")));
  }

  @Test
  void taskParameters() throws Exception {
    mvc.perform(get(URIConstants.CONFIG_CLIENT_PROFILE_URI, 1, 1))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.tasks[?(@.id=='task/20')].parameters.div", hasItem("print")))
      .andExpect(jsonPath("$.tasks[?(@.id=='task/20')].parameters.legend.visible", hasItem(true)));
  }
}
