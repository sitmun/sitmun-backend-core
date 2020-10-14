package org.sitmun.plugin.core.domain;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite primary key of {@link ThematicMapRange}.
 */
public class ThematicMapRangeId implements Serializable {

  private ThematicMap map;

  private Integer position;

  public ThematicMapRangeId() {
  }

  public ThematicMapRangeId(ThematicMap map, Integer position) {
    this.map = map;
    this.position = position;
  }

  public ThematicMap getMap() {
    return map;
  }

  public void setMap(ThematicMap map) {
    this.map = map;
  }

  public Integer getPosition() {
    return position;
  }

  public void setPosition(Integer position) {
    this.position = position;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ThematicMapRangeId that = (ThematicMapRangeId) o;
    return map.equals(that.map) && position.equals(that.position);
  }

  @Override
  public int hashCode() {
    return Objects.hash(map, position);
  }
}
