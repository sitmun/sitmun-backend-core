package org.sitmun.plugin.core.web.rest;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sitmun.plugin.core.domain.Cartography;
import org.sitmun.plugin.core.domain.CartographyAvailability;
import org.sitmun.plugin.core.domain.Service;
import org.sitmun.plugin.core.domain.Territory;
import org.sitmun.plugin.core.repository.CartographyAvailabilityRepository;
import org.sitmun.plugin.core.repository.CartographyRepository;
import org.sitmun.plugin.core.repository.ServiceRepository;
import org.sitmun.plugin.core.repository.TerritoryRepository;
import org.sitmun.plugin.core.security.TokenProvider;
import org.sitmun.plugin.core.test.TestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class CartographyRestResourceIntTest {

  private static final String ADMIN_USERNAME = "admin";
  private static final String CARTOGRAPHY_NAME = "Cartography Name";

  private static final String CARTOGRAPHY_URI = "http://localhost/api/cartographies";
  @Autowired
  CartographyRepository cartographyRepository;
  @Autowired
  CartographyAvailabilityRepository cartographyAvailabilityRepository;
  @Autowired
  TokenProvider tokenProvider;
  @Autowired
  TerritoryRepository territoryRepository;
  @Autowired
  private ServiceRepository serviceRepository;
  @Autowired
  private MockMvc mvc;

  @Value("${default.territory.name}")
  private String defaultTerritoryName;

  private Cartography cartography;
  private Service service;

  @Before
  public void init() {
    Territory territory = territoryRepository.findOneByName(defaultTerritoryName).get();

    service = new Service();
    service.setName("Service");
    service.setServiceURL("");
    service.setType("");
    serviceRepository.saveAll(Arrays.asList(service));

    ArrayList<Cartography> cartosToCreate = new ArrayList<>();
    ArrayList<CartographyAvailability> availabilitesToCreate =
        new ArrayList<>();

    cartography = new Cartography();
    cartography.setName(CARTOGRAPHY_NAME);
    cartography.setLayers("");
    cartography.setApplyFilterToGetMap(false);
    cartography.setApplyFilterToGetFeatureInfo(false);
    cartography.setApplyFilterToSpatialSelection(false);
    cartography.setService(service);
    cartosToCreate.add(cartography);

    Cartography cartographyWithAvailabilities = new Cartography();
    cartographyWithAvailabilities.setName("Cartography with availabilities");
    cartographyWithAvailabilities.setLayers("");
    cartographyWithAvailabilities.setApplyFilterToGetMap(false);
    cartographyWithAvailabilities.setApplyFilterToGetFeatureInfo(false);
    cartographyWithAvailabilities.setApplyFilterToSpatialSelection(false);
    cartographyWithAvailabilities.setService(service);
    cartosToCreate.add(cartographyWithAvailabilities);

    cartographyRepository.saveAll(cartosToCreate);
    CartographyAvailability cartographyAvailability1 = new CartographyAvailability();
    cartographyAvailability1.setCartography(cartographyWithAvailabilities);
    cartographyAvailability1.setTerritory(territory);
    cartographyAvailability1.setCreatedDate(new Date());
    availabilitesToCreate.add(cartographyAvailability1);

    cartographyAvailabilityRepository.saveAll(availabilitesToCreate);
  }

  @Test
  @WithMockUser(username = ADMIN_USERNAME)
  public void postCartography() throws Exception {

    String content = new JSONObject()
        .put("name", CARTOGRAPHY_NAME)
        .put("layers", "")
        .put("applyFilterToGetMap", false)
        .put("applyFilterToSpatialSelection", false)
        .put("applyFilterToGetFeatureInfo", false)
        .put("service", "http://localhost/api/services/" + service.getId())
        .toString();

    String location = mvc.perform(post(CARTOGRAPHY_URI)
        .contentType(MediaType.APPLICATION_JSON)
        .content(content)
    ).andExpect(status().isCreated())
        .andReturn().getResponse().getHeader("Location");

    assertThat(location, notNullValue());

    mvc.perform(get(location))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaTypes.HAL_JSON))
        .andExpect(jsonPath("$.name", equalTo(CARTOGRAPHY_NAME)));
  }

  @Test
  public void getCartographiesAsPublic() throws Exception {
    mvc.perform(get(CARTOGRAPHY_URI))
        .andDo(print())
        .andExpect(status().isOk());
  }

  @Test
  public void postCartographyAsPublicUserFails() throws Exception {
    mvc.perform(post(CARTOGRAPHY_URI)
        .contentType(MediaType.APPLICATION_JSON)
        .content(TestUtils.asJsonString(cartography)))
        .andDo(print()).andExpect(status().is4xxClientError()).andReturn();
  }

}
