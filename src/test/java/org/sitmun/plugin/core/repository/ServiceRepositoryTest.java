package org.sitmun.plugin.core.repository;

import static org.assertj.core.api.Assertions.assertThat;


import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sitmun.plugin.core.domain.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
public class ServiceRepositoryTest {

  @Autowired
  private ServiceRepository serviceRepository;

  private Service service;

  @Before
  public void init() {
    service = new Service();

  }

  @Test
  public void saveService() {
    assertThat(service.getId()).isNull();
    serviceRepository.save(service);
    assertThat(service.getId()).isNotZero();
  }

  @Test
  public void findOneServiceById() {
    assertThat(service.getId()).isNull();
    serviceRepository.save(service);
    assertThat(service.getId()).isNotZero();

    assertThat(serviceRepository.findOne(service.getId())).isNotNull();
  }

}
