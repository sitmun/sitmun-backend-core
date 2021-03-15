package org.sitmun.plugin.core.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sitmun.plugin.core.domain.Cartography;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@DataJpaTest
@ActiveProfiles("dev")
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

  private Cartography.CartographyBuilder cartographyBuilder() {
    return Cartography.builder()
      .name("Test")
      .createdDate(new Date())
      .order(0)
      .queryableFeatureAvailable(true)
      .queryableFeatureEnabled(true)
      .selectableFeatureEnabled(true)
      .thematic(true)
      .transparency(0);
  }
}

