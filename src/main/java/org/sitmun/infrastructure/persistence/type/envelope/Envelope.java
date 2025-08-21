package org.sitmun.infrastructure.persistence.type.envelope;

import com.fasterxml.jackson.annotation.JsonView;
import java.util.Objects;
import lombok.*;
import org.sitmun.authorization.client.dto.ClientConfigurationViews;

/**
 * Defines a rectangular region of the 2D coordinate plane.
 *
 * <p>It is often used to represent the bounding box of a Geometry, e.g. the minimum and maximum x
 * and y values of the Coordinates.
 */
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
@JsonView(ClientConfigurationViews.ApplicationTerritory.class)
public class Envelope {

  /** The envelope minimum y-value. */
  private Double minY;

  /** The envelope maximum x-value. */
  private Double maxX;

  /** The envelope maximum y-value. */
  private Double maxY;

  /** The envelope minimum x-value. */
  private Double minX;

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    Envelope envelope = (Envelope) obj;
    return Objects.equals(minY, envelope.minY)
        && Objects.equals(maxX, envelope.maxX)
        && Objects.equals(maxY, envelope.maxY)
        && Objects.equals(minX, envelope.minX);
  }

  @Override
  public int hashCode() {
    return Objects.hash(minY, maxX, maxY, minX);
  }
}
