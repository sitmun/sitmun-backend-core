package org.sitmun.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.infrastructure.persistence.type.i18n.I18nTestConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@AutoConfigureTestDatabase
@TestPropertySource(properties = {
    "spring.liquibase.enabled=false",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@DisplayName("Service Repository WMS search test")
class ServiceRepositoryWmsSearchTest {

  @Autowired private ServiceRepository serviceRepository;

  @Test
  @DisplayName("findWms returns only WMS services")
  void findWmsReturnsOnlyWmsServices() {
    serviceRepository.save(Service.builder().name("WMS Mountains").type("WMS").serviceURL("https://example.com/wms").blocked(false).build());
    serviceRepository.save(Service.builder().name("WMTS Hills").type("WMTS").serviceURL("https://example.com/wmts").blocked(false).build());
    serviceRepository.save(Service.builder().name("WFS Rivers").type("WFS").serviceURL("https://example.com/wfs").blocked(false).build());

    List<Service> services = serviceRepository.findWms();

    assertThat(services).extracting(Service::getName).contains("WMS Mountains").doesNotContain("WMTS Hills", "WFS Rivers");
    assertThat(services).extracting(Service::getType).containsOnly("WMS");
  }

  @TestConfiguration
  @Import(I18nTestConfiguration.class)
  static class Configuration {}
}
