package org.sitmun.administration.service.template;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.sitmun.administration.service.mapimage.MapImageTaskExecutionService;
import org.sitmun.domain.DomainConstants;
import org.sitmun.domain.task.Task;

class MapImageTemplateTaskExecutorTest {
  @Test
  void resolvesFeatureBboxAndEmbedsRenderedImage() {
    MapImageTaskExecutionService renderService = mock(MapImageTaskExecutionService.class);
    when(renderService.renderMapImage(org.mockito.ArgumentMatchers.any()))
        .thenReturn(new byte[] {1, 2});
    MapImageTemplateTaskExecutor executor =
        new MapImageTemplateTaskExecutor(new MapImageFeatureBboxResolver(), renderService);
    Task task = new Task();
    task.setId(42);
    task.setProperties(
        Map.of(
            DomainConstants.Tasks.PROPERTY_WIDTH, 800,
            DomainConstants.Tasks.PROPERTY_HEIGHT, 600));

    var response =
        executor.execute(
            task,
            Map.of(
                "__featureBboxSize", "4",
                "featureBboxMinX", "0",
                "featureBboxMinY", "0",
                "featureBboxMaxX", "4",
                "featureBboxMaxY", "3"));

    verify(renderService)
        .renderMapImage(
            argThat(
                request ->
                    request.getTaskId().equals(42)
                        && request.getBbox().equals(List.of(0d, 0d, 4d, 3d))));
    assertThat(response.getResourceUrl()).isEqualTo("data:image/png;base64,AQI=");
    assertThat(response.getContext())
        .containsEntry("binary", true)
        .containsEntry("embeddable", true);
  }
}
