package org.sitmun.domain.service.parameter;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.domain.service.Service;
import org.sitmun.infrastructure.persistence.exception.BusinessRuleException;

@DisplayName("ServiceParameterEventHandler")
class ServiceParameterEventHandlerTest {

  private final ServiceParameterEventHandler handler = new ServiceParameterEventHandler();

  @Test
  @DisplayName("Rejects INFO on a WFS service")
  void rejectsInfoOnWfsService() {
    Service service = Service.builder().blocked(false).type("WFS").build();
    ServiceParameter parameter =
        ServiceParameter.builder().name("INFO_FORMAT").type("INFO").service(service).build();

    assertThatThrownBy(() -> handler.handleBeforeCreate(parameter))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  @DisplayName("Allows GETMAP on a WMS service")
  void allowsGetMapOnWmsService() {
    Service service = Service.builder().blocked(false).type("WMS").build();
    ServiceParameter parameter =
        ServiceParameter.builder()
            .name("FORMAT")
            .type("GETMAP")
            .value("image/png")
            .service(service)
            .build();

    handler.handleBeforeCreate(parameter);
  }
}
