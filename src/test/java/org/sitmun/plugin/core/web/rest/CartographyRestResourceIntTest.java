package org.sitmun.plugin.core.web.rest;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


import java.util.ArrayList;
import java.util.Date;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sitmun.plugin.core.domain.Cartography;
import org.sitmun.plugin.core.domain.CartographyAvailability;
import org.sitmun.plugin.core.domain.Territory;
import org.sitmun.plugin.core.repository.CartographyAvailabilityRepository;
import org.sitmun.plugin.core.repository.CartographyRepository;
import org.sitmun.plugin.core.repository.TerritoryRepository;
import org.sitmun.plugin.core.security.TokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
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
  private MockMvc mvc;
  private Cartography cartography;
  private Territory territory;
  @Value("${default.territory.name}")
  private String defaultTerritoryName;
  private Cartography cartographyWithAvailabilities;

  @Before
  public void init() {
    territory = territoryRepository.findOneByName(defaultTerritoryName).get();
    ArrayList<Cartography> cartosToCreate = new ArrayList<Cartography>();
    ArrayList<CartographyAvailability> availabilitesToCreate = new ArrayList<CartographyAvailability>();
    cartography = new Cartography();
    cartography.setName(CARTOGRAPHY_NAME);
    cartosToCreate.add(cartography);
    cartographyWithAvailabilities = new Cartography();
    cartographyWithAvailabilities.setName("Cartography with availabilities");
    cartosToCreate.add(cartographyWithAvailabilities);
    cartographyRepository.save(cartosToCreate);
    CartographyAvailability cartographyAvailability1 = new CartographyAvailability();
    cartographyAvailability1.setCartography(cartographyWithAvailabilities);
    cartographyAvailability1.setTerritory(territory);
    cartographyAvailability1.setCreatedDate(new Date());
    availabilitesToCreate.add(cartographyAvailability1);

    cartographyAvailabilityRepository.save(availabilitesToCreate);
  }

  @Test
  @WithMockUser(username = ADMIN_USERNAME)
  public void postCartography() throws Exception {

    String uri = mvc.perform(post(CARTOGRAPHY_URI)
                                 // .header(HEADER_STRING, TOKEN_PREFIX + token)
                                 .contentType(MediaType.APPLICATION_JSON_UTF8).content(Util.convertObjectToJsonBytes(cartography)))
                     .andExpect(status().isCreated()).andReturn().getResponse().getHeader("Location");

    mvc.perform(get(uri)
        // .header(HEADER_STRING, TOKEN_PREFIX + token)
    ).andExpect(status().isOk()).andExpect(content().contentType(Util.APPLICATION_HAL_JSON_UTF8))
        .andExpect(jsonPath("$.name", equalTo(CARTOGRAPHY_NAME)));
  }

  @Test
  public void getCartographiesAsPublic() throws Exception {
    mvc.perform(get(CARTOGRAPHY_URI)).andDo(print()).andExpect(status().isOk());
  }

  @Test
  public void postCartographyAsPublicUserFails() throws Exception {

    mvc.perform(post(CARTOGRAPHY_URI)
                    // .header(HEADER_STRING, TOKEN_PREFIX + token)
                    .contentType(MediaType.APPLICATION_JSON_UTF8).content(Util.convertObjectToJsonBytes(cartography)))
        .andDo(print()).andExpect(status().is4xxClientError()).andReturn();
  }

  @After
  public void cleanup() {
    cartographyRepository.deleteAll();
    cartographyAvailabilityRepository.deleteAll();
  }

}
