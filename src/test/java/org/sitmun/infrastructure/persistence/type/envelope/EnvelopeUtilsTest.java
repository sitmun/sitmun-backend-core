package org.sitmun.infrastructure.persistence.type.envelope;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.infrastructure.persistence.type.point.Point;

@DisplayName("EnvelopeUtils")
class EnvelopeUtilsTest {

  @Test
  @DisplayName("computeCenteredView returns null when extent is null")
  void nullExtent() {
    assertThat(EnvelopeUtils.computeCenteredView(null, Point.builder().x(1.0).y(2.0).build()))
        .isNull();
  }

  @Test
  @DisplayName("computeCenteredView returns extent when center is null")
  void nullCenter() {
    Envelope extent = envelope(100.0, 50.0, 200.0, 150.0);
    assertThat(EnvelopeUtils.computeCenteredView(extent, null)).isEqualTo(extent);
  }

  @Test
  @DisplayName("computeCenteredView returns extent when center coordinates are incomplete")
  void partialCenter() {
    Envelope extent = envelope(100.0, 50.0, 200.0, 150.0);
    Point incompleteCenter = Point.builder().x(150.0).build();
    assertThat(EnvelopeUtils.computeCenteredView(extent, incompleteCenter)).isEqualTo(extent);
  }

  @Test
  @DisplayName("computeCenteredView returns extent when center is (0, 0) from legacy data")
  void legacyZeroCenter() {
    Envelope extent = envelope(100.0, 50.0, 200.0, 150.0);
    Point legacyCenter = Point.builder().x(0.0).y(0.0).build();
    assertThat(EnvelopeUtils.computeCenteredView(extent, legacyCenter)).isEqualTo(extent);
  }

  @Test
  @DisplayName("computeCenteredView equals extent when center is at extent center")
  void centeredPoint() {
    Envelope extent = envelope(100.0, 50.0, 200.0, 150.0);
    Point center = Point.builder().x(150.0).y(100.0).build();
    Envelope computed = EnvelopeUtils.computeCenteredView(extent, center);
    assertThat(computed).isNotNull();
    assertThat(computed.getMinX()).isEqualTo(100.0);
    assertThat(computed.getMaxX()).isEqualTo(200.0);
    assertThat(computed.getMinY()).isEqualTo(50.0);
    assertThat(computed.getMaxY()).isEqualTo(150.0);
  }

  @Test
  @DisplayName("computeCenteredView expands to keep center in middle when point is offset")
  void offsetPoint() {
    Envelope extent = envelope(100.0, 50.0, 200.0, 150.0);
    Point center = Point.builder().x(120.0).y(70.0).build();
    Envelope computed = EnvelopeUtils.computeCenteredView(extent, center);
    assertThat(computed).isNotNull();
    double computedCenterX = (computed.getMinX() + computed.getMaxX()) / 2;
    double computedCenterY = (computed.getMinY() + computed.getMaxY()) / 2;
    assertThat(computedCenterX).isEqualTo(120.0);
    assertThat(computedCenterY).isEqualTo(70.0);
    assertThat(computed.getMinX()).isLessThanOrEqualTo(100.0);
    assertThat(computed.getMaxX()).isGreaterThanOrEqualTo(200.0);
    assertThat(computed.getMinY()).isLessThanOrEqualTo(50.0);
    assertThat(computed.getMaxY()).isGreaterThanOrEqualTo(150.0);
  }

  @Test
  @DisplayName("computeCenteredView for seed territory 1 adjusts minY by 1")
  void seedTerritory1() {
    Envelope extent = envelope(363487.0, 4561229.0, 481617.0, 4686464.0);
    Point center = Point.builder().x(422552.0).y(4623846.0).build();
    Envelope computed = EnvelopeUtils.computeCenteredView(extent, center);
    assertThat(computed).isNotNull();
    assertThat(computed.getMinX()).isEqualTo(363487.0);
    assertThat(computed.getMinY()).isEqualTo(4561228.0);
    assertThat(computed.getMaxX()).isEqualTo(481617.0);
    assertThat(computed.getMaxY()).isEqualTo(4686464.0);
  }

  @Test
  @DisplayName("computeCenteredView for seed territory 2 adjusts maxY by 1")
  void seedTerritory2() {
    Envelope extent = envelope(448046.0, 4603029.0, 458244.0, 4609234.0);
    Point center = Point.builder().x(453145.0).y(4606132.0).build();
    Envelope computed = EnvelopeUtils.computeCenteredView(extent, center);
    assertThat(computed).isNotNull();
    assertThat(computed.getMinX()).isEqualTo(448046.0);
    assertThat(computed.getMinY()).isEqualTo(4603029.0);
    assertThat(computed.getMaxX()).isEqualTo(458244.0);
    assertThat(computed.getMaxY()).isEqualTo(4609235.0);
  }

  private static Envelope envelope(double minX, double minY, double maxX, double maxY) {
    return Envelope.builder().minX(minX).minY(minY).maxX(maxX).maxY(maxY).build();
  }
}
