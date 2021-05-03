package org.sitmun.plugin.core.repository.rest;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sitmun.plugin.core.domain.Cartography;
import org.sitmun.plugin.core.domain.CartographyAvailability;
import org.sitmun.plugin.core.domain.Service;
import org.sitmun.plugin.core.domain.Territory;
import org.sitmun.plugin.core.repository.CartographyAvailabilityRepository;
import org.sitmun.plugin.core.repository.CartographyRepository;
import org.sitmun.plugin.core.repository.ServiceRepository;
import org.sitmun.plugin.core.repository.TerritoryRepository;
import org.sitmun.plugin.core.test.TestUtils;
import org.sitmun.plugin.core.test.URIConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.sitmun.plugin.core.test.TestConstants.SITMUN_ADMIN_USERNAME;
import static org.sitmun.plugin.core.test.TestUtils.withMockSitmunAdmin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
public class CartographyRepositoryDataRestTest {

  private static final String CARTOGRAPHY_NAME = "Cartography Name";

  @Autowired
  private CartographyRepository cartographyRepository;
  @Autowired
  private CartographyAvailabilityRepository cartographyAvailabilityRepository;
  @Autowired
  private TerritoryRepository territoryRepository;
  @Autowired
  private ServiceRepository serviceRepository;
  @Autowired
  private MockMvc mvc;

  private Territory territory;
  private Cartography cartography;
  private Service service;
  private ArrayList<Cartography> cartographies;
  private ArrayList<CartographyAvailability> availabilities;

  @BeforeEach
  public void init() {

    withMockSitmunAdmin(() -> {

      territory = Territory.builder()
        .name("Territorio 1")
        .code("")
        .blocked(false)
        .build();
      territoryRepository.save(territory);

      service = Service.builder()
        .name("Service")
        .serviceURL("")
        .type("")
        .blocked(false)
        .build();
      serviceRepository.save(service);

      cartographies = new ArrayList<>();
      availabilities = new ArrayList<>();

      Cartography.CartographyBuilder cartographyDefaults = Cartography.builder()
        .type("I")
        .name(CARTOGRAPHY_NAME)
        .layers(Collections.emptyList())
        .queryableFeatureAvailable(false)
        .queryableFeatureEnabled(false)
        .service(service)
        .availabilities(Collections.emptySet())
        .blocked(false);

      cartography = cartographyDefaults
        .name(CARTOGRAPHY_NAME)
        .build();
      cartographies.add(cartography);

      Cartography cartographyWithAvailabilities = cartographyDefaults
        .name("Cartography with availabilities")
        .build();

      cartographies.add(cartographyWithAvailabilities);

      cartographyRepository.saveAll(cartographies);
      CartographyAvailability cartographyAvailability1 = new CartographyAvailability();
      cartographyAvailability1.setCartography(cartographyWithAvailabilities);
      cartographyAvailability1.setTerritory(territory);
      cartographyAvailability1.setCreatedDate(new Date());
      availabilities.add(cartographyAvailability1);

      cartographyAvailabilityRepository.saveAll(availabilities);
    });
  }

  @AfterEach
  public void after() {
    withMockSitmunAdmin(() -> {
      cartographyAvailabilityRepository.deleteAll(availabilities);
      cartographyRepository.deleteAll(cartographies);
      serviceRepository.delete(service);
      territoryRepository.delete(territory);
    });
  }

  @Test
  public void postCartography() throws Exception {

    String content = new JSONObject()
      .put("name", CARTOGRAPHY_NAME)
      .put("layers", new JSONArray())
      .put("queryableFeatureAvailable", false)
      .put("queryableFeatureEnabled", false)
      .put("service", "http://localhost/api/services/" + service.getId())
      .put("blocked", false)
      .toString();

    String location = mvc.perform(post(URIConstants.CARTOGRAPHIES_URI)
      .contentType(MediaType.APPLICATION_JSON)
      .content(content)
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
    ).andExpect(status().isCreated())
      .andReturn().getResponse().getHeader("Location");

    assertThat(location, notNullValue());

    mvc.perform(get(location)
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME)))
      .andExpect(status().isOk())
      .andExpect(content().contentType(MediaTypes.HAL_JSON))
      .andExpect(jsonPath("$.name", equalTo(CARTOGRAPHY_NAME)));

    withMockSitmunAdmin(() -> {
      String[] paths = URI.create(location).getPath().split("/");
      Integer id = Integer.parseInt(paths[paths.length - 1]);
      cartographyRepository.findById(id).ifPresent((it) -> cartographies.add(it));
    });
  }

  @Test
  public void getCartographiesAsPublic() throws Exception {
    mvc.perform(get(URIConstants.CARTOGRAPHIES_URI)
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME)))
      .andExpect(status().isOk());
  }

  @Test
  public void postCartographyAsPublicUserFails() throws Exception {
    mvc.perform(post(URIConstants.CARTOGRAPHIES_URI)
      .contentType(MediaType.APPLICATION_JSON)
      .content(TestUtils.asJsonString(cartography)))
      .andExpect(status().is4xxClientError()).andReturn();
  }

  @Test
  public void hasTreeNodeListProperty() throws Exception {
    mvc.perform(get(URIConstants.CARTOGRAPHIES_URI + "?size=10")
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME)))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.cartographies[*]", hasSize(10)))
      .andExpect(jsonPath("$._embedded.cartographies[*]._links.treeNodes", hasSize(10)));
  }

  @Test
  public void hasAccessToCartographyGroups() throws Exception {
    mvc.perform(get(URIConstants.CARTOGRAPHY_URI_PERMISSION_URI, 85)
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME)))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.cartography-groups[*]", hasSize(1)));
  }

  /**
   * This requeste must be rejected because applyFilterXXX should
   * be a boolean instead of a number.
   *
   * @see <a href="https://github.com/sitmun/sitmun-admin-app/issues/41"/>Github</a>
   */
  //
  @Test
  public void checkIssueSitmunAdminApp41() throws Exception {
    String badRequest = "{\"name\":\"SEE1M - Recollida selectiva\"," +
      "\"layers\":[\"SEE1M_111P_MA\"]," +
      "\"minimumScale\":null," +
      "\"maximumScale\":85000," +
      "\"order\":3577," +
      "\"transparency\":0," +
      "\"metadataURL\":\"http://sitmun.diba.cat/sitmun2/metadada.jsp?title=SE${MUN_INE}&lang=${LANG}\"," +
      "\"legendType\":\"CAPABILITIES\"," +
      "\"legendURL\":null," +
      "\"description\":null," +
      "\"datasetURL\":null," +
      "\"applyFilterToGetFeatureInfo\":\"0\"," +
      "\"applyFilterToSpatialSelection\":\"1\"," +
      "\"queryableFeatureEnabled\":true," +
      "\"queryableFeatureAvailable\":false," +
      "\"queryableLayers\":[\"SEE1M_111P_MA\"]," +
      "\"thematic\":false," +
      "\"blocked\":false," +
      "\"selectableFeatureEnabled\":true," +
      "\"selectableLayers\":[\"DIBA:SIT_SEE1MV1_111P_MA\"]," +
      "\"_links\":{" +
      "\"self\":{\"href\":\"https://sitmun-backend-core.herokuapp.com/api/cartographies/724\"}," +
      "\"cartography\":{\"href\":\"https://sitmun-backend-core.herokuapp.com/api/cartographies/724{?projection}\",\"templated\":true}," +
      "\"filters\":{\"href\":\"https://sitmun-backend-core.herokuapp.com/api/cartographies/724/filters\"}," +
      "\"spatialSelectionConnection\":{\"href\":\"https://sitmun-backend-core.herokuapp.com/api/cartographies/724/spatialSelectionConnection\"}," +
      "\"availabilities\":{\"href\":\"https://sitmun-backend-core.herokuapp.com/api/cartographies/724/availabilities{?projection}\",\"templated\":true}," +
      "\"parameters\":{\"href\":\"https://sitmun-backend-core.herokuapp.com/api/cartographies/724/parameters\"}," +
      "\"service\":{\"href\":\"https://sitmun-backend-core.herokuapp.com/api/cartographies/724/service\"}," +
      "\"spatialSelectionService\":{\"href\":\"https://sitmun-backend-core.herokuapp.com/api/cartographies/724/spatialSelectionService\"}," +
      "\"defaultStyle\":{\"href\":\"https://sitmun-backend-core.herokuapp.com/api/cartographies/724/defaultStyle\"}," +
      "\"permissions\":{\"href\":\"https://sitmun-backend-core.herokuapp.com/api/cartographies/724/permissions\"}," +
      "\"styles\":{\"href\":\"https://sitmun-backend-core.herokuapp.com/api/cartographies/724/styles\"}," +
      "\"treeNodes\":{\"href\":\"https://sitmun-backend-core.herokuapp.com/api/cartographies/724/treeNodes{?projection}\",\"templated\":true}}" +
      "}";
    mvc.perform(put(URIConstants.CARTOGRAPHY_URI, 724)
      .contentType(MediaType.APPLICATION_JSON)
      .content(badRequest)
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME)))
      .andDo(print())
      .andExpect(status().isUnprocessableEntity());

  }

}
