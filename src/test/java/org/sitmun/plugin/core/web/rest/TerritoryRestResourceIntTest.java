package org.sitmun.plugin.core.web.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sitmun.plugin.core.domain.Territory;
import org.sitmun.plugin.core.repository.TerritoryRepository;
import org.sitmun.plugin.core.security.TokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class TerritoryRestResourceIntTest {

  private static final String USER_USERNAME = "admin";
  private static final String NEW_USER_USERNAME = "admin_new";

  private static final String USER_FIRSTNAME = "Admin";
  private static final String USER_CHANGEDFIRSTNAME = "Administrator";
  private static final String USER_LASTNAME = "Admin";
  private static final String USER_CHANGEDLASTNAME = "Territory 1";
  private static final Boolean USER_BLOCKED = false;
  private static final Boolean USER_ADMINISTRATOR = true;
  // private static final String DEFAULT_USER_URI =
  // "http://localhost/api/users/1";
  private static final String TERRITORY_URI = "http://localhost/api/territories";
  @Autowired
  TerritoryRepository territoryRepository;
  // @Autowired
  // TerritoryService territoryService;
  @Autowired
  TokenProvider tokenProvider;
  @Autowired
  private MockMvc mvc;
  private String token;
  private Territory territory;

  @Before
  public void init() {
    token = tokenProvider.createToken(USER_USERNAME);
    territory = new Territory();
    // Asignar atributos al territorio (municipio1)
    // Crear TerritoryType y asignar al territorio (tipo municipio)
    // Crear territorio 2 de tipo comarca
    // territoryService.createTerritory(territory1);
    // territoryService.createTerritory(territory2);

  }

  @After
  public void cleanup() {
    // territoryRepository.deleteAll();
  }

  @Test
  public void getTerritoriesAsPublic() throws Exception {
    mvc.perform(get(TERRITORY_URI)).andDo(print()).andExpect(status().isOk());
  }

  @Test
  public void createTerritoriesAsPublicFails() throws Exception {

    // create a territorie as an public user
    // fail is expected

    mvc.perform(post(TERRITORY_URI)
                    //.header(HEADER_STRING, TOKEN_PREFIX + token)
                    .contentType(MediaType.APPLICATION_JSON_UTF8)
                    .content(Util.convertObjectToJsonBytes(territory))
    ).andDo(print())
        .andExpect(status().is4xxClientError()).andReturn();

  }

  @Test
  public void setMemberAsPublicFails() {
    // TO DO
    // Define territory1 (municipality) as member of territory2 (supramunicipality)
    // as public user
    // fails is expected
  }

  @Test
  public void updateTerritoryAsPublicFails() {
    // TO DO
    // Modify territory type
    // fail is expected
  }

  @Test
  public void deleteMemberAsPublicFails() {
    // TO DO
    // Delete territory1 (municipality) as member of territory2 (supramunicipality)
    // by an public user
    // fail is expected
  }

  @Test
  public void getTerritoriesAsTerritorialUser() {
    // TO DO
    // get all territories as TerritorialUser
    // ok is expected
  }

  @Test
  public void createTerritoriesAsTerritorialUserFails() {
    // TO DO
    // create a territorie as an TerritorialUserr
    // fail is expected
  }

  @Test
  public void setMemberAsTerritorialUserFails() {
    // TO DO
    // Define territory1 (municipality) as member of territory2 (supramunicipality)
    // asTerritorialUser
    // fails is expected
  }

  @Test
  public void updateTerritoryAsTerritorialUserFails() {
    // TO DO
    // Modify territory type
    // fail is expected
  }

  @Test
  public void deleteMemberAsTerritorialUserFails() {
    // TO DO
    // Delete territory1 (municipality) as member of territory2 (supramunicipality)
    // by an TerritorialUser
    // fail is expected
  }

  @Test
  public void getTerritoriesAsSitmunAdmin() {
    // TO DO
    // get all territories as an admin user
    // ok is expected
  }

  @Test
  public void createTerritoriesAsSitmunAdmin() {
    // TO DO
    // create a territorie as an sitmun admin user
    // ok is expected
  }

  @Test
  public void setMemberAsSitmunAdmin() {
    // TO DO
    // Define territory1 (municipality) as member of territory2 (supramunicipality)
    // as an sitmun admin user
    // --> It should appear territory2 as child and territory1 as parent
    // ok is expected
  }

  @Test
  public void updateTerritoryAsSitmunAdmin() {
    // TO DO
    // Modify territory type
    // ok is expected --> Eliminar su listado de miembros si se cambia el tipo
  }

  @Test
  public void deleteMemberAsSitmunAdmin() {
    // TO DO
    // Delete territory1 (municipality) as member of territory2 (supramunicipality)
    // by an admin user
    // ok is expected
  }

  @Test
  public void getTerritoriesAsOrganizationAdmin() {
    // TO DO
    // get all territories as an organization admin user
    // ok is expected
  }

  @Test
  public void createTerritoriesAsOrganizationAdminFails() {
    // TO DO
    // create a territorie as an organization user
    // fail is expected
  }

  @Test
  public void setMemberAsOrganizationAdminFails() {
    // TO DO
    // Define territory1 (municipality) as member of territory2 (supramunicipality)
    // by an organization admin user (ADMIN DE ORGANIZACION)
    // fail is expected (no permission)
  }

  @Test
  public void updateTerritoryAsOrganizationAdminFails() {
    // TO DO
    // Try to modify territory type by an organization admin user (ADMIN DE
    // ORGANIZACION)
    // fail is expected (no permission)
  }

  @Test
  public void deleteMemberAsOrganizationAdminFails() {
    // TO DO
    // Delete territory1 (municipality) as member of territory2 (supramunicipality)
    // by an organization admin user (ADMIN DE ORGANIZACION)
    // fail is expected (no permission)
  }

}
