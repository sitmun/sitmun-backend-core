package org.sitmun.domain.service.usage;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("Usage operation names")
class UsageOperationTest {

  @ParameterizedTest
  @CsvSource({
    "GetMap, GetMap",
    "getmap, GetMap",
    "GetTile, GetTile",
    "GetFeatureInfo, GetFeatureInfo",
    "GetCapabilities, GetCapabilities",
    "Other, Other",
    "other, Other",
    "ViewerConfig, ViewerConfig",
    "GetLegendGraphic, GetLegendGraphic",
    "getlegendgraphic, getlegendgraphic"
  })
  @DisplayName("A real request name is stored as itself and a known name keeps its spelling")
  void canonicalKeepsARealName(String raw, String stored) {
    assertThat(UsageOperation.canonical(raw)).isEqualTo(stored);
  }

  @Test
  @DisplayName("A request name longer than the operation column is cut to 32 characters")
  void longNameFitsTheOperationColumn() {
    String raw = "abcdefghijklmnopqrstuvwxyz0123456789";
    assertThat(raw).hasSize(36);
    assertThat(UsageOperation.canonical(raw)).isEqualTo("abcdefghijklmnopqrstuvwxyz012345");
  }

  @Test
  @DisplayName("A missing request name stays Other")
  void missingNameStaysOther() {
    assertThat(UsageOperation.canonical(null)).isEqualTo(UsageOperation.OTHER);
    assertThat(UsageOperation.canonical("  ")).isEqualTo(UsageOperation.OTHER);
    assertThat(UsageOperation.canonical("")).isEqualTo(UsageOperation.OTHER);
  }
}
