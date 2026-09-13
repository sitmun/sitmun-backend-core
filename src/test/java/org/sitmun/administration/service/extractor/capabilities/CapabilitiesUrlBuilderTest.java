package org.sitmun.administration.service.extractor.capabilities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CapabilitiesUrlBuilder")
class CapabilitiesUrlBuilderTest {

  @Test
  @DisplayName("Adds request and service for a bare WMS URL")
  void addsGetCapabilitiesForBareWmsUrl() {
    String result = CapabilitiesUrlBuilder.build("https://example.com/wms", "WMS");

    assertThat(result).contains("request=GetCapabilities");
    assertThat(result).contains("service=WMS");
  }

  @Test
  @DisplayName("Keeps MapServer map= and does not add a second ?")
  void keepsMapServerQueryAndSingleQuestionMark() {
    String endpoint =
        "https://pcivil.icgc.cat/ogc/geoservei?map=/opt/idec/dades/pcivil/risc_quimic.map";

    String result = CapabilitiesUrlBuilder.build(endpoint, "WMS");

    assertThat(result.split("\\?", -1)).hasSize(2);
    assertThat(result).contains("map=/opt/idec/dades/pcivil/risc_quimic.map");
    assertThat(result).contains("request=GetCapabilities");
    assertThat(result).contains("service=WMS");
  }

  @Test
  @DisplayName("Leaves a URL that already has request=GetCapabilities unchanged")
  void leavesExistingGetCapabilitiesUnchanged() {
    String endpoint = "https://example.com/wms?request=GetCapabilities&service=WMS";

    assertThat(CapabilitiesUrlBuilder.build(endpoint, "WMS")).isEqualTo(endpoint);
  }

  @Test
  @DisplayName("Rejects non-WMS types")
  void rejectsNonWmsType() {
    assertThatThrownBy(() -> CapabilitiesUrlBuilder.build("https://example.com/wmts", "WMTS"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Unsupported service type for capabilities: WMTS");
  }

  @Test
  @DisplayName("Rejects blank url or type")
  void rejectsBlankUrlOrType() {
    assertThatThrownBy(() -> CapabilitiesUrlBuilder.build("  ", "WMS"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("url is required");
    assertThatThrownBy(() -> CapabilitiesUrlBuilder.build("https://example.com/wms", " "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("type is required");
  }
}
