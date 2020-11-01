package org.sitmun.plugin.core.repository;

import static org.assertj.core.api.Assertions.assertThat;


import java.math.BigInteger;
import java.util.Date;
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

  @Test
  public void saveCartography() {
    Cartography cartography = cartographyBuilder().build();
    assertThat(cartography.getId()).isNull();
    cartographyRepository.save(cartography);
    assertThat(cartography.getId()).isNotZero();
  }

  @Test
  public void findOneCartographyById() {
    Cartography cartography = cartographyBuilder().build();
    assertThat(cartography.getId()).isNull();
    cartographyRepository.save(cartography);
    assertThat(cartography.getId()).isNotZero();

    assertThat(cartographyRepository.findById(cartography.getId())).isNotNull();
  }

  private Cartography.Builder cartographyBuilder() {
    return Cartography.builder()
        .setName("Test")
        .setCreatedDate(new Date())
        .setOrder(BigInteger.ZERO)
        .setQueryableFeatureAvailable(true)
        .setQueryableFeatureEnabled(true)
        .setSelectableFeatureEnabled(true)
        .setThematic(true)
        .setTransparency(BigInteger.ZERO);
  }
}

