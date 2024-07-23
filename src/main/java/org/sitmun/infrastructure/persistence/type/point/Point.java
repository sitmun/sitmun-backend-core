package org.sitmun.infrastructure.persistence.type.point;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.*;
import org.sitmun.authorization.dto.ClientConfigurationViews;

import java.util.Objects;

/**
 * Defines a 2D point .
 */
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
@JsonView(ClientConfigurationViews.ApplicationTerritory.class)
public class Point {

  /**
   * The x-value.
   */
  private Double x;

  /**
   * The y-value.
   */
  private Double y;

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
        return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
        return false;
    }
    Point point = (Point) obj;
    return Objects.equals(x, point.x) && Objects.equals(y, point.y);
  }

  @Override
  public int hashCode() {
    return Objects.hash(x, y);
  }
}
