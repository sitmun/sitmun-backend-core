package org.sitmun.authorization.client.controller;

import static org.hamcrest.Matchers.*;
import static org.sitmun.domain.DomainConstants.Tasks.SCOPE_API;
import static org.sitmun.domain.DomainConstants.Tasks.SCOPE_SQL;
import static org.sitmun.domain.DomainConstants.Tasks.SCOPE_URL;
import static org.sitmun.test.URIConstants.CONFIG_CLIENT_PROFILE_URI;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.SitmunConstants;
import org.sitmun.test.URIConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("API Authorization and Configuration - Profile endpoint")
class ClientConfigurationProfileControllerTest {

  @Autowired private MockMvc mvc;

  @Value("${sitmun.proxy-middleware.force:false}")
  private boolean proxyForce;

  @Value("${sitmun.proxy-middleware.url:}")
  private String proxyUrl;

  @Test
  @DisplayName("GET: initialExtent computed from territory 1 extent + center")
  void applicationDetails() throws Exception {
    mvc.perform(get(CONFIG_CLIENT_PROFILE_URI, 1, 1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.application.theme", is("sitmun-base")))
        .andExpect(
            jsonPath(
                "$.application.logo",
                is("https://sitmun.org/Documents/Imatges/8480img1320220524090127.jpg")))
        .andExpect(jsonPath("$.application.srs", is("EPSG:25831")))
        .andExpect(jsonPath("$.application.initialExtent[0]").value(363487.0))
        .andExpect(jsonPath("$.application.initialExtent[1]").value(4561228.0))
        .andExpect(jsonPath("$.application.initialExtent[2]").value(481617.0))
        .andExpect(jsonPath("$.application.initialExtent[3]").value(4686464.0));
  }

  @Test
  @DisplayName("GET: Territory SRS overrides SRS application")
  void territorySrsOverridesSrsApplication() throws Exception {
    mvc.perform(get(CONFIG_CLIENT_PROFILE_URI, 1, 2))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.application.srs", is("EPSG:25830")));
  }

  @Test
  @DisplayName("GET: initialExtent computed from territory 2 extent + center")
  void applicationExtentFromTerritory() throws Exception {
    mvc.perform(get(CONFIG_CLIENT_PROFILE_URI, 1, 2))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.application.initialExtent[0]").value(448046.0))
        .andExpect(jsonPath("$.application.initialExtent[1]").value(4603029.0))
        .andExpect(jsonPath("$.application.initialExtent[2]").value(458244.0))
        .andExpect(jsonPath("$.application.initialExtent[3]").value(4609235.0));
  }

  @Test
  @DisplayName("GET: initialExtent from app-territory override (territory 3, no center)")
  void applicationExtentFromLinkToTerritory() throws Exception {
    mvc.perform(get(CONFIG_CLIENT_PROFILE_URI, 1, 3))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.application.initialExtent[0]").value(430250.0))
        .andExpect(jsonPath("$.application.initialExtent[1]").value(4612070.0))
        .andExpect(jsonPath("$.application.initialExtent[2]").value(469609.0))
        .andExpect(jsonPath("$.application.initialExtent[3]").value(4638298.5));
  }

  @Test
  @DisplayName("GET: Get layers details")
  void layers() throws Exception {
    mvc.perform(get(CONFIG_CLIENT_PROFILE_URI, 1, 1))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.layers[?(@.id=='layer/1')].title", hasItem("WMTS Bases - ICGC- Topo")))
        .andExpect(jsonPath("$.layers[?(@.id=='layer/1')].layers[0]", hasItem("topo")))
        .andExpect(jsonPath("$.layers[?(@.id=='layer/1')].service", hasItem("service/1")))
        .andExpect(
            jsonPath("$.layers[?(@.id=='layer/1')].queryableFeatureEnabled", hasItem(false)));
  }

  @Test
  @DisplayName("GET: Layer scale denominators appear in profile JSON when set")
  void layerScaleDenominatorsInProfile() throws Exception {
    mvc.perform(get(CONFIG_CLIENT_PROFILE_URI, 1, 1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.layers[?(@.id=='layer/1')].minScaleDenominator", hasItem(500)))
        .andExpect(jsonPath("$.layers[?(@.id=='layer/1')].maxScaleDenominator", hasItem(1000000)));
  }

  @Test
  @DisplayName("GET: Layers without scale omit denominator keys")
  void layersWithoutScaleOmitDenominatorKeys() throws Exception {
    mvc.perform(get(CONFIG_CLIENT_PROFILE_URI, 1, 1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.layers[?(@.id=='layer/3')].minScaleDenominator").doesNotExist())
        .andExpect(jsonPath("$.layers[?(@.id=='layer/3')].maxScaleDenominator").doesNotExist());
  }

  @Test
  @DisplayName("GET: Layer transparency appears in profile JSON when set")
  void layerTransparencyInProfile() throws Exception {
    mvc.perform(get(CONFIG_CLIENT_PROFILE_URI, 1, 1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.layers[?(@.id=='layer/1')].transparency", hasItem(50)));
  }

  @Test
  @DisplayName("GET: Layers without transparency omit transparency key")
  void layersWithoutTransparencyOmitKey() throws Exception {
    mvc.perform(get(CONFIG_CLIENT_PROFILE_URI, 1, 1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.layers[?(@.id=='layer/3')].transparency").doesNotExist());
  }

  @Test
  @DisplayName("GET: Layer order appears in profile JSON when set")
  void layerOrderInProfile() throws Exception {
    mvc.perform(get(CONFIG_CLIENT_PROFILE_URI, 1, 1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.layers[?(@.id=='layer/1')].order", hasItem(10)));
  }

  @Test
  @DisplayName("GET: Layers without order omit order key")
  void layersWithoutOrderOmitKey() throws Exception {
    mvc.perform(get(CONFIG_CLIENT_PROFILE_URI, 1, 1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.layers[?(@.id=='layer/4')].order").doesNotExist());
  }

  @Test
  @DisplayName("GET: Get services details")
  void services() throws Exception {
    String url =
        proxyForce
            ? proxyUrl + "/proxy/1/1/WMTS/1"
            : "https://geoserveis.icgc.cat/icc_mapesmultibase/utm/wmts/service";

    mvc.perform(get(CONFIG_CLIENT_PROFILE_URI, 1, 1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.services[?(@.id=='service/1')].url", hasItem(url)))
        .andExpect(jsonPath("$.services[?(@.id=='service/1')].type", hasItem("WMTS")))
        .andExpect(
            jsonPath("$.services[?(@.id=='service/1')].parameters.format", hasItem("image/jpeg")))
        .andExpect(
            jsonPath("$.services[?(@.id=='service/1')].parameters.matrixSet", hasItem("UTM25831")))
        .andExpect(jsonPath("$.services[?(@.id=='service/1')].crs").exists());
  }

  @Test
  @DisplayName("GET: Get groups details")
  void groups() throws Exception {
    mvc.perform(get(CONFIG_CLIENT_PROFILE_URI, 1, 1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.groups[?(@.id=='group/2')].title", hasItem("Background Map")))
        .andExpect(
            jsonPath("$.groups[?(@.id=='group/2')].layers.*", hasItems("layer/1", "layer/2")));
  }

  @Test
  @DisplayName("GET: Get backgrounds details")
  void backgrounds() throws Exception {
    mvc.perform(get(CONFIG_CLIENT_PROFILE_URI, 1, 1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.backgrounds").isArray())
        .andExpect(jsonPath("$.backgrounds[?(@.id=='group/2')].title", hasItem("Background Map")))
        .andExpect(
            jsonPath(
                "$.backgrounds[?(@.id=='group/2')].thumbnail",
                hasItem("http://example.com/background_map.png")))
        .andExpect(jsonPath("$.groups[?(@.id=='group/2')].title", hasItem("Background Map")));
  }

  @Test
  @DisplayName("GET: Get situation map details")
  void situationMap() throws Exception {
    mvc.perform(get(CONFIG_CLIENT_PROFILE_URI, 1, 1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.application.situation-map", is("group/3")));
  }

  @Test
  @DisplayName("GET: Profile situation map excludes blocked cartographies")
  void situationMapExcludesBlockedCartographies() throws Exception {
    mvc.perform(get(CONFIG_CLIENT_PROFILE_URI, 1, 1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.application.situation-map", is("group/3")))
        .andExpect(jsonPath("$.groups[?(@.id=='group/3')].title", hasItem("Situation Map")))
        .andExpect(jsonPath("$.groups[?(@.id=='group/3')].layers.*", hasItem("layer/4")))
        .andExpect(jsonPath("$.groups[?(@.id=='group/3')].layers.*", not(hasItem("layer/10"))))
        .andExpect(jsonPath("$.groups[?(@.id=='group/3')].layers.*", not(hasItem("layer/11"))));
  }

  @Test
  @DisplayName("GET: Get tasks details")
  void tasks() throws Exception {
    mvc.perform(get(CONFIG_CLIENT_PROFILE_URI, 1, 1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.tasks[?(@.id=='task/1')].ui-control", hasItem("sitna.attribution")));
  }

  @Test
  @DisplayName("GET: Get task parameters details")
  void taskParameters() throws Exception {
    mvc.perform(get(CONFIG_CLIENT_PROFILE_URI, 1, 1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.tasks[?(@.id=='task/20')].parameters.div", hasItem("print")))
        .andExpect(
            jsonPath("$.tasks[?(@.id=='task/20')].parameters.legend.visible", hasItem(true)));
  }

  @Test
  @DisplayName(
      "GET: web-api-query (proxied) omits URI template placeholders; client only uses middleware URL")
  void taskWebApiQueryProxiedOmitsTemplateParametersFromProfile() throws Exception {
    String expectedUrl = proxyUrl + "/proxy/1/1/API/35";
    mvc.perform(get(CONFIG_CLIENT_PROFILE_URI, 1, 1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.tasks[?(@.id=='task/35')].scope", hasItem(SCOPE_API)))
        .andExpect(jsonPath("$.tasks[?(@.id=='task/35')].url", hasItem(expectedUrl)))
        .andExpect(jsonPath("$.tasks[?(@.id=='task/35')].parameters").doesNotExist());
  }

  @Test
  @DisplayName("GET: web-api-query (proxied) exposes query parameter and proxy middleware URL")
  void taskWebApiQueryProxiedQueryParameter() throws Exception {
    String expectedUrl = proxyUrl + "/proxy/1/1/API/37";
    mvc.perform(get(CONFIG_CLIENT_PROFILE_URI, 1, 1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.tasks[?(@.id=='task/37')].parameters.limit.type", hasItem("query")))
        .andExpect(jsonPath("$.tasks[?(@.id=='task/37')].scope", hasItem(SCOPE_API)))
        .andExpect(jsonPath("$.tasks[?(@.id=='task/37')].url", hasItem(expectedUrl)));
  }

  @Test
  @DisplayName(
      "GET: web-api-query-no-proxy exposes template parameter, URL scope, and direct command URL")
  void taskWebApiQueryNoProxyTemplateParameter() throws Exception {
    mvc.perform(get(CONFIG_CLIENT_PROFILE_URI, 1, 1))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.tasks[?(@.id=='task/38')].parameters.codigo.type", hasItem("template")))
        .andExpect(jsonPath("$.tasks[?(@.id=='task/38')].scope", hasItem(SCOPE_URL)))
        .andExpect(
            jsonPath(
                "$.tasks[?(@.id=='task/38')].url",
                hasItem("https://api.example.invalid/stopcode/{codigo}")));
  }

  @Test
  @DisplayName(
      "GET: web-api-query-no-proxy exposes query parameter, URL scope, and direct command URL")
  void taskWebApiQueryNoProxyQueryParameter() throws Exception {
    mvc.perform(get(CONFIG_CLIENT_PROFILE_URI, 1, 1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.tasks[?(@.id=='task/39')].parameters.limit.type", hasItem("query")))
        .andExpect(jsonPath("$.tasks[?(@.id=='task/39')].scope", hasItem(SCOPE_URL)))
        .andExpect(
            jsonPath(
                "$.tasks[?(@.id=='task/39')].url",
                hasItem("https://api.example.invalid/page?limit={limit}")));
  }

  @Test
  @DisplayName(
      "GET: sql-query exposes query parameter type, SQL scope, and JDBC proxy middleware URL")
  void taskSqlQueryQueryParameterAndProxyUrl() throws Exception {
    String expectedUrl = proxyUrl + "/proxy/1/1/SQL/34";
    mvc.perform(get(CONFIG_CLIENT_PROFILE_URI, 1, 1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.tasks[?(@.id=='task/34')].parameters.limit.type", hasItem("query")))
        .andExpect(jsonPath("$.tasks[?(@.id=='task/34')].scope", hasItem(SCOPE_SQL)))
        .andExpect(jsonPath("$.tasks[?(@.id=='task/34')].url", hasItem(expectedUrl)));
  }

  @Test
  @DisplayName(
      "GET: sql-query exposes template parameter type, SQL scope, and JDBC proxy middleware URL")
  void taskSqlQueryTemplateParameterAndProxyUrl() throws Exception {
    String expectedUrl = proxyUrl + "/proxy/1/1/SQL/40";
    mvc.perform(get(CONFIG_CLIENT_PROFILE_URI, 1, 1))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.tasks[?(@.id=='task/40')].parameters.codigo.type", hasItem("template")))
        .andExpect(jsonPath("$.tasks[?(@.id=='task/40')].scope", hasItem(SCOPE_SQL)))
        .andExpect(jsonPath("$.tasks[?(@.id=='task/40')].url", hasItem(expectedUrl)));
  }

  @Test
  @DisplayName("GET: Get tree details")
  void tree() throws Exception {
    mvc.perform(get(CONFIG_CLIENT_PROFILE_URI, 1, 1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.trees[?(@.id=='tree/1')].title", hasItem("Provincial")))
        .andExpect(jsonPath("$.trees[?(@.id=='tree/1')].rootNode", hasItem("node/tree/1")))
        .andExpect(
            jsonPath("$.trees[?(@.id=='tree/1')].nodes['node/tree/1'].children[*]").isArray())
        .andExpect(
            jsonPath("$.trees[?(@.id=='tree/1')].nodes['node/tree/1'].children[*]", hasSize(2)))
        .andExpect(
            jsonPath(
                "$.trees[?(@.id=='tree/1')].nodes['node/tree/1'].children[*]",
                containsInAnyOrder("node/1", "node/7")))
        .andExpect(
            jsonPath("$.trees[?(@.id=='tree/1')].nodes['node/tree/1'].loadData", hasItem(false)))
        .andExpect(
            jsonPath("$.trees[?(@.id=='tree/1')].nodes['node/9'].resource", hasItem("layer/9")))
        .andExpect(jsonPath("$.trees[?(@.id=='tree/1')].nodes['node/9'].loadData", hasItem(false)));
  }

  @Test
  @DisplayName("GET: Profile tree excludes inactive leaf nodes")
  void treeExcludesInactiveLeaf() throws Exception {
    mvc.perform(get(CONFIG_CLIENT_PROFILE_URI, 1, 1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.trees[?(@.id=='tree/1')].nodes['node/12']").doesNotExist())
        .andExpect(
            jsonPath(
                "$.trees[?(@.id=='tree/1')].nodes['node/7'].children[*]", not(hasItem("node/12"))));
  }

  @Test
  @DisplayName("GET: Profile tree excludes inactive folder and its descendants")
  void treeExcludesInactiveFolderSubtree() throws Exception {
    mvc.perform(get(CONFIG_CLIENT_PROFILE_URI, 1, 1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.trees[?(@.id=='tree/1')].nodes['node/13']").doesNotExist())
        .andExpect(jsonPath("$.trees[?(@.id=='tree/1')].nodes['node/14']").doesNotExist())
        .andExpect(
            jsonPath(
                "$.trees[?(@.id=='tree/1')].nodes['node/1'].children[*]", not(hasItem("node/13"))));
  }

  @Test
  @DisplayName("GET: Profile tree excludes nodes referencing blocked cartographies")
  void treeExcludesNodesWithBlockedCartographies() throws Exception {
    mvc.perform(get(CONFIG_CLIENT_PROFILE_URI, 1, 1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.trees[?(@.id=='tree/1')].nodes['node/10']").doesNotExist())
        .andExpect(jsonPath("$.trees[?(@.id=='tree/1')].nodes['node/11']").doesNotExist())
        .andExpect(
            jsonPath(
                "$.trees[?(@.id=='tree/1')].nodes['node/7'].children[*]", not(hasItem("node/10"))))
        .andExpect(
            jsonPath(
                "$.trees[?(@.id=='tree/1')].nodes['node/7'].children[*]", not(hasItem("node/11"))));
  }

  @Test
  @DisplayName("GET: Ensure order in children")
  void treeNodeOrder() throws Exception {
    mvc.perform(get(CONFIG_CLIENT_PROFILE_URI, 1, 1))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath(
                "$.trees[?(@.id=='tree/1')].nodes['node/tree/1'].children[*]",
                containsInRelativeOrder("node/1", "node/7")))
        .andExpect(jsonPath("$.trees[?(@.id=='tree/1')].nodes['node/1'].order", hasItem(1)))
        .andExpect(jsonPath("$.trees[?(@.id=='tree/1')].nodes['node/7'].order", hasItem(2)))
        .andExpect(
            jsonPath(
                "$.trees[?(@.id=='tree/1')].nodes['node/7'].children[*]",
                containsInRelativeOrder("node/9", "node/8")))
        .andExpect(jsonPath("$.trees[?(@.id=='tree/1')].nodes['node/9'].order", hasItem(1)))
        .andExpect(jsonPath("$.trees[?(@.id=='tree/1')].nodes['node/8'].order", hasItem(2)));
  }

  @Test
  @DisplayName("GET: Get proxy details")
  void proxy() throws Exception {
    mvc.perform(get(CONFIG_CLIENT_PROFILE_URI, 1, 1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.global." + SitmunConstants.PROXY_CONF_KEY, is(proxyUrl)));
  }

  @Test
  @DisplayName("GET: Get application zoom")
  void zooms() throws Exception {
    mvc.perform(get(CONFIG_CLIENT_PROFILE_URI, 1, 1))
        .andExpect(jsonPath("$.application.defaultZoomLevel", is(8)));
    mvc.perform(get(CONFIG_CLIENT_PROFILE_URI, 1, 3))
        .andExpect(jsonPath("$.application.defaultZoomLevel", nullValue()));
  }

  @Test
  @DisplayName("GET: Get application point of interest")
  void pointOfInterest() throws Exception {
    mvc.perform(get(CONFIG_CLIENT_PROFILE_URI, 1, 1))
        .andExpect(jsonPath("$.application.pointOfInterest.x", is(422552.0)))
        .andExpect(jsonPath("$.application.pointOfInterest.y", is(4623846.0)));
    mvc.perform(get(CONFIG_CLIENT_PROFILE_URI, 1, 3))
        .andExpect(jsonPath("$.application.pointOfInterest", nullValue()));
  }

  @Test
  @DisplayName("GET: Get the root page in incremental mode")
  void modePageRootPage() throws Exception {
    mvc.perform(get(URIConstants.CONFIG_CLIENT_PROFILE_URI_FILTERED, 1, 1, "none"))
        .andExpect(jsonPath("$.trees[0].rootNode", is("node/tree/1")))
        .andExpect(jsonPath("$.trees[0].nodes.size()", is(10)));

    mvc.perform(get(URIConstants.CONFIG_CLIENT_PROFILE_URI_FILTERED, 1, 1, "node"))
        .andExpect(jsonPath("$.trees[0].rootNode", is("node/tree/1")))
        .andExpect(jsonPath("$.trees[0].nodes.size()", is(3)));

    mvc.perform(get(URIConstants.CONFIG_CLIENT_PROFILE_URI_FILTERED, 1, 1, "node/1"))
        .andExpect(jsonPath("$.trees[0].rootNode", is("node/1")))
        .andExpect(jsonPath("$.trees[0].nodes.size()", is(3)));
  }

  @Test
  @DisplayName("GET: Profile omits cartographies, services, and tasks tied to blocked services")
  void profileOmitsBlockedServiceContent() throws Exception {
    mvc.perform(get(CONFIG_CLIENT_PROFILE_URI, 1, 1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.layers[*].id", not(hasItem("layer/10"))))
        .andExpect(jsonPath("$.services[*].id", not(hasItem("service/8"))))
        .andExpect(jsonPath("$.tasks[*].id", not(hasItem("task/36"))));
  }

  @Test
  @DisplayName(
      "GET: Profile omits cartographies blocked at layer (GEO_BLOCKED) with usable service")
  void profileOmitsGeoBlockedCartography() throws Exception {
    mvc.perform(get(CONFIG_CLIENT_PROFILE_URI, 1, 1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.layers[*].id", not(hasItem("layer/11"))));
  }

  @Test
  @DisplayName("GET: Profile includes background group but excludes blocked members")
  void backgroundGroupExcludesBlockedLayers() throws Exception {
    mvc.perform(get(CONFIG_CLIENT_PROFILE_URI, 1, 1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.groups[?(@.id=='group/2')].title", hasItem("Background Map")))
        .andExpect(
            jsonPath("$.groups[?(@.id=='group/2')].layers.*", hasItems("layer/1", "layer/2")))
        .andExpect(jsonPath("$.groups[?(@.id=='group/2')].layers.*", not(hasItem("layer/10"))))
        .andExpect(jsonPath("$.groups[?(@.id=='group/2')].layers.*", not(hasItem("layer/11"))));
  }

  // Phase 1 snapshot tests: lock wire format for parameters and top-level TaskDto fields

  @Test
  @DisplayName("GET: SQL query task - parameters block and top-level fields snapshot")
  void sqlQueryTaskWireFormatSnapshot() throws Exception {
    String expectedUrl =
        (proxyForce ? proxyUrl : "http://localhost:8080/middleware") + "/proxy/1/1/SQL/34";
    mvc.perform(get(CONFIG_CLIENT_PROFILE_URI, 1, 1))
        .andExpect(status().isOk())
        // Parameters block
        .andExpect(jsonPath("$.tasks[?(@.id=='task/34')].parameters.limit.type", hasItem("query")))
        .andExpect(jsonPath("$.tasks[?(@.id=='task/34')].parameters.limit.required", hasItem(true)))
        // Parameters must not contain label, value, name
        .andExpect(jsonPath("$.tasks[?(@.id=='task/34')].parameters.limit.label").doesNotExist())
        .andExpect(jsonPath("$.tasks[?(@.id=='task/34')].parameters.limit.value").doesNotExist())
        .andExpect(jsonPath("$.tasks[?(@.id=='task/34')].parameters.limit.name").doesNotExist())
        // Top-level TaskDto fields
        .andExpect(jsonPath("$.tasks[?(@.id=='task/34')].scope", hasItem(SCOPE_SQL)))
        .andExpect(jsonPath("$.tasks[?(@.id=='task/34')].url", hasItem(expectedUrl)))
        .andExpect(jsonPath("$.tasks[?(@.id=='task/34')].type", hasItem("simple")))
        // ui-control field
        .andExpect(
            jsonPath("$.tasks[?(@.id=='task/34')]['ui-control']").value(everyItem(nullValue())));
  }

  @Test
  @DisplayName("GET: Web API proxied task - parameters block and top-level fields snapshot")
  void webApiProxiedTaskWireFormatSnapshot() throws Exception {
    String expectedUrl =
        (proxyForce ? proxyUrl : "http://localhost:8080/middleware") + "/proxy/1/1/API/37";
    mvc.perform(get(CONFIG_CLIENT_PROFILE_URI, 1, 1))
        .andExpect(status().isOk())
        // Parameters block
        .andExpect(jsonPath("$.tasks[?(@.id=='task/37')].parameters.limit.type", hasItem("query")))
        .andExpect(
            jsonPath("$.tasks[?(@.id=='task/37')].parameters.limit.required", hasItem(false)))
        // Top-level TaskDto fields
        .andExpect(jsonPath("$.tasks[?(@.id=='task/37')].scope", hasItem(SCOPE_API)))
        .andExpect(jsonPath("$.tasks[?(@.id=='task/37')].url", hasItem(expectedUrl)))
        .andExpect(jsonPath("$.tasks[?(@.id=='task/37')].type", hasItem("simple")))
        // ui-control field
        .andExpect(
            jsonPath("$.tasks[?(@.id=='task/37')]['ui-control']").value(everyItem(nullValue())));
  }

  @Test
  @DisplayName("GET: Web API direct/URL task - parameters block and top-level fields snapshot")
  void webApiDirectTaskWireFormatSnapshot() throws Exception {
    mvc.perform(get(CONFIG_CLIENT_PROFILE_URI, 1, 1))
        .andExpect(status().isOk())
        // Parameters block
        .andExpect(
            jsonPath("$.tasks[?(@.id=='task/38')].parameters.codigo.type", hasItem("template")))
        .andExpect(
            jsonPath("$.tasks[?(@.id=='task/38')].parameters.codigo.required", hasItem(true)))
        // Top-level TaskDto fields
        .andExpect(jsonPath("$.tasks[?(@.id=='task/38')].scope", hasItem(SCOPE_URL)))
        .andExpect(
            jsonPath(
                "$.tasks[?(@.id=='task/38')].url",
                hasItem("https://api.example.invalid/stopcode/{codigo}")))
        .andExpect(jsonPath("$.tasks[?(@.id=='task/38')].type", hasItem("simple")))
        // ui-control field
        .andExpect(
            jsonPath("$.tasks[?(@.id=='task/38')]['ui-control']").value(everyItem(nullValue())));
  }

  @Test
  @DisplayName("GET: Task with ui-control field present")
  void taskWithUiControl() throws Exception {
    mvc.perform(get(CONFIG_CLIENT_PROFILE_URI, 1, 1))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.tasks[?(@.id=='task/1')]['ui-control']", hasItem("sitna.attribution")));
  }

  @Test
  @DisplayName("GET: Task parameters must never emit provided flag or queryType/apiUrl keys")
  void taskParametersContractViolationDefense() throws Exception {
    mvc.perform(get(CONFIG_CLIENT_PROFILE_URI, 1, 1))
        .andExpect(status().isOk())
        // Verify no task parameters contain 'provided' key
        .andExpect(jsonPath("$.tasks[*].parameters.*.provided").doesNotExist())
        // TaskDto.parameters must not inject top-level keys named queryType or apiUrl
        // (viewer checks task.parameters.queryType, which is different from TaskDto.queryType)
        // This test verifies parameters CAN have these names (they're valid parameter names)
        // but they don't leak into top-level TaskDto fields
        .andExpect(jsonPath("$.tasks[*].queryType").doesNotExist())
        .andExpect(jsonPath("$.tasks[*].apiUrl").doesNotExist());
  }
}
