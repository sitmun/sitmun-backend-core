package org.sitmun.authorization.proxy.mbtiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sitmun.authorization.client.service.AuthorizationService;
import org.sitmun.authorization.client.service.Profile;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.cartography.Cartography;
import org.sitmun.domain.service.Service;
import org.sitmun.infrastructure.security.core.SecurityConstants;

@ExtendWith(MockitoExtension.class)
@DisplayName("MbtilesResourceAccessValidator")
class MbtilesResourceAccessValidatorTest {

  @Mock AuthorizationService authorizationService;
  @InjectMocks MbtilesResourceAccessValidator validator;

  private Application editionApp;
  private Service wmsService;
  private Cartography layer;

  @BeforeEach
  void setUp() {
    editionApp = new Application();
    editionApp.setId(10);
    editionApp.setType("ED");

    wmsService = new Service();
    wmsService.setId(5);
    wmsService.setServiceURL("https://tiles.example.com/wms");
    wmsService.setType("WMS");

    layer = new Cartography();
    layer.setId(7);
    layer.setService(wmsService);
    layer.setLayers(List.of("roads"));
  }

  @Test
  @DisplayName("Allows ED access and rebuilds canonical tile request from persisted URLs")
  void allowsAuthorizedEditionRequest() {
    when(authorizationService.findApplicationByUserApplicationAndTerritory("alice", 10, 20))
        .thenReturn(Optional.of(editionApp));
    when(authorizationService.createProfile(any()))
        .thenReturn(
            Optional.of(
                Profile.builder()
                    .application(editionApp)
                    .services(List.of(wmsService))
                    .layers(List.of(layer))
                    .build()));

    MbtilesProxyRequestDto request =
        new MbtilesProxyRequestDto(
            10,
            20,
            "estimate",
            null,
            new MbtilesBboxDto(0, 0, 1, 1),
            0,
            5,
            "EPSG:25831",
            List.of(new MbtilesServiceRefDto(5, List.of(7))));

    var outcome = validator.authorize(request, "alice", 1_700_000_000_000L);

    assertThat(outcome).isInstanceOf(MbtilesResourceAccessValidator.Outcome.Allowed.class);
    var response = ((MbtilesResourceAccessValidator.Outcome.Allowed) outcome).response();
    assertThat(response.tileRequest().mapServices())
        .singleElement()
        .satisfies(
            mapService -> {
              assertThat(mapService.url()).isEqualTo("https://tiles.example.com/wms");
              assertThat(mapService.layers()).containsExactly("roads");
              assertThat(mapService.type()).isEqualTo("WMS");
            });
    assertThat(response.action()).isEqualTo("estimate");
  }

  @Test
  @DisplayName("Denies public principal")
  void deniesPublicPrincipal() {
    MbtilesProxyRequestDto request =
        new MbtilesProxyRequestDto(
            10,
            20,
            "estimate",
            null,
            new MbtilesBboxDto(0, 0, 1, 1),
            0,
            5,
            "EPSG:25831",
            List.of(new MbtilesServiceRefDto(5, List.of(7))));

    var outcome = validator.authorize(request, SecurityConstants.PUBLIC_PRINCIPAL, 0);

    assertThat(outcome).isInstanceOf(MbtilesResourceAccessValidator.Outcome.Denied.class);
  }

  @Test
  @DisplayName("Denies non-ED application")
  void deniesNonEditionApplication() {
    Application internal = new Application();
    internal.setId(10);
    internal.setType("I");
    when(authorizationService.findApplicationByUserApplicationAndTerritory("alice", 10, 20))
        .thenReturn(Optional.of(internal));

    MbtilesProxyRequestDto request =
        new MbtilesProxyRequestDto(
            10,
            20,
            "create",
            null,
            new MbtilesBboxDto(0, 0, 1, 1),
            0,
            5,
            "EPSG:25831",
            List.of(new MbtilesServiceRefDto(5, List.of(7))));

    var outcome = validator.authorize(request, "alice", 1L);

    assertThat(outcome).isInstanceOf(MbtilesResourceAccessValidator.Outcome.Denied.class);
  }

  @Test
  @DisplayName("Denies unauthorized service or layer references")
  void deniesUnknownServiceLayer() {
    when(authorizationService.findApplicationByUserApplicationAndTerritory("alice", 10, 20))
        .thenReturn(Optional.of(editionApp));
    when(authorizationService.createProfile(any()))
        .thenReturn(
            Optional.of(
                Profile.builder()
                    .application(editionApp)
                    .services(List.of(wmsService))
                    .layers(List.of(layer))
                    .build()));

    MbtilesProxyRequestDto request =
        new MbtilesProxyRequestDto(
            10,
            20,
            "estimate",
            null,
            new MbtilesBboxDto(0, 0, 1, 1),
            0,
            5,
            "EPSG:25831",
            List.of(new MbtilesServiceRefDto(99, List.of(7))));

    var outcome = validator.authorize(request, "alice", 1L);

    assertThat(outcome).isInstanceOf(MbtilesResourceAccessValidator.Outcome.Denied.class);
  }
}
