package org.sitmun.administration.service.mapimage;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class MapImageBboxValidator {

  private static final String INVALID_BBOX_MESSAGE =
      "bbox must contain exactly four finite values ordered as minX, minY, maxX, maxY";

  private MapImageBboxValidator() {}

  public static List<Double> validate(List<Double> bbox) {
    if (bbox == null
        || bbox.size() != 4
        || bbox.stream().anyMatch(value -> value == null || !Double.isFinite(value))) {
      throw invalidBbox();
    }
    if (bbox.get(0) > bbox.get(2) || bbox.get(1) > bbox.get(3)) {
      throw invalidBbox();
    }
    return List.copyOf(bbox);
  }

  public static ResponseStatusException invalidBbox() {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, INVALID_BBOX_MESSAGE);
  }
}
