package org.sitmun.authorization.client.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.authorization.client.dto.CartographyDto;
import org.sitmun.authorization.client.dto.ProfileDto;
import org.sitmun.authorization.client.dto.ServiceDto;
import org.sitmun.authorization.client.service.Profile;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.cartography.Cartography;
import org.sitmun.domain.cartography.parameter.CartographyServiceParameter;
import org.sitmun.domain.service.Service;
import org.sitmun.domain.service.parameter.ServiceParameter;
import org.sitmun.domain.territory.Territory;

@DisplayName("ProfileMapper parameter scope")
class ProfileMapperParameterScopeTest {

  private final ProfileMapper mapper =
      new ProfileMapper() {
        @Override
        public ProfileDto map(Profile profile, Application application, Territory territory) {
          return null;
        }
      };

  @Test
  @DisplayName("A WMTS service publishes matrixSet and format and omits INFO and GETMAP")
  void wmtsServicePublishesTileSettingsOnly() {
    Service service =
        Service.builder()
            .id(8)
            .blocked(false)
            .type("WMTS")
            .name("Tiles")
            .serviceURL("https://example.test/wmts")
            .parameters(
                new HashSet<>(
                    List.of(
                        ServiceParameter.builder()
                            .id(1)
                            .name("matrixSet")
                            .value("EPSG:25831")
                            .type("WMTS")
                            .build(),
                        ServiceParameter.builder()
                            .id(2)
                            .name("format")
                            .value("image/png")
                            .type("WMTS")
                            .build(),
                        ServiceParameter.builder()
                            .id(3)
                            .name("INFO_FORMAT")
                            .value("text/plain")
                            .type("INFO")
                            .build(),
                        ServiceParameter.builder()
                            .id(4)
                            .name("FORMAT")
                            .value("image/jpeg")
                            .type("GETMAP")
                            .build())))
            .build();

    ServiceDto dto = mapper.map(service);

    assertThat(dto.getParameters())
        .containsEntry("matrixSet", "EPSG:25831")
        .containsEntry("format", "image/png");
    assertThat(dto.getParameters()).doesNotContainKey("INFO_FORMAT").doesNotContainKey("FORMAT");
  }

  @Test
  @DisplayName("A proxied layer publishes protocol parameters and omits INFO")
  void layerPublishesProtocolParametersOnly() {
    Service service =
        Service.builder().id(2).blocked(false).type("WMS").isProxied(true).name("Roads").build();
    Cartography cartography =
        Cartography.builder()
            .id(9)
            .name("Roads")
            .layers(List.of("roads"))
            .service(service)
            .serviceParameters(
                new HashSet<>(
                    List.of(
                        CartographyServiceParameter.builder()
                            .id(1)
                            .name("CQL_FILTER")
                            .value("IN(1)")
                            .type("WMS")
                            .build(),
                        CartographyServiceParameter.builder()
                            .id(2)
                            .name("INFO_FORMAT")
                            .value("application/json")
                            .type("INFO")
                            .build())))
            .build();

    CartographyDto dto = mapper.map(cartography);

    assertThat(dto.getParameters()).containsEntry("CQL_FILTER", "IN(1)");
    assertThat(dto.getParameters()).doesNotContainKey("INFO_FORMAT");
  }
}
