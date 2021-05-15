package org.sitmun.plugin.core.repository.rest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sitmun.plugin.core.domain.Territory;
import org.sitmun.plugin.core.repository.TerritoryRepository;
import org.sitmun.plugin.core.test.TestUtils;
import org.sitmun.plugin.core.test.URIConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.sitmun.plugin.core.test.TestConstants.SITMUN_ADMIN_USERNAME;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc

public class TerritoryRepositoryDataRestTest {

  @Autowired
  TerritoryRepository territoryRepository;
  @Autowired
  private MockMvc mvc;
  private Territory territory;

  @BeforeEach
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
    mvc.perform(get(URIConstants.TERRITORIES_URI)
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME)))
      .andExpect(status().isOk());
  }

  @Test
  public void createTerritoriesAsPublicFails() throws Exception {
    mvc.perform(post(URIConstants.TERRITORIES_URI)
      .contentType(MediaType.APPLICATION_JSON)
      .content(TestUtils.asJsonString(territory))
    ).andExpect(status().is4xxClientError()).andReturn();

  }

  @Test
  public void hasLinkToCartographyAvailability() throws Exception {
    mvc.perform(get(URIConstants.TERRITORIES_URI)
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME)))
      .andExpect(status().isOk());
  }

  @Test
  public void hasLinkToTaskAvailability() throws Exception {
    mvc.perform(get(URIConstants.TERRITORY_URI, 0)
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME)))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._links.taskAvailabilities").exists());
  }

  @Test
  public void hasLinkToCartographyAvailabilities() throws Exception {
    mvc.perform(get(URIConstants.TERRITORY_URI, 0)
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME)))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._links.cartographyAvailabilities").exists());
  }

  @Disabled
  public void setMemberAsPublicFails() {
    // TODO: Define territory1 (municipality) as member of territory2 (supramunicipality) as public user
    // fails is expected
  }

  @Disabled
  public void updateTerritoryAsPublicFails() {
    // TODO: Modify territory type
    // fail is expected
  }

  @Disabled
  public void deleteMemberAsPublicFails() {
    // TODO: Delete territory1 (municipality) as member of territory2 (supramunicipality) by an public user
    // fail is expected
  }

  @Disabled
  public void getTerritoriesAsTerritorialUser() {
    // TODO: get all territories as TerritorialUser
    // ok is expected
  }

  @Disabled
  public void createTerritoriesAsTerritorialUserFails() {
    // TODO: create a territorie as an TerritorialUser
    // fail is expected
  }

  @Disabled
  public void setMemberAsTerritorialUserFails() {
    // TODO: Define territory1 (municipality) as member of territory2 (supramunicipality) as TerritorialUser
    // fails is expected
  }

  @Disabled
  public void updateTerritoryAsTerritorialUserFails() {
    // TODO: Modify territory type
    // fail is expected
  }

  @Disabled
  public void deleteMemberAsTerritorialUserFails() {
    // TODO: Delete territory1 (municipality) as member of territory2 (supramunicipality) by a TerritorialUser
    // fail is expected
  }

  @Disabled
  public void getTerritoriesAsSitmunAdmin() {
    // TODO: get all territories as an admin user
    // ok is expected
  }

  @Disabled
  public void createTerritoriesAsSitmunAdmin() {
    // TODO; create a territorie as an sitmun admin user
    // ok is expected
  }

  @Disabled
  public void setMemberAsSitmunAdmin() {
    // TODO: Define territory1 (municipality) as member of territory2 (supramunicipality) as an sitmun admin user
    // --> It should appear territory2 as child and territory1 as parent
    // ok is expected
  }

  @Disabled
  public void updateTerritoryAsSitmunAdmin() {
    // TODO: Modify territory type
    // ok is expected --> Eliminar su listado de miembros si se cambia el tipo
  }

  @Disabled
  public void deleteMemberAsSitmunAdmin() {
    // TODO: Delete territory1 (municipality) as member of territory2 (supramunicipality) by an admin user
    // ok is expected
  }

  @Disabled
  public void getTerritoriesAsOrganizationAdmin() {
    // TODO: get all territories as an organization admin user
    // ok is expected
  }

  @Disabled
  public void createTerritoriesAsOrganizationAdminFails() {
    // TODO: create a territorie as an organization user
    // fail is expected
  }

  @Disabled
  public void setMemberAsOrganizationAdminFails() {
    // TODO: Define territory1 (municipality) as member of territory2 (supramunicipality) by an organization admin user (ADMIN DE ORGANIZACION)
    // fail is expected (no permission)
  }

  @Disabled
  public void updateTerritoryAsOrganizationAdminFails() {
    // TODO: Try to modify territory type by an organization admin user (ADMIN DE ORGANIZACION)
    // fail is expected (no permission)
  }

  @Disabled
  public void deleteMemberAsOrganizationAdminFails() {
    // TODO: Delete territory1 (municipality) as member of territory2 (supramunicipality) by an organization admin user (ADMIN DE ORGANIZACION)
    // fail is expected (no permission)
  }

}
