package org.sitmun.plugin.core.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sitmun.plugin.core.domain.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@DataJpaTest
@ActiveProfiles("dev")
public class ServiceRepositoryTest {

  @Autowired
  private ServiceRepository serviceRepository;

  private Service service;

  @BeforeEach
  public void init() {
    service = Service.builder().build();
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

    assertThat(serviceRepository.findById(service.getId())).isNotNull();
  }

}
