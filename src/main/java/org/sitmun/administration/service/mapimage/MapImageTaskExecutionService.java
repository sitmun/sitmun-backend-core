package org.sitmun.administration.service.mapimage;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import org.sitmun.administration.controller.dto.MapImageRenderRequestDto;
import org.sitmun.domain.DomainConstants;
import org.sitmun.domain.service.Service;
import org.sitmun.domain.service.ServiceBlockPolicy;
import org.sitmun.domain.service.ServiceRepository;
import org.sitmun.domain.task.Task;
import org.sitmun.domain.task.TaskRepository;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class MapImageTaskExecutionService {

  private static final String DEFAULT_FORMAT = "png";
  private static final int DEFAULT_WIDTH = 1024;
  private static final int DEFAULT_HEIGHT = 768;
  private static final TypeReference<List<MapImageSourceDefinition>> MAP_SOURCE_TYPE =
      new TypeReference<>() {};

  private final TaskRepository taskRepository;
  private final ServiceRepository serviceRepository;
  private final MapImageWmsRenderer mapImageWmsRenderer;
  private final ObjectMapper objectMapper;

  @Transactional(readOnly = true, noRollbackFor = ResponseStatusException.class)
  public byte[] renderMapImage(MapImageRenderRequestDto requestDto) {
    Task task = taskRepository.findById(requestDto.getTaskId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found: " + requestDto.getTaskId()));

    if (!DomainConstants.Tasks.isMapImageTask(task)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Task is not a map image task");
    }

    Map<String, Object> properties = task.getProperties() == null ? Map.of() : task.getProperties();
    List<MapImageSourceDefinition> mapSources = readMapSources(properties);
    if (mapSources.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Task has no map sources configured");
    }

    List<Double> bbox = resolveBbox(requestDto.getBbox());
    int width = resolveDimension(requestDto.getWidth(), properties, DomainConstants.Tasks.PROPERTY_WIDTH, DEFAULT_WIDTH);
    int height = resolveDimension(requestDto.getHeight(), properties, DomainConstants.Tasks.PROPERTY_HEIGHT, DEFAULT_HEIGHT);
    String format = normalizeFormat(resolveString(requestDto.getFormat(), properties, DomainConstants.Tasks.PROPERTY_FORMAT, DEFAULT_FORMAT));
    String srs = resolveString(requestDto.getSrs(), properties, DomainConstants.Tasks.PROPERTY_SRS, "EPSG:4326");
    MapImageRenderContext renderContext = new MapImageRenderContext(bbox, width, height, srs);

    if (!DEFAULT_FORMAT.equalsIgnoreCase(format)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only png output is supported for map image tasks");
    }

    BufferedImage canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    Graphics2D graphics = canvas.createGraphics();
    try {
      graphics.setComposite(AlphaComposite.SrcOver);
      for (MapImageSourceDefinition mapSource : mapSources) {
        BufferedImage overlay = renderOverlaySource(mapSource, renderContext);
        graphics.drawImage(overlay, 0, 0, width, height, null);
        overlay.flush();
      }
    } finally {
      graphics.dispose();
    }
    return encodePng(canvas);
  }

  private List<MapImageSourceDefinition> readMapSources(Map<String, Object> properties) {
    Object rawMapSources = properties.get(DomainConstants.Tasks.PROPERTY_MAP_SOURCES);
    if (rawMapSources == null) {
      return List.of();
    }
    List<MapImageSourceDefinition> mapSources = objectMapper.convertValue(rawMapSources, MAP_SOURCE_TYPE);
    return mapSources == null ? List.of() : mapSources;
  }

  private List<Double> resolveBbox(List<Double> requestBbox) {
    return MapImageBboxValidator.validate(requestBbox);
  }

  private int resolveDimension(Integer requestValue, Map<String, Object> properties, String propertyKey, int defaultValue) {
    if (requestValue != null && requestValue > 0) {
      return requestValue;
    }
    Object rawValue = properties.get(propertyKey);
    if (rawValue instanceof Number number && number.intValue() > 0) {
      return number.intValue();
    }
    return defaultValue;
  }

  private String resolveString(String requestValue, Map<String, Object> properties, String propertyKey, String defaultValue) {
    if (StringUtils.hasText(requestValue)) {
      return requestValue.trim();
    }
    Object rawValue = properties.get(propertyKey);
    return rawValue instanceof String value && StringUtils.hasText(value) ? value.trim() : defaultValue;
  }

  private String normalizeFormat(String format) {
    return StringUtils.hasText(format) ? format.trim().toLowerCase() : DEFAULT_FORMAT;
  }

  private BufferedImage renderOverlaySource(MapImageSourceDefinition mapSource, MapImageRenderContext renderContext) {
    if (mapSource.serviceId() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Map source lacks serviceId");
    }
    if (mapSource.layerNames() == null || mapSource.layerNames().isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Map source lacks layerNames");
    }

    Service service = serviceRepository.findById(mapSource.serviceId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service not found: " + mapSource.serviceId()));

    if (!DomainConstants.Services.TYPE_WMS.equalsIgnoreCase(service.getType())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Service " + mapSource.serviceId() + " is not WMS");
    }
    if (!ServiceBlockPolicy.isAccessibleInClientProfile(service)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Service " + mapSource.serviceId() + " is blocked");
    }

    return mapImageWmsRenderer.render(service, mapSource.layerNames(), renderContext);
  }

  private byte[] encodePng(BufferedImage image) {
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      if (!ImageIO.write(image, "png", out)) {
        throw new IOException("No PNG writer available");
      }
      return out.toByteArray();
    } catch (IOException e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to encode PNG image", e);
    }
  }

  private record MapImageSourceDefinition(Integer serviceId, List<String> layerNames) {}
}
