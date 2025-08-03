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
    serviceRepository.delete(service);
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
    mvc.perform(get(CARTOGRAPHIES_URI + "?size=10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.cartographies[*]", hasSize(10)))
        .andExpect(jsonPath("$._embedded.cartographies[*]._links.treeNodes", hasSize(10)));
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
}
