package org.sitmun.plugin.core.repository;

import static org.assertj.core.api.Assertions.assertThat;


import java.util.Date;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sitmun.plugin.core.domain.Territory;
import org.sitmun.plugin.core.domain.TerritoryType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
public class TerritoryRepositoryTest {

  @Autowired
  private TerritoryRepository territoryRepository;

  @Autowired
  private TerritoryTypeRepository territoryTypeRepository;

  private Territory territory;

  @Before
  public void init() {
    TerritoryType type = new TerritoryType();
    type.setName("tipo Territorio 1");
    territoryTypeRepository.save(type);

    territory = new Territory();
    territory.setName("Admin");
    territory.setScope(null);
    territory.setBlocked(false);
    territory.setAddress(null);
    territory.setEmail("email@email.org");
    territory.setExt(null);
    territory.setCreatedDate(new Date());
    territory.setLogo(null);
    territory.setMembers(null);
    territory.setOrganizationName("Test");
    territory.setComments(null);
    territory.setType(type);
  }

  @Test
  public void saveTerritory() {
    assertThat(territory.getId()).isNull();
    territoryRepository.save(territory);
    assertThat(territory.getId()).isNotZero();
  }

  @Test
  public void findOneTerritoryById() {
    assertThat(territory.getId()).isNull();
    territoryRepository.save(territory);
    assertThat(territory.getId()).isNotZero();

    assertThat(territoryRepository.findOne(territory.getId())).isNotNull();
  }
}
