package org.sitmun.domain.cartography;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.sitmun.test.DateTimeMatchers.isIso8601DateAndTime;
import static org.sitmun.test.TestUtils.*;
import static org.sitmun.test.URIConstants.*;
import static org.springframework.http.MediaType.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.domain.cartography.availability.CartographyAvailability;
import org.sitmun.domain.cartography.availability.CartographyAvailabilityRepository;
import org.sitmun.domain.service.Service;
import org.sitmun.domain.service.ServiceRepository;
import org.sitmun.domain.territory.Territory;
import org.sitmun.domain.territory.TerritoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.hateoas.MediaTypes;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Cartography Repository Data REST test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CartographyRepositoryDataRestTest {

  private static final String CARTOGRAPHY_NAME = "Cartography Name";

  @Autowired private CartographyRepository cartographyRepository;
  @Autowired private CartographyAvailabilityRepository cartographyAvailabilityRepository;
  @Autowired private TerritoryRepository territoryRepository;
  @Autowired private ServiceRepository serviceRepository;
  @Autowired private MockMvc mvc;

  private Territory territory;
  private Cartography cartography;
  private Service service;
  private ArrayList<Cartography> cartographies;
  private ArrayList<CartographyAvailability> availabilities;
  private ArrayList<Service> services;

  @BeforeEach
  void init() {
    territory = Territory.builder().name("Territorio 1").code("some-code").blocked(false).build();
    territoryRepository.save(territory);

    service =
        Service.builder()
            .name("Service")
            .serviceURL("http://localhost/api/services/1")
            .type("service-type")
            .blocked(false)
            .build();
    serviceRepository.save(service);
    services = new ArrayList<>();
    services.add(service);

    cartographies = new ArrayList<>();
    availabilities = new ArrayList<>();

    Cartography.CartographyBuilder cartographyDefaults =
        Cartography.builder()
            .type("I")
            .name(CARTOGRAPHY_NAME)
            .layers(List.of("Layer1", "Layer2"))
            .queryableFeatureAvailable(false)
            .queryableFeatureEnabled(false)
            .service(service)
            .availabilities(Collections.emptySet())
            .blocked(false);

    cartography = cartographyDefaults.name(CARTOGRAPHY_NAME).build();
    cartographies.add(cartography);

    Cartography cartographyWithAvailabilities =
        cartographyDefaults.name("Cartography with availabilities").build();

    cartographies.add(cartographyWithAvailabilities);

    cartographyRepository.saveAll(cartographies);
    CartographyAvailability cartographyAvailability1 = new CartographyAvailability();
    cartographyAvailability1.setCartography(cartographyWithAvailabilities);
    cartographyAvailability1.setTerritory(territory);
    cartographyAvailability1.setCreatedDate(
        Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()));
    availabilities.add(cartographyAvailability1);

    cartographyAvailabilityRepository.saveAll(availabilities);
  }

  @AfterEach
  void after() {
    cartographyAvailabilityRepository.deleteAll(availabilities);
    cartographyRepository.deleteAll(cartographies);
    serviceRepository.deleteAll(services);
    territoryRepository.delete(territory);
  }

  @Test
  @DisplayName("POST: minimum set of properties")
  @WithMockUser(roles = "ADMIN")
  void postCartography() throws Exception {

    String content =
        new JSONObject()
            .put("name", CARTOGRAPHY_NAME)
            .put("layers", new JSONArray(List.of("Layer1", "Layer2")))
            .put("queryableFeatureAvailable", false)
            .put("queryableFeatureEnabled", false)
            .put("service", "http://localhost/api/services/" + service.getId())
            .put("blocked", false)
            .toString();

    String location =
        mvc.perform(post(CARTOGRAPHIES_URI).contentType(APPLICATION_JSON).content(content))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getHeader("Location");

    assertThat(location, notNullValue());

    mvc.perform(get(location))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaTypes.HAL_JSON))
        .andExpect(jsonPath("$.name", equalTo(CARTOGRAPHY_NAME)))
        .andExpect(jsonPath("$.createdDate", isIso8601DateAndTime()));

    Integer id = extractId(location);
    cartographyRepository.findById(id).ifPresent(it -> cartographies.add(it));
  }

  @Test
  @DisplayName("POST: fail as public user")
  void postCartographyAsPublicUserFails() throws Exception {
    mvc.perform(
            post(CARTOGRAPHIES_URI)
                .contentType(APPLICATION_JSON)
                .content(asJsonString(cartography)))
        .andExpect(status().is4xxClientError())
        .andReturn();
  }

  @Test
  @DisplayName("GET: has treeNodes property")
  @WithMockUser(roles = "ADMIN")
  void hasTreeNodeListProperty() throws Exception {
    for (Cartography cartography : cartographies) {
      mvc.perform(get(CARTOGRAPHY_URI, cartography.getId()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$._links.treeNodes").exists());
    }
  }

  @Test
  @DisplayName("GET: has cartography-groups property")
  @WithMockUser(roles = "ADMIN")
  void hasAccessToCartographyGroups() throws Exception {
    mvc.perform(get(CARTOGRAPHY_URI_PERMISSION_URI, 1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.cartography-groups[*]", hasSize(1)));
  }

  /**
   * This request must be rejected because applyFilterXXX should be a boolean instead of a number.
   *
   * @see <a href="https://github.com/sitmun/sitmun-admin-app/issues/41"/>Github</a>
   */
  @Test
  @DisplayName("PUT: applyFilterXXX must not use integers as use booleans")
  @WithMockUser(roles = "ADMIN")
  void checkIssueSitmunAdminApp41() throws Exception {
    String badRequest =
        """
      {
          "name": "SEE1M - Recollida selectiva",
          "layers": [
              "SEE1M_111P_MA"
          ],
          "minimumScale": null,
          "maximumScale": 85000,
          "order": 3577,
          "transparency": 0,
          "metadataURL": "http://sitmun.diba.cat/sitmun2/metadada.jsp?title=SE${MUN_INE}&lang=${LANG}",
          "legendType": "CAPABILITIES",
          "legendURL": null,
          "description": null,
          "datasetURL": null,
          "applyFilterToGetFeatureInfo": "0",
          "applyFilterToSpatialSelection": "1",
          "queryableFeatureEnabled": true,
          "queryableFeatureAvailable": false,
          "queryableLayers": [
              "SEE1M_111P_MA"
          ],
          "thematic": false,
          "blocked": false,
          "selectableFeatureEnabled": true,
          "selectableLayers": [
              "DIBA:SIT_SEE1MV1_111P_MA"
          ]
      }""";
    mvc.perform(put(CARTOGRAPHY_URI, 724).contentType(APPLICATION_JSON).content(badRequest))
        .andExpect(jsonPath("$.errors[0].field", is("applyFilterToGetFeatureInfo")))
        .andExpect(status().isUnprocessableEntity());
  }

  /**
   * Test values are null.
   *
   * @see <a href="https://github.com/sitmun/sitmun-admin-app/issues/41"/>Github</a>
   */
  @Test
  @DisplayName("GET: filters are available in projections")
  @WithMockUser(roles = "ADMIN")
  void applyFilterTestDataIsNull() throws Exception {
    mvc.perform(get(CARTOGRAPHY_URI, 1).contentType(APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.applyFilterToGetFeatureInfo", is(true)))
        .andExpect(jsonPath("$.applyFilterToSpatialSelection", is(true)));

    mvc.perform(get(CARTOGRAPHY_URI_PROJECTION, 1).contentType(APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.applyFilterToGetFeatureInfo", is(true)))
        .andExpect(jsonPath("$.applyFilterToSpatialSelection", is(true)));
  }

  @Test
  @DisplayName("GET search/content: returns only matching cartographies")
  @WithMockUser(roles = "ADMIN")
  void searchContentReturnsOnlyMatchingCartographies() throws Exception {
    saveCartography("Needle Search Alpha");
    saveCartography("Unrelated Search Beta");

    mvc.perform(
            get(CARTOGRAPHIES_URI + "/search/content")
                .param("q", "Needle Search")
                .param("projection", "view")
                .param("page", "0")
                .param("size", "100")
                .param("sort", "name,ASC")
                .param("sort", "id,ASC"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.cartographies", hasSize(1)))
        .andExpect(jsonPath("$._embedded.cartographies[0].name", is("Needle Search Alpha")));
  }

  @Test
  @DisplayName("GET search/content: is case-insensitive")
  @WithMockUser(roles = "ADMIN")
  void searchContentIsCaseInsensitive() throws Exception {
    saveCartography("Case Insensitive Layer");

    mvc.perform(
            get(CARTOGRAPHIES_URI + "/search/content")
                .param("q", "cAsE iNsEnSiTiVe")
                .param("projection", "view")
                .param("page", "0")
                .param("size", "100")
                .param("sort", "name,ASC")
                .param("sort", "id,ASC"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.cartographies", hasSize(1)))
        .andExpect(jsonPath("$._embedded.cartographies[0].name", is("Case Insensitive Layer")));
  }

  @Test
  @DisplayName("GET search/content: searches service name and preserves projection")
  @WithMockUser(roles = "ADMIN")
  void searchContentWorksWithProjectionServiceName() throws Exception {
    Service searchService = saveService("Unique Search Service");
    saveCartography("Layer Matched By Service", searchService);

    mvc.perform(
            get(CARTOGRAPHIES_URI + "/search/content")
                .param("q", "Unique Search Service")
                .param("projection", "view")
                .param("page", "0")
                .param("size", "100")
                .param("sort", "name,ASC")
                .param("sort", "id,ASC"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.cartographies", hasSize(1)))
        .andExpect(
            jsonPath("$._embedded.cartographies[0].serviceName", is("Unique Search Service")));
  }

  @Test
  @DisplayName("GET search/content: reports filtered page totals")
  @WithMockUser(roles = "ADMIN")
  void searchContentReportsFilteredPageTotals() throws Exception {
    saveCartography("Paged Search One");
    saveCartography("Paged Search Two");
    saveCartography("Paged Other");

    mvc.perform(
            get(CARTOGRAPHIES_URI + "/search/content")
                .param("q", "Paged Search")
                .param("projection", "view")
                .param("page", "0")
                .param("size", "1")
                .param("sort", "name,ASC")
                .param("sort", "id,ASC"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.cartographies", hasSize(1)))
        .andExpect(jsonPath("$.page.totalElements", is(2)))
        .andExpect(jsonPath("$.page.totalPages", is(2)));
  }

  @Test
  @DisplayName("GET search/content: duplicate-name paging is deterministic with id tie-breaker")
  @WithMockUser(roles = "ADMIN")
  void searchContentSortsDeterministicallyWithIdTieBreaker() throws Exception {
    saveCartography("Duplicate Search");
    saveCartography("Duplicate Search");

    String firstFetch =
        mvc.perform(
                get(CARTOGRAPHIES_URI + "/search/content")
                    .param("q", "Duplicate Search")
                    .param("projection", "view")
                    .param("page", "0")
                    .param("size", "1")
                    .param("sort", "name,ASC")
                    .param("sort", "id,ASC"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.cartographies", hasSize(1)))
            .andExpect(jsonPath("$.page.totalElements", is(2)))
            .andReturn()
            .getResponse()
            .getContentAsString();

    mvc.perform(
            get(CARTOGRAPHIES_URI + "/search/content")
                .param("q", "Duplicate Search")
                .param("projection", "view")
                .param("page", "1")
                .param("size", "1")
                .param("sort", "name,ASC")
                .param("sort", "id,ASC"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.cartographies", hasSize(1)))
        .andExpect(jsonPath("$.page.number", is(1)));

    String secondFetch =
        mvc.perform(
                get(CARTOGRAPHIES_URI + "/search/content")
                    .param("q", "Duplicate Search")
                    .param("projection", "view")
                    .param("page", "0")
                    .param("size", "1")
                    .param("sort", "name,ASC")
                    .param("sort", "id,ASC"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.cartographies", hasSize(1)))
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(secondFetch, is(firstFetch));
  }

  @SuppressWarnings("UnusedReturnValue")
  private Cartography saveCartography(String name) {
    return saveCartography(name, service);
  }

  private Cartography saveCartography(String name, Service cartographyService) {
    Cartography saved =
        cartographyRepository.save(
            Cartography.builder()
                .type("I")
                .name(name)
                .layers(List.of("Layer1", "Layer2"))
                .queryableFeatureAvailable(false)
                .queryableFeatureEnabled(false)
                .service(cartographyService)
                .availabilities(Collections.emptySet())
                .blocked(false)
                .build());
    cartographies.add(saved);
    return saved;
  }

  private Service saveService(@SuppressWarnings("SameParameterValue") String name) {
    Service saved =
        serviceRepository.save(
            Service.builder()
                .name(name)
                .serviceURL("http://localhost/api/services/" + name.replace(" ", "-"))
                .type("service-type")
                .blocked(false)
                .build());
    services.add(saved);
    return saved;
  }
}
