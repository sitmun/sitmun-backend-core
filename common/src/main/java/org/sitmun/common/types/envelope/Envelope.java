package org.sitmun.common.types.envelope;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.*;
import org.sitmun.feature.client.config.Views;

/**
 * Defines a rectangular region of the 2D coordinate plane.
 * <p>
 * It is often used to represent the bounding box of a Geometry,
 * e.g. the minimum and maximum x and y values of the Coordinates.
 */
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
@JsonView({Views.WorkspaceApplication.class})
public class Envelope {

  /**
   * The envelope minimum y-value.
   */
  public Double minY;
  /**
   * The envelope maximum x-value.
   */
  private Double maxX;
  /**
   * The envelope maximum y-value.
   */
  private Double maxY;
  /**
   * The envelope minimum x-value.
   */
  private Double minX;

}
