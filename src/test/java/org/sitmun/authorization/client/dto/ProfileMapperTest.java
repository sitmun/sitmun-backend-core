package org.sitmun.authorization.client.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.domain.cartography.Cartography;
import org.sitmun.domain.service.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@DisplayName("ProfileMapper Cartography mapping")
class ProfileMapperTest {

  @Autowired private ProfileMapper profileMapper;

  private final ObjectMapper objectMapper = new ObjectMapper();

  private Cartography cartography(Integer minScale, Integer maxScale) {
    return cartography(minScale, maxScale, null);
  }

  private Cartography cartography(Integer minScale, Integer maxScale, Integer transparency) {
    return Cartography.builder()
        .id(42)
        .name("Test layer")
        .layers(List.of("L1"))
        .minimumScale(minScale)
        .maximumScale(maxScale)
        .transparency(transparency)
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
  @DisplayName("One-sided positive scale maps correctly")
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
}
