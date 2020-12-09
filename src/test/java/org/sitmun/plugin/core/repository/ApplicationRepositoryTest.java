package org.sitmun.plugin.core.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;


import java.math.BigInteger;
import java.time.Instant;
import java.util.Date;
import java.util.HashSet;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sitmun.plugin.core.domain.Application;
import org.sitmun.plugin.core.domain.ApplicationBackground;
import org.sitmun.plugin.core.domain.ApplicationParameter;
import org.sitmun.plugin.core.domain.Background;
import org.sitmun.plugin.core.domain.BackgroundMap;
import org.sitmun.plugin.core.domain.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
public class ApplicationRepositoryTest {

  @Autowired
  private ApplicationRepository applicationRepository;

  @Autowired
  private CartographyPermissionRepository cartographyPermissionRepository;

  @Autowired
  private BackgroundRepository backgroundRepository;

  private Application application;


  @Before
  public void init() {

    application = Application.builder()
        .setName("Test")
        .setCreatedDate(Date.from(Instant.now()))
        .setTrees(null)
        .setTreeAutoRefresh(true)
        .setScales(null)
        .setSituationMap(null)
        .setParameters(null)
        .setSrs(null)
        .setParameters(new HashSet<>())
        .setTheme(null)
        .setType(null)
        .setTitle("Test")
        .setAvailableRoles(new HashSet<>())
        .setBackgrounds(new HashSet<>())
        .build();

    Role rol = Role.builder()
        .setName("Rol 1")
        .build();
    application.getAvailableRoles().add(rol);

    BackgroundMap backgroundMap;
    backgroundMap = new BackgroundMap();
    backgroundMap.setName("Background map");
    cartographyPermissionRepository.save(backgroundMap);

    Background background = new Background();
    background.setActive(true);
    background.setDescription(null);
    background.setName("fondo");
    background.setCartographyGroup(backgroundMap);
    background.setCreatedDate(new Date());
    backgroundRepository.save(background);

    ApplicationBackground applicationBackground = new ApplicationBackground();
    applicationBackground.setApplication(application);
    applicationBackground.setBackground(background);
    applicationBackground.setOrder(BigInteger.ONE);
    application.getBackgrounds().add(applicationBackground);

    ApplicationParameter parameter = new ApplicationParameter();
    parameter.setApplication(application);
    parameter.setName("param1");
    parameter.setType("tipo1");
    parameter.setValue("valor1");
    application.getParameters().add(parameter);
  }

  @Test
  public void saveApplication() {
    assertThat(application.getId()).isNull();
    applicationRepository.save(application);
    assertThat(application.getId()).isNotZero();
  }

  @Test
  public void findOneApplicationById() {
    assertThat(application.getId()).isNull();
    applicationRepository.save(application);
    assertThat(application.getId()).isNotZero();

    application = applicationRepository.findById(application.getId()).get();
    SoftAssertions softly = new SoftAssertions();
    softly.assertThat(application.getAvailableRoles()).isNotEmpty();
    softly.assertThat(application.getBackgrounds()).isNotEmpty();
    softly.assertThat(application.getParameters()).isNotEmpty();
    softly.assertAll();
  }

  @Test
  public void deleteApplicationById() {
    assertThat(application.getId()).isNull();
    applicationRepository.save(application);
    assumeThat(application.getId()).isNotZero();

    BigInteger id = application.getId();
    applicationRepository.delete(application);
    assertThat(applicationRepository.findById(id)).isEmpty();
  }
}
