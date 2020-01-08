package org.sitmun.plugin.core.repository;

import static org.assertj.core.api.Assertions.assertThat;


import java.math.BigInteger;
import java.util.Date;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sitmun.plugin.core.domain.Cartography;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
public class CartographyRepositoryTest {

  @Autowired
  private CartographyRepository cartographyRepository;

  private Cartography cartography;

  /**
   *
   */
  @Before
  public void init() {
    cartography = new Cartography();
    cartography.setName("Test");
    cartography.setLayers(null);
    cartography.setSelectionLayer(null);
    cartography.setConnection(null);
    cartography.setAvailabilities(null);
    cartography.setEditable(true);
    cartography.setMaximumScale(null);
    cartography.setMinimumScale(null);
    cartography.setCreatedDate(new Date());
    cartography.setOrder(BigInteger.ZERO);
    cartography.setQueryable(true);
    cartography.setQueryAct(true);
    cartography.setQueryLay(null);
    cartography.setSelectable(true);
    cartography.setService(null);
    cartography.setSelectionService(null);
    cartography.setThemeable(true);
    cartography.setLegendTip(null);
    cartography.setType(null);
    cartography.setGeometryType(null);
    cartography.setTransparency(BigInteger.ZERO);
    cartography.setLegendUrl(null);
    cartography.setMetadataUrl(null);
    cartography.setVisible(true);

  }

  @Test
  public void saveCartography() {
    assertThat(cartography.getId()).isNull();
    cartographyRepository.save(cartography);
    assertThat(cartography.getId()).isNotZero();
  }

  @Test
  public void findOneCartographyById() {
    assertThat(cartography.getId()).isNull();
    cartographyRepository.save(cartography);
    assertThat(cartography.getId()).isNotZero();

    assertThat(cartographyRepository.findOne(cartography.getId())).isNotNull();
  }
}

