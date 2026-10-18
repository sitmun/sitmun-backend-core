package org.sitmun.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.domain.cartography.parameter.CartographyServiceParameter;
import org.sitmun.domain.cartography.parameter.CartographyServiceParameterRepository;
import org.sitmun.domain.service.parameter.ServiceParameter;
import org.sitmun.domain.service.parameter.ServiceParameterRepository;
import org.sitmun.infrastructure.persistence.exception.BusinessRuleException;

@DisplayName("ServiceEventHandler")
class ServiceEventHandlerTest {

  private final CartographyServiceParameterRepository parameters =
      mock(CartographyServiceParameterRepository.class);
  private final ServiceParameterRepository serviceParameters =
      mock(ServiceParameterRepository.class);
  private final ServiceEventHandler handler =
      new ServiceEventHandler(parameters, serviceParameters);

  @Test
  @DisplayName("Keeps stored password when PUT password is null")
  void keepsStoredPasswordOnNullPut() {
    Service service =
        Service.builder().blocked(false).password("kept").authenticationMode("None").build();
    service.postLoad();
    service.setPassword(null);

    handler.handleBeforeSave(service);

    assertThat(service.getPassword()).isEqualTo("kept");
  }

  @Test
  @DisplayName("Clears password when PUT password is empty")
  void clearsPasswordOnEmptyPut() {
    Service service =
        Service.builder().blocked(false).password("secret").authenticationMode("None").build();
    service.postLoad();
    service.setPassword("");

    handler.handleBeforeSave(service);

    assertThat(service.getPassword()).isNull();
  }

  @Test
  @DisplayName("Forces proxied when authentication is not None")
  void forcesProxiedWhenAuthenticated() {
    Service service =
        Service.builder()
            .blocked(false)
            .isProxied(false)
            .authenticationMode("HTTP Basic authentication")
            .build();

    handler.handleBeforeSave(service);

    assertThat(service.getIsProxied()).isTrue();
  }

  @Test
  @DisplayName("Does not force proxied off when authentication is None")
  void doesNotForceProxiedOffWhenNone() {
    Service service =
        Service.builder().blocked(false).isProxied(true).authenticationMode("None").build();

    handler.handleBeforeSave(service);

    assertThat(service.getIsProxied()).isTrue();
  }

  @Test
  @DisplayName("Create treats empty password as null and forces proxied for Basic")
  void createClearsEmptyPasswordAndForcesProxied() {
    Service service =
        Service.builder()
            .blocked(false)
            .isProxied(false)
            .authenticationMode("HTTP Basic authentication")
            .password("")
            .build();

    handler.handleBeforeCreate(service);

    assertThat(service.getPassword()).isNull();
    assertThat(service.getIsProxied()).isTrue();
  }

  @Test
  @DisplayName("Rejects turning proxy off while a layer still has service parameters")
  void rejectsUnproxiedServiceThatStillHasLayerParameters() {
    when(parameters.existsByCartography_Service_Id(4)).thenReturn(true);
    Service service =
        Service.builder().id(4).blocked(false).isProxied(false).authenticationMode("None").build();

    assertThatThrownBy(() -> handler.handleBeforeSave(service))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  @DisplayName("Rejects a WFS to WMS change while a WFS parameter remains")
  void rejectsWfsToWmsWhileWfsParameterRemains() {
    when(serviceParameters.findByService_Id(4))
        .thenReturn(List.of(ServiceParameter.builder().type("WFS").name("CQL_FILTER").build()));
    Service service =
        Service.builder()
            .id(4)
            .blocked(false)
            .type("WMS")
            .isProxied(true)
            .authenticationMode("None")
            .build();

    assertThatThrownBy(() -> handler.handleBeforeSave(service))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  @DisplayName("Rejects a WMS to WFS change while a layer still has a WMS parameter")
  void rejectsWmsToWfsWhileLayerParameterRemains() {
    when(parameters.findByCartography_Service_Id(4))
        .thenReturn(
            List.of(
                CartographyServiceParameter.builder().type("INFO").name("INFO_FORMAT").build()));
    Service service =
        Service.builder()
            .id(4)
            .blocked(false)
            .type("WFS")
            .isProxied(true)
            .authenticationMode("None")
            .build();

    assertThatThrownBy(() -> handler.handleBeforeSave(service))
        .isInstanceOf(BusinessRuleException.class);
  }
}
