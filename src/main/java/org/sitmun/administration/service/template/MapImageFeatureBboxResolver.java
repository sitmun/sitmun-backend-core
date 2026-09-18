package org.sitmun.administration.service.template;

import java.util.List;
import java.util.Map;
import org.sitmun.administration.service.mapimage.MapImageBboxValidator;
import org.sitmun.domain.DomainConstants;
import org.sitmun.domain.task.Task;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/** Resolves feature-driven map image bounds using task rendering settings. */
@Component
final class MapImageFeatureBboxResolver {
  private static final String FEATURE_BBOX_SIZE = "__featureBboxSize";
  private static final int DEFAULT_WIDTH = 1024;
  private static final int DEFAULT_HEIGHT = 768;
  private static final double GEOGRAPHIC_MIN_SIZE = 0.0015d;
  private static final double PROJECTED_MIN_SIZE = 150d;

  List<Double> resolve(Task task, Map<String, String> parameters) {
    List<Double> bbox = readFeatureBbox(parameters);
    if (bbox == null) {
      return null;
    }
    bbox = expandDegenerateBbox(bbox, readDegenerateBboxSize(task));
    bbox = applyBboxMargin(bbox, readBboxMarginRatio(task));
    return fitBboxToAspectRatio(
        bbox,
        readPositiveTaskDimension(task, DomainConstants.Tasks.PROPERTY_WIDTH, DEFAULT_WIDTH),
        readPositiveTaskDimension(task, DomainConstants.Tasks.PROPERTY_HEIGHT, DEFAULT_HEIGHT));
  }

  private List<Double> readFeatureBbox(Map<String, String> parameters) {
    if (parameters == null || parameters.isEmpty()) {
      return null;
    }
    if (parameters.containsKey(FEATURE_BBOX_SIZE)
        && !"4".equals(parameters.get(FEATURE_BBOX_SIZE))) {
      throw MapImageBboxValidator.invalidBbox();
    }
    List<String> keys =
        List.of("featureBboxMinX", "featureBboxMinY", "featureBboxMaxX", "featureBboxMaxY");
    boolean hasBbox =
        parameters.containsKey(FEATURE_BBOX_SIZE)
            || keys.stream().anyMatch(parameters::containsKey);
    if (!hasBbox) {
      return null;
    }
    if (keys.stream().anyMatch(key -> !StringUtils.hasText(parameters.get(key)))) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "featureBbox parameters must include featureBboxMinX, featureBboxMinY, featureBboxMaxX and featureBboxMaxY together");
    }
    return MapImageBboxValidator.validate(
        keys.stream().map(key -> readDoubleParameter(parameters, key)).toList());
  }

  private Double readDoubleParameter(Map<String, String> parameters, String key) {
    try {
      return Double.valueOf(parameters.get(key).trim());
    } catch (NumberFormatException exception) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "featureBbox parameter " + key + " must be numeric", exception);
    }
  }

  private double readBboxMarginRatio(Task task) {
    Object value =
        task.getProperties() == null
            ? null
            : task.getProperties().get(DomainConstants.Tasks.PROPERTY_BBOX_MARGIN_PERCENT);
    return value instanceof Number number ? Math.max(0d, number.doubleValue()) / 100d : 0d;
  }

  private double readDegenerateBboxSize(Task task) {
    Object value =
        task.getProperties() == null
            ? null
            : task.getProperties().get(DomainConstants.Tasks.PROPERTY_SRS);
    String srs = value instanceof String string ? string.trim() : "";
    return "EPSG:4326".equalsIgnoreCase(srs) || "CRS:84".equalsIgnoreCase(srs)
        ? GEOGRAPHIC_MIN_SIZE
        : PROJECTED_MIN_SIZE;
  }

  private List<Double> expandDegenerateBbox(List<Double> bbox, double minimumSize) {
    double minX = bbox.get(0);
    double minY = bbox.get(1);
    double maxX = bbox.get(2);
    double maxY = bbox.get(3);
    if (Double.compare(minX, maxX) == 0) {
      minX -= minimumSize / 2d;
      maxX += minimumSize / 2d;
    }
    if (Double.compare(minY, maxY) == 0) {
      minY -= minimumSize / 2d;
      maxY += minimumSize / 2d;
    }
    return List.of(minX, minY, maxX, maxY);
  }

  private List<Double> applyBboxMargin(List<Double> bbox, double marginRatio) {
    double horizontalMargin = (bbox.get(2) - bbox.get(0)) * marginRatio / 2d;
    double verticalMargin = (bbox.get(3) - bbox.get(1)) * marginRatio / 2d;
    return List.of(
        bbox.get(0) - horizontalMargin,
        bbox.get(1) - verticalMargin,
        bbox.get(2) + horizontalMargin,
        bbox.get(3) + verticalMargin);
  }

  private List<Double> fitBboxToAspectRatio(List<Double> bbox, int width, int height) {
    double bboxWidth = bbox.get(2) - bbox.get(0);
    double bboxHeight = bbox.get(3) - bbox.get(1);
    double bboxRatio = bboxWidth / bboxHeight;
    double targetRatio = (double) width / height;
    if (bboxRatio < targetRatio) {
      double halfWidth = bboxHeight * targetRatio / 2d;
      double centerX = (bbox.get(0) + bbox.get(2)) / 2d;
      return List.of(centerX - halfWidth, bbox.get(1), centerX + halfWidth, bbox.get(3));
    }
    if (bboxRatio > targetRatio) {
      double halfHeight = bboxWidth / targetRatio / 2d;
      double centerY = (bbox.get(1) + bbox.get(3)) / 2d;
      return List.of(bbox.get(0), centerY - halfHeight, bbox.get(2), centerY + halfHeight);
    }
    return bbox;
  }

  private int readPositiveTaskDimension(Task task, String key, int fallback) {
    Object value = task.getProperties() == null ? null : task.getProperties().get(key);
    return value instanceof Number number && number.intValue() > 0 ? number.intValue() : fallback;
  }
}
