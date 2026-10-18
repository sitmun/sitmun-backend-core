package org.sitmun.domain.cartography.parameter;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.domain.cartography.Cartography;
import org.sitmun.domain.service.Service;
import org.sitmun.infrastructure.persistence.exception.BusinessRuleException;

@DisplayName("CartographyServiceParameterEventHandler")
class CartographyServiceParameterEventHandlerTest {

  private final CartographyServiceParameterEventHandler handler =
      new CartographyServiceParameterEventHandler();

  @Test
  @DisplayName("Rejects a parameter whose service is not proxied")
  void rejectsParameterOnDirectService() {
    Service service = Service.builder().blocked(false).isProxied(false).build();
    Cartography cartography =
        Cartography.builder()
            .name("roads")
            .layers(java.util.List.of("roads"))
            .service(service)
            .build();
    CartographyServiceParameter parameter =
        CartographyServiceParameter.builder()
            .name("CQL_FILTER")
            .value("IN(1)")
            .type("WMS")
            .cartography(cartography)
            .build();

    assertThatThrownBy(() -> handler.handleBeforeCreate(parameter))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  @DisplayName("Rejects a WMS parameter on a proxied WFS service")
  void rejectsParameterTypeTheServiceDoesNotAllow() {
    Service service = Service.builder().blocked(false).isProxied(true).type("WFS").build();
    Cartography cartography =
        Cartography.builder()
            .name("roads")
            .layers(java.util.List.of("roads"))
            .service(service)
            .build();
    CartographyServiceParameter parameter =
        CartographyServiceParameter.builder()
            .name("CQL_FILTER")
            .value("IN(1)")
            .type("WMS")
            .cartography(cartography)
            .build();

    assertThatThrownBy(() -> handler.handleBeforeCreate(parameter))
        .isInstanceOf(BusinessRuleException.class);
  }
}
