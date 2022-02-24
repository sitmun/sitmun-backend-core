package org.sitmun.constraints;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sitmun.domain.Cartography;
import org.sitmun.domain.CartographyAvailability;
import org.sitmun.domain.Service;
import org.sitmun.domain.Territory;
import org.sitmun.repository.CartographyAvailabilityRepository;
import org.sitmun.repository.CartographyRepository;
import org.sitmun.repository.ServiceRepository;
import org.sitmun.repository.TerritoryRepository;
import org.sitmun.test.Fixtures;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.sitmun.test.TestUtils.withMockSitmunAdmin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional

public class CodeListTest {

  private static final String CARTOGRAPHY_NAME = "Cartography Name";
  private static final String CARTOGRAPHY_URI = "http://localhost/api/cartographies";
  @Autowired
  CartographyRepository cartographyRepository;
  @Autowired
  CartographyAvailabilityRepository cartographyAvailabilityRepository;
  @Autowired
  TerritoryRepository territoryRepository;
  @Autowired
  private ServiceRepository serviceRepository;
  @Autowired
  private MockMvc mvc;

  private List<Cartography> cartographies;
  private Service service;
  private Territory territory;
  private CartographyAvailability cartographyAvailability;

  @BeforeEach
  public void init() {
    withMockSitmunAdmin(() -> {

      territory = Territory.builder()
        .name("Some territory")
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

      Cartography cartography = Cartography.builder()
        .name(CARTOGRAPHY_NAME)
        .layers(Collections.emptyList())
        .applyFilterToGetMap(false)
        .applyFilterToGetFeatureInfo(false)
        .applyFilterToSpatialSelection(false)
        .service(service)
        .availabilities(Collections.emptySet())
        .blocked(false)
        .build();
      cartographies.add(cartography);

      Cartography cartographyWithAvailabilities = Cartography.builder()
        .name("Cartography with availabilities")
        .layers(Collections.emptyList())
        .applyFilterToGetMap(false)
        .applyFilterToGetFeatureInfo(false)
        .applyFilterToSpatialSelection(false)
        .service(service)
        .availabilities(Collections.emptySet())
        .blocked(false)
        .build();

      cartographies.add(cartographyWithAvailabilities);

      cartographyRepository.saveAll(cartographies);

      cartographyAvailability = new CartographyAvailability();
      cartographyAvailability.setCartography(cartographyWithAvailabilities);
      cartographyAvailability.setTerritory(territory);
      cartographyAvailability.setCreatedDate(new Date());

      cartographyAvailabilityRepository.save(cartographyAvailability);
    });
  }

  @AfterEach
  public void cleanup() {
    withMockSitmunAdmin(() -> {
      cartographyAvailabilityRepository.delete(cartographyAvailability);
      cartographyRepository.deleteAll(cartographies);
      serviceRepository.delete(service);
      territoryRepository.delete(territory);
    });
  }

  @Test
  @Disabled("Potential freeze of active connections. @Transactional may be required in REST controllers.")
  public void passIfCodeListValueIsValid() throws Exception {

    String content = new JSONObject()
      .put("name", CARTOGRAPHY_NAME)
      .put("layers", new JSONArray())
      .put("queryableFeatureAvailable", false)
      .put("queryableFeatureEnabled", false)
      .put("legendType", "LINK")
      .put("service", "http://localhost/api/services/" + service.getId())
      .put("blocked", false)
      .toString();

    String location = mvc.perform(post(CARTOGRAPHY_URI)
      .contentType(MediaType.APPLICATION_JSON)
      .content(content)
      .with(SecurityMockMvcRequestPostProcessors.user(Fixtures.admin()))
    ).andExpect(status().isCreated())
      .andReturn().getResponse().getHeader("Location");

    assertThat(location, notNullValue());

    mvc.perform(get(location)
      .with(SecurityMockMvcRequestPostProcessors.user(Fixtures.admin()))
    )
      .andExpect(status().isOk())
      .andExpect(content().contentType(MediaTypes.HAL_JSON))
      .andExpect(jsonPath("$.name", equalTo(CARTOGRAPHY_NAME)));
  }

  @Test
  @Disabled("Potential freeze of active connections. @Transactional may be required in REST controllers.")
  public void failIfCodeListValueIsWrong() throws Exception {

    String content = new JSONObject()
      .put("name", CARTOGRAPHY_NAME)
      .put("layers", new JSONArray())
      .put("queryableFeatureAvailable", false)
      .put("queryableFeatureEnabled", false)
      .put("legendType", "WRONG VALUE")
      .put("service", "http://localhost/api/services/" + service.getId())
      .put("blocked", false)
      .toString();

    mvc.perform(post(CARTOGRAPHY_URI)
      .contentType(MediaType.APPLICATION_JSON)
      .content(content)
      .with(SecurityMockMvcRequestPostProcessors.user(Fixtures.admin()))
    ).andExpect(status().is4xxClientError())
      .andExpect(jsonPath("$.errors[0].property", equalTo("legendType")))
      .andExpect(jsonPath("$.errors[0].invalidValue", equalTo("WRONG VALUE")));
  }
}
