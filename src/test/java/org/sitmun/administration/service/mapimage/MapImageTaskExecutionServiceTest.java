package org.sitmun.administration.service.mapimage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.imageio.ImageIO;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sitmun.administration.controller.dto.MapImageRenderRequestDto;
import org.sitmun.administration.service.extractor.HttpClientFactory;
import org.sitmun.domain.DomainConstants;
import org.sitmun.domain.service.Service;
import org.sitmun.domain.service.ServiceRepository;
import org.sitmun.domain.task.Task;
import org.sitmun.domain.task.TaskRepository;
import org.sitmun.domain.task.type.TaskType;
import org.sitmun.infrastructure.config.SystemVariableProperties;
import org.sitmun.infrastructure.variables.SystemVariableResolver;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
@DisplayName("MapImageTaskExecutionService")
class MapImageTaskExecutionServiceTest {

  @Mock private TaskRepository taskRepository;
  @Mock private ServiceRepository serviceRepository;
  @Mock private HttpClientFactory httpClientFactory;

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final SystemVariableResolver systemVariableResolver =
      new SystemVariableResolver(new SystemVariableProperties());

  @Test
  @DisplayName("renderMapImage composes WMS images in configured order")
  void renderMapImageComposesWmsImagesInConfiguredOrder() throws Exception {
    MapImageTaskExecutionService service = buildService();
    Task task = buildMapImageTask();
    Service wmsService = buildWmsService(5, "https://maps.example.com/wms");

    when(taskRepository.findById(33)).thenReturn(Optional.of(task));
    when(serviceRepository.findById(5)).thenReturn(Optional.of(wmsService));
    when(serviceRepository.findById(6)).thenReturn(Optional.of(buildWmsService(6, "https://maps.example.com/wms2")));
    when(httpClientFactory.executeRequest(org.mockito.ArgumentMatchers.any(Request.class)))
        .thenReturn(successResponse(png(Color.RED)))
        .thenReturn(successResponse(png(Color.BLUE)));

    MapImageRenderRequestDto request = new MapImageRenderRequestDto();
    request.setTaskId(33);
    request.setBbox(List.of(1d, 2d, 3d, 4d));
    request.setWidth(10);
    request.setHeight(10);

    byte[] rendered = service.renderMapImage(request);

    BufferedImage image = ImageIO.read(new java.io.ByteArrayInputStream(rendered));
    assertThat(image).isNotNull();
    assertThat(new Color(image.getRGB(5, 5), true).getBlue()).isEqualTo(255);
  }

  @Test
  @DisplayName("renderMapImage rejects non WMS services")
  void renderMapImageRejectsNonWmsServices() {
    MapImageTaskExecutionService service = buildService();
    Task task = buildMapImageTask();
    Service nonWmsService = buildWmsService(5, "https://maps.example.com/wmts");
    nonWmsService.setType("WMTS");

    when(taskRepository.findById(33)).thenReturn(Optional.of(task));
    when(serviceRepository.findById(5)).thenReturn(Optional.of(nonWmsService));

    MapImageRenderRequestDto request = new MapImageRenderRequestDto();
    request.setTaskId(33);
    request.setBbox(List.of(1d, 2d, 3d, 4d));

    assertThatThrownBy(() -> service.renderMapImage(request))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(exception -> assertThat(((ResponseStatusException) exception).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
  }

  @Test
  @DisplayName("renderMapImage rejects missing task")
  void renderMapImageRejectsMissingTask() {
    MapImageTaskExecutionService service = buildService();
    when(taskRepository.findById(33)).thenReturn(Optional.empty());

    MapImageRenderRequestDto request = new MapImageRenderRequestDto();
    request.setTaskId(33);

    assertThatThrownBy(() -> service.renderMapImage(request))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(exception -> assertThat(((ResponseStatusException) exception).getStatusCode())
            .isEqualTo(HttpStatus.NOT_FOUND));
  }

  @Test
  @DisplayName("renderMapImage rejects wrong task type")
  void renderMapImageRejectsWrongTaskType() {
    MapImageTaskExecutionService service = buildService();
    Task task = buildMapImageTask();
    task.getType().setId(DomainConstants.Tasks.TASK_TYPE_ID_QUERY);
    when(taskRepository.findById(33)).thenReturn(Optional.of(task));

    MapImageRenderRequestDto request = new MapImageRenderRequestDto();
    request.setTaskId(33);

    assertThatThrownBy(() -> service.renderMapImage(request))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(exception -> assertThat(((ResponseStatusException) exception).getReason())
            .isEqualTo("Task is not a map image task"));
  }

  @Test
  @DisplayName("renderMapImage rejects task without sources")
  void renderMapImageRejectsTaskWithoutSources() {
    MapImageTaskExecutionService service = buildService();
    Task task = buildMapImageTask();
    task.setProperties(Map.of());
    when(taskRepository.findById(33)).thenReturn(Optional.of(task));

    MapImageRenderRequestDto request = new MapImageRenderRequestDto();
    request.setTaskId(33);

    assertThatThrownBy(() -> service.renderMapImage(request))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(exception -> assertThat(((ResponseStatusException) exception).getReason())
            .isEqualTo("Task has no map sources configured"));
  }

  @Test
  @DisplayName("renderMapImage rejects unsupported format")
  void renderMapImageRejectsUnsupportedFormat() {
    MapImageTaskExecutionService service = buildService();
    when(taskRepository.findById(33)).thenReturn(Optional.of(buildMapImageTask()));

    MapImageRenderRequestDto request = new MapImageRenderRequestDto();
    request.setTaskId(33);
    request.setBbox(List.of(1d, 2d, 3d, 4d));
    request.setFormat("jpeg");

    assertThatThrownBy(() -> service.renderMapImage(request))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(exception -> assertThat(((ResponseStatusException) exception).getReason())
            .isEqualTo("Only png output is supported for map image tasks"));
  }

  @Test
  @DisplayName("renderMapImage rejects missing bbox")
  void renderMapImageRejectsMissingBbox() {
    MapImageTaskExecutionService service = buildService();
    when(taskRepository.findById(33)).thenReturn(Optional.of(buildMapImageTask()));

    MapImageRenderRequestDto request = new MapImageRenderRequestDto();
    request.setTaskId(33);

    assertThatThrownBy(() -> service.renderMapImage(request))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(exception -> assertThat(((ResponseStatusException) exception).getReason())
            .isEqualTo("bbox must contain four numeric values"));
  }

  @Test
  @DisplayName("renderMapImage rejects overlay source without service id")
  void renderMapImageRejectsOverlaySourceWithoutServiceId() {
    MapImageTaskExecutionService service = buildService();
    Task task = buildMapImageTask();
    task.setProperties(Map.of(DomainConstants.Tasks.PROPERTY_MAP_SOURCES, List.of(Map.of("layerNames", List.of("layer_a")))));
    when(taskRepository.findById(33)).thenReturn(Optional.of(task));

    MapImageRenderRequestDto request = new MapImageRenderRequestDto();
    request.setTaskId(33);
    request.setBbox(List.of(1d, 2d, 3d, 4d));

    assertThatThrownBy(() -> service.renderMapImage(request))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(exception -> assertThat(((ResponseStatusException) exception).getReason())
            .isEqualTo("Map source lacks serviceId"));
  }

  @Test
  @DisplayName("renderMapImage rejects overlay service not found")
  void renderMapImageRejectsOverlayServiceNotFound() {
    MapImageTaskExecutionService service = buildService();
    when(taskRepository.findById(33)).thenReturn(Optional.of(buildMapImageTask()));
    when(serviceRepository.findById(5)).thenReturn(Optional.empty());

    MapImageRenderRequestDto request = new MapImageRenderRequestDto();
    request.setTaskId(33);
    request.setBbox(List.of(1d, 2d, 3d, 4d));

    assertThatThrownBy(() -> service.renderMapImage(request))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(exception -> assertThat(((ResponseStatusException) exception).getStatusCode())
            .isEqualTo(HttpStatus.NOT_FOUND));
  }

  private MapImageTaskExecutionService buildService() {
    MapImageWmsRenderer wmsRenderer = new MapImageWmsRenderer(httpClientFactory, systemVariableResolver);
    return new MapImageTaskExecutionService(
        taskRepository,
        serviceRepository,
        wmsRenderer,
        objectMapper);
  }

  private static Task buildMapImageTask() {
    Task task = new Task();
    task.setId(33);
    TaskType taskType = new TaskType();
    taskType.setId(DomainConstants.Tasks.TASK_TYPE_ID_MAP_IMAGE);
    task.setType(taskType);
    task.setProperties(
        Map.of(
            DomainConstants.Tasks.PROPERTY_MAP_SOURCES,
            List.of(
                Map.of("serviceId", 5, "layerNames", List.of("layer_a")),
                Map.of("serviceId", 6, "layerNames", List.of("layer_b")))));
    return task;
  }

  private static Service buildWmsService(int id, String url) {
    Service service = new Service();
    service.setId(id);
    service.setType("WMS");
    service.setServiceURL(url);
    return service;
  }

  private static Response successResponse(byte[] body) {
    return new Response.Builder()
        .request(new Request.Builder().url("https://maps.example.com/wms").build())
        .protocol(Protocol.HTTP_1_1)
        .code(200)
        .message("OK")
        .body(ResponseBody.create(okhttp3.MediaType.parse("image/png"), body))
        .build();
  }

  private static byte[] png(Color color) throws IOException {
    BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
    Graphics2D graphics = image.createGraphics();
    try {
      graphics.setColor(color);
      graphics.fillRect(0, 0, 10, 10);
    } finally {
      graphics.dispose();
    }

    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      ImageIO.write(image, "png", out);
      return out.toByteArray();
    }
  }
}
