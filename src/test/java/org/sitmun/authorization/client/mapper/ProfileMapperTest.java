package org.sitmun.authorization.client.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.authorization.client.dto.CartographyDto;
import org.sitmun.authorization.client.dto.NodeDto;
import org.sitmun.domain.cartography.Cartography;
import org.sitmun.domain.cartography.parameter.CartographyParameter;
import org.sitmun.domain.cartography.parameter.FeatureInfoFormatOptions;
import org.sitmun.domain.service.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@DisplayName("ProfileMapper Cartography mapping")
class ProfileMapperTest {

  @Autowired private ProfileMapper profileMapper;

  private final ObjectMapper objectMapper = new ObjectMapper();

  private Cartography cartography(Integer minScale, Integer maxScale) {
    return cartography(minScale, maxScale, null, null, null);
  }

  private Cartography cartography(Integer minScale, Integer maxScale, Integer transparency) {
    return cartography(minScale, maxScale, transparency, null, null);
  }

  private Cartography cartography(
      Integer minScale,
      Integer maxScale,
      Integer transparency,
      String metadataURL,
      String datasetURL) {
    return cartography(minScale, maxScale, transparency, metadataURL, datasetURL, null);
  }

  private Cartography cartography(
      Integer minScale,
      Integer maxScale,
      Integer transparency,
      String metadataURL,
      String datasetURL,
      String description) {
    return Cartography.builder()
        .id(42)
        .name("Test layer")
        .description(description)
        .layers(List.of("L1"))
        .minimumScale(minScale)
        .maximumScale(maxScale)
        .transparency(transparency)
        .metadataURL(metadataURL)
        .datasetURL(datasetURL)
        .service(Service.builder().id(99).build())
        .build();
  }

  @Test
  @DisplayName("Maps positive scales to profile denominators")
  void mapsPositiveScales() {
    CartographyDto dto = profileMapper.map(cartography(200, 50000));
    assertThat(dto.getMinScaleDenominator()).isEqualTo(200);
    assertThat(dto.getMaxScaleDenominator()).isEqualTo(50000);
  }

  @Test
  @DisplayName("Null scales become null DTO fields")
  void nullScalesBecomeNull() {
    CartographyDto dto = profileMapper.map(cartography(null, null));
    assertThat(dto.getMinScaleDenominator()).isNull();
    assertThat(dto.getMaxScaleDenominator()).isNull();
  }

  @Test
  @DisplayName("Zero scales normalize to null")
  void zeroScalesNormalizeToNull() {
    CartographyDto dto = profileMapper.map(cartography(0, 0));
    assertThat(dto.getMinScaleDenominator()).isNull();
    assertThat(dto.getMaxScaleDenominator()).isNull();
  }

  @Test
  @DisplayName("Negative scales normalize to null")
  void negativeScalesNormalizeToNull() {
    CartographyDto dto = profileMapper.map(cartography(-1, -100));
    assertThat(dto.getMinScaleDenominator()).isNull();
    assertThat(dto.getMaxScaleDenominator()).isNull();
  }

  @Test
  @DisplayName("A positive min or max is kept and the other side stays null")
  void oneSidedScale() {
    CartographyDto minOnly = profileMapper.map(cartography(100, null));
    assertThat(minOnly.getMinScaleDenominator()).isEqualTo(100);
    assertThat(minOnly.getMaxScaleDenominator()).isNull();

    CartographyDto maxOnly = profileMapper.map(cartography(null, 100));
    assertThat(maxOnly.getMinScaleDenominator()).isNull();
    assertThat(maxOnly.getMaxScaleDenominator()).isEqualTo(100);
  }

  @Test
  @DisplayName("Jackson omits null denominator keys")
  void jacksonOmitsNullDenominators() throws Exception {
    CartographyDto dto =
        CartographyDto.builder()
            .id("layer/1")
            .title("t")
            .layers(List.of("a"))
            .service("service/1")
            .minScaleDenominator(null)
            .maxScaleDenominator(null)
            .build();

    String json = objectMapper.writeValueAsString(dto);
    assertThat(json).doesNotContain("minScaleDenominator");
    assertThat(json).doesNotContain("maxScaleDenominator");
  }

  @Test
  @DisplayName("Jackson serializes positive denominators")
  void jacksonSerializesPositiveDenominators() throws Exception {
    CartographyDto dto =
        CartographyDto.builder()
            .id("layer/1")
            .title("t")
            .layers(List.of("a"))
            .service("service/1")
            .minScaleDenominator(500)
            .maxScaleDenominator(1000000)
            .build();

    String json = objectMapper.writeValueAsString(dto);
    assertThat(json).contains("\"minScaleDenominator\":500");
    assertThat(json).contains("\"maxScaleDenominator\":1000000");
  }

  @Test
  @DisplayName("Maps null entity transparency to null DTO field")
  void nullTransparencyBecomesNull() {
    CartographyDto dto = profileMapper.map(cartography(null, null, null));
    assertThat(dto.getTransparency()).isNull();
  }

  @Test
  @DisplayName("Maps zero transparency (max opacity) verbatim")
  void zeroTransparencyMaps() {
    CartographyDto dto = profileMapper.map(cartography(null, null, 0));
    assertThat(dto.getTransparency()).isEqualTo(0);
  }

  @Test
  @DisplayName("Maps maximum transparency verbatim")
  void hundredTransparencyMaps() {
    CartographyDto dto = profileMapper.map(cartography(null, null, 100));
    assertThat(dto.getTransparency()).isEqualTo(100);
  }

  @Test
  @DisplayName("Jackson omits null transparency key")
  void jacksonOmitsNullTransparency() throws Exception {
    CartographyDto dto =
        CartographyDto.builder()
            .id("layer/1")
            .title("t")
            .layers(List.of("a"))
            .service("service/1")
            .build();

    String json = objectMapper.writeValueAsString(dto);
    assertThat(json).doesNotContain("transparency");
  }

  @Test
  @DisplayName("Maps metadataURL and datasetURL when set")
  void mapsMetadataAndDatasetUrls() {
    CartographyDto dto =
        profileMapper.map(
            cartography(null, null, null, "https://example.com/md", "https://example.com/data"));
    assertThat(dto.getMetadataURL()).isEqualTo("https://example.com/md");
    assertThat(dto.getDatasetURL()).isEqualTo("https://example.com/data");
  }

  @Test
  @DisplayName("Jackson omits null metadataURL and datasetURL keys")
  void jacksonOmitsNullMetadataAndDatasetUrls() throws Exception {
    CartographyDto dto = profileMapper.map(cartography(null, null, null, null, null));
    String json = objectMapper.writeValueAsString(dto);
    assertThat(json).doesNotContain("metadataURL");
    assertThat(json).doesNotContain("datasetURL");
  }

  @Test
  @DisplayName("NodeDto carries optional tree node metadata and dataset URLs")
  void nodeDtoOptionalUrls() throws Exception {
    NodeDto dto =
        NodeDto.builder()
            .title("Folder")
            .metadataURL("https://example.com/md")
            .datasetURL("https://example.com/data.zip")
            .build();
    assertThat(dto.getMetadataURL()).isEqualTo("https://example.com/md");
    assertThat(dto.getDatasetURL()).isEqualTo("https://example.com/data.zip");
    String json = objectMapper.writeValueAsString(dto);
    assertThat(json).contains("\"metadataURL\":\"https://example.com/md\"");
    assertThat(json).contains("\"datasetURL\":\"https://example.com/data.zip\"");
  }

  @Test
  @DisplayName("Jackson serializes zero transparency")
  void jacksonSerializesZeroTransparency() throws Exception {
    CartographyDto dto =
        CartographyDto.builder()
            .id("layer/1")
            .title("t")
            .layers(List.of("a"))
            .service("service/1")
            .transparency(0)
            .build();

    String json = objectMapper.writeValueAsString(dto);
    assertThat(json).contains("\"transparency\":0");
  }

  @Test
  @DisplayName("Maps cartography description verbatim when set")
  void mapsDescriptionWhenSet() {
    CartographyDto dto =
        profileMapper.map(cartography(null, null, null, null, null, "An abstract description"));
    assertThat(dto.getDescription()).isEqualTo("An abstract description");
  }

  @Test
  @DisplayName("Null cartography description maps to null DTO field")
  void nullDescriptionBecomesNull() {
    CartographyDto dto = profileMapper.map(cartography(null, null, null, null, null, null));
    assertThat(dto.getDescription()).isNull();
  }

  @Test
  @DisplayName("Jackson omits null description key")
  void jacksonOmitsNullDescription() throws Exception {
    CartographyDto dto = profileMapper.map(cartography(null, null, null, null, null, null));
    String json = objectMapper.writeValueAsString(dto);
    assertThat(json).doesNotContain("description");
  }

  @Test
  @DisplayName("Jackson serializes non-null description")
  void jacksonSerializesDescription() throws Exception {
    CartographyDto dto =
        profileMapper.map(cartography(null, null, null, null, null, "Some abstract"));
    String json = objectMapper.writeValueAsString(dto);
    assertThat(json).contains("\"description\":\"Some abstract\"");
  }

  @Test
  @DisplayName("Publishes vector or raster from the linked service and omits a layer service type")
  void publishesLayerKindFromServiceType() throws Exception {
    CartographyDto pointLayer =
        profileMapper.map(
            cartographyWithService(
                "WFS",
                "POINT",
                geometryParameter("the_geom", "Point", true, "polygon"),
                textParameter("nom")));
    assertThat(pointLayer.getKind()).isEqualTo("vector");
    assertThat(pointLayer.getGeometryName()).isEqualTo("the_geom");
    assertThat(pointLayer.getPreferredGeometryType()).isNull();
    assertThat(pointLayer.getFeatureInfoFields())
        .extracting("name")
        .containsExactlyInAnyOrder("nom", "the_geom");
    assertThat(pointLayer.getFeatureInfoFields())
        .filteredOn(field -> "the_geom".equals(field.getName()))
        .extracting("format")
        .containsExactly("Geometry");
    assertThat(pointLayer.getFeatureInfoFields())
        .filteredOn(field -> "nom".equals(field.getName()))
        .extracting("format")
        .containsOnlyNulls();
    assertThat(objectMapper.writeValueAsString(pointLayer)).doesNotContain("\"geometry\":");

    CartographyDto generic =
        profileMapper.map(
            cartographyWithService(
                "WFS", "POINT", geometryParameter("shape", "Geometry", true, "polygon")));
    assertThat(generic.getGeometryName()).isEqualTo("shape");
    assertThat(generic.getPreferredGeometryType()).isEqualTo("polygon");
    String genericJson = objectMapper.writeValueAsString(generic);
    assertThat(genericJson).contains("\"kind\":\"vector\"");
    assertThat(genericJson).contains("\"geometryName\":\"shape\"");
    assertThat(genericJson).contains("\"preferredGeometryType\":\"polygon\"");

    CartographyDto codeListType =
        profileMapper.map(
            cartographyWithService(
                "WFS", "POINT", geometryParameter("shape", "Geometry", true, "POINT")));
    assertThat(codeListType.getPreferredGeometryType()).isNull();

    CartographyParameter first = geometryParameter("geom_a", "Point", false, null);
    first.setOrder(1);
    CartographyParameter last = geometryParameter("shape", "Point", false, null);
    last.setOrder(2);
    CartographyDto unmarked = profileMapper.map(cartographyWithService("WFS", null, first, last));
    assertThat(unmarked.getGeometryName()).isEqualTo("shape");
    assertThat(unmarked.getPreferredGeometryType()).isNull();
    assertThat(unmarked.getFeatureInfoFields())
        .filteredOn(field -> "shape".equals(field.getName()))
        .extracting("format")
        .containsExactly("Geometry");

    CartographyDto wms =
        profileMapper.map(
            cartographyWithService(
                "WMS", "LINE", geometryParameter("the_geom", "Point", true, null)));
    assertThat(wms.getKind()).isEqualTo("raster");
    assertThat(wms.getGeometryName()).isNull();
    assertThat(wms.getPreferredGeometryType()).isNull();

    CartographyDto wmts = profileMapper.map(cartographyWithService("WMTS", null));
    assertThat(wmts.getKind()).isEqualTo("raster");
    assertThat(wmts.getGeometryName()).isNull();
    assertThat(wmts.getPreferredGeometryType()).isNull();
  }

  private Cartography cartographyWithService(
      String serviceType, String geometryType, CartographyParameter... parameters) {
    return Cartography.builder()
        .id(42)
        .name("Test layer")
        .layers(List.of("L1"))
        .geometryType(geometryType)
        .parameters(Set.of(parameters))
        .service(Service.builder().id(99).type(serviceType).build())
        .build();
  }

  private static CartographyParameter geometryParameter(
      String name, String kind, boolean chosen, String preferredGeometryType) {
    FeatureInfoFormatOptions options = new FeatureInfoFormatOptions();
    options.setGeometryKind(kind);
    if (chosen) {
      options.setLayerGeometry(true);
    }
    options.setPreferredGeometryType(preferredGeometryType);
    return CartographyParameter.builder()
        .id(name.hashCode())
        .name(name)
        .value(name)
        .type("INFO")
        .options(options)
        .build();
  }

  private static CartographyParameter textParameter(String name) {
    return CartographyParameter.builder()
        .id(name.hashCode())
        .name(name)
        .value(name)
        .type("INFO")
        .build();
  }
}
