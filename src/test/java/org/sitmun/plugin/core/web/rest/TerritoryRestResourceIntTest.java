package org.sitmun.plugin.core.web.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sitmun.plugin.core.config.RepositoryRestConfig;
import org.sitmun.plugin.core.domain.Territory;
import org.sitmun.plugin.core.repository.TerritoryRepository;
import org.sitmun.plugin.core.security.TokenProvider;
import org.sitmun.plugin.core.test.TestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

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
  private Territory territory;

  @Before
  public void init() {
    territory = Territory.builder().build();
    // Asignar atributos al territorio (municipio1)
    // Crear TerritoryType y asignar al territorio (tipo municipio)
    // Crear territorio 2 de tipo comarca
    // territoryService.createTerritory(territory1);
    // territoryService.createTerritory(territory2);
  }

  @Test
  public void getTerritoriesAsPublic() throws Exception {
    mvc.perform(get(TERRITORY_URI))
        .andDo(print())
        .andExpect(status().isOk());
  }

  @Test
  public void createTerritoriesAsPublicFails() throws Exception {
    mvc.perform(post(TERRITORY_URI)
        .contentType(MediaType.APPLICATION_JSON)
        .content(TestUtils.asJsonString(territory))
    ).andDo(print())
        .andExpect(status().is4xxClientError()).andReturn();

  }

  @Test
  public void setMemberAsPublicFails() {
    // TODO: Define territory1 (municipality) as member of territory2 (supramunicipality) as public user
    // fails is expected
  }

  @Test
  public void updateTerritoryAsPublicFails() {
    // TODO: Modify territory type
    // fail is expected
  }

  @Test
  public void deleteMemberAsPublicFails() {
    // TODO: Delete territory1 (municipality) as member of territory2 (supramunicipality) by an public user
    // fail is expected
  }

  @Test
  public void getTerritoriesAsTerritorialUser() {
    // TODO: get all territories as TerritorialUser
    // ok is expected
  }

  @Test
  public void createTerritoriesAsTerritorialUserFails() {
    // TODO: create a territorie as an TerritorialUser
    // fail is expected
  }

  @Test
  public void setMemberAsTerritorialUserFails() {
    // TODO: Define territory1 (municipality) as member of territory2 (supramunicipality) as TerritorialUser
    // fails is expected
  }

  @Test
  public void updateTerritoryAsTerritorialUserFails() {
    // TODO: Modify territory type
    // fail is expected
  }

  @Test
  public void deleteMemberAsTerritorialUserFails() {
    // TODO: Delete territory1 (municipality) as member of territory2 (supramunicipality) by a TerritorialUser
    // fail is expected
  }

  @Test
  public void getTerritoriesAsSitmunAdmin() {
    // TODO: get all territories as an admin user
    // ok is expected
  }

  @Test
  public void createTerritoriesAsSitmunAdmin() {
    // TODO; create a territorie as an sitmun admin user
    // ok is expected
  }

  @Test
  public void setMemberAsSitmunAdmin() {
    // TODO: Define territory1 (municipality) as member of territory2 (supramunicipality) as an sitmun admin user
    // --> It should appear territory2 as child and territory1 as parent
    // ok is expected
  }

  @Test
  public void updateTerritoryAsSitmunAdmin() {
    // TODO: Modify territory type
    // ok is expected --> Eliminar su listado de miembros si se cambia el tipo
  }

  @Test
  public void deleteMemberAsSitmunAdmin() {
    // TODO: Delete territory1 (municipality) as member of territory2 (supramunicipality) by an admin user
    // ok is expected
  }

  @Test
  public void getTerritoriesAsOrganizationAdmin() {
    // TODO: get all territories as an organization admin user
    // ok is expected
  }

  @Test
  public void createTerritoriesAsOrganizationAdminFails() {
    // TODO: create a territorie as an organization user
    // fail is expected
  }

  @Test
  public void setMemberAsOrganizationAdminFails() {
    // TODO: Define territory1 (municipality) as member of territory2 (supramunicipality) by an organization admin user (ADMIN DE ORGANIZACION)
    // fail is expected (no permission)
  }

  @Test
  public void updateTerritoryAsOrganizationAdminFails() {
    // TODO: Try to modify territory type by an organization admin user (ADMIN DE ORGANIZACION)
    // fail is expected (no permission)
  }

  @Test
  public void deleteMemberAsOrganizationAdminFails() {
    // TODO: Delete territory1 (municipality) as member of territory2 (supramunicipality) by an organization admin user (ADMIN DE ORGANIZACION)
    // fail is expected (no permission)
  }

  @TestConfiguration
  static class ContextConfiguration {
    @Bean
    public Validator validator() {
      return new LocalValidatorFactoryBean();
    }

    @Bean
    RepositoryRestConfigurer repositoryRestConfigurer() {
      return new RepositoryRestConfig(validator());
    }
  }

}
