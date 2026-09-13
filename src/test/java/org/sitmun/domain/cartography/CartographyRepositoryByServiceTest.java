package org.sitmun.domain.cartography;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.domain.service.Service;
import org.sitmun.domain.service.ServiceRepository;
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
@DisplayName("Cartography Repository by-service test")
class CartographyRepositoryByServiceTest {

  @Autowired private CartographyRepository cartographyRepository;
  @Autowired private ServiceRepository serviceRepository;

  @Test
  @DisplayName("findByService returns only cartographies for selected service")
  void findByServiceReturnsOnlyCartographiesForSelectedService() {
    Service firstService = saveService("Service One");
    Service secondService = saveService("Service Two");
    Cartography first = saveCartography("Layer A", firstService);
    saveCartography("Layer B", secondService);

    List<Cartography> cartographies = cartographyRepository.findByService(firstService.getId());

    assertThat(cartographies).extracting(Cartography::getName).contains(first.getName()).doesNotContain("Layer B");
  }

  @Test
  @DisplayName("findByService returns empty list when service has no cartographies")
  void findByServiceReturnsEmptyListWhenServiceHasNoCartographies() {
    Service emptyService = saveService("Empty Service");

    assertThat(cartographyRepository.findByService(emptyService.getId())).isEmpty();
  }

  private Service saveService(String name) {
    return serviceRepository.save(Service.builder().name(name).type("WMS").serviceURL("https://example.com/" + name.replace(" ", "-")).blocked(false).build());
  }

  private Cartography saveCartography(String name, Service service) {
    return cartographyRepository.save(
        Cartography.builder()
            .type("I")
            .name(name)
            .layers(List.of("layer"))
            .queryableFeatureAvailable(false)
            .queryableFeatureEnabled(false)
            .service(service)
            .availabilities(java.util.Collections.emptySet())
            .blocked(false)
            .build());
  }

  @TestConfiguration
  @Import(I18nTestConfiguration.class)
  static class Configuration {}
}
