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
    TerritoryType type = new TerritoryType.Builder().build();
    type.setName("tipo Territorio 1");
    territoryTypeRepository.save(type);

    territory =  Territory.builder()
        .setName("Admin")
        .setBlocked(false)
        .setTerritorialAuthorityEmail("email@email.org")
        .setCreatedDate(new Date())
        .setTerritorialAuthorityName("Test")
        .setType(type)
        .build();
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

    assertThat(territoryRepository.findById(territory.getId())).isNotNull();
  }
}
