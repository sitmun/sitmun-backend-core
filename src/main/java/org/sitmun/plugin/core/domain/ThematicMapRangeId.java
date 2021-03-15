package org.sitmun.plugin.core.domain;

import lombok.*;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite primary key of {@link ThematicMapRange}.
 */
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ThematicMapRangeId implements Serializable {

  private ThematicMap map;

  private Integer position;

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ThematicMapRangeId that = (ThematicMapRangeId) o;
    return Objects.equals(map, that.map) && Objects.equals(position, that.position);
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
