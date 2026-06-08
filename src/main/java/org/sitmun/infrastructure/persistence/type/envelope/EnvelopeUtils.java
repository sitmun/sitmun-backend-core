package org.sitmun.infrastructure.persistence.type.envelope;

import org.jspecify.annotations.Nullable;
import org.sitmun.infrastructure.persistence.type.point.Point;

/** Utilities for {@link Envelope} geometry. */
public final class EnvelopeUtils {

  private EnvelopeUtils() {}

  /**
   * Computes a centered view envelope that includes the full extent.
   *
   * <p>Returns the extent unchanged when the center is null, incomplete, or {@code (0, 0)} (legacy
   * data). Returns null when the extent is null.
   */
  public static @Nullable Envelope computeCenteredView(
      @Nullable Envelope extent, @Nullable Point center) {
    if (extent == null) {
      return null;
    }

    if (center == null
        || center.getX() == null
        || center.getY() == null
        || (Double.compare(center.getX(), 0.0) == 0 && Double.compare(center.getY(), 0.0) == 0)) {
      return extent;
    }

    double halfWidth =
        Math.max(
            Math.abs(center.getX() - extent.getMinX()), Math.abs(center.getX() - extent.getMaxX()));
    double halfHeight =
        Math.max(
            Math.abs(center.getY() - extent.getMinY()), Math.abs(center.getY() - extent.getMaxY()));

    return Envelope.builder()
        .minX(center.getX() - halfWidth)
        .maxX(center.getX() + halfWidth)
        .minY(center.getY() - halfHeight)
        .maxY(center.getY() + halfHeight)
        .build();
  }
}
