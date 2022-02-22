package org.sitmun.domain;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.*;

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
public class Envelope {

  /**
   * The envelope minimum y-value.
   */
  @JsonView({WorkspaceApplication.View.class})
  public Double minY;
  /**
   * The envelope maximum x-value.
   */
  @JsonView({WorkspaceApplication.View.class})
  private Double maxX;
  /**
   * The envelope maximum y-value.
   */
  @JsonView({WorkspaceApplication.View.class})
  private Double maxY;
  /**
   * The envelope minimum x-value.
   */
  @JsonView({WorkspaceApplication.View.class})
  private Double minX;

}
