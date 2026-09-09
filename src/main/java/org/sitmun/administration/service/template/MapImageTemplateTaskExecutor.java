package org.sitmun.administration.service.template;

import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.sitmun.administration.controller.dto.MapImageRenderRequestDto;
import org.sitmun.administration.controller.dto.TemplateTaskExecutionResponseDto;
import org.sitmun.administration.service.mapimage.MapImageTaskExecutionService;
import org.sitmun.domain.task.Task;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/** Executes map-image tasks and exposes rendered content as an embedded data URL. */
@Component
final class MapImageTemplateTaskExecutor {
  private static final String COMPLETED = "COMPLETED";
  private static final String VALUE = "value";

  private final MapImageFeatureBboxResolver bboxResolver;
  private final MapImageTaskExecutionService mapImageTaskExecutionService;

  MapImageTemplateTaskExecutor(
      MapImageFeatureBboxResolver bboxResolver,
      MapImageTaskExecutionService mapImageTaskExecutionService) {
    this.bboxResolver = bboxResolver;
    this.mapImageTaskExecutionService = mapImageTaskExecutionService;
  }

  TemplateTaskExecutionResponseDto execute(Task task, Map<String, String> parameters) {
    MapImageRenderRequestDto request = new MapImageRenderRequestDto();
    request.setTaskId(task.getId());
    request.setBbox(bboxResolver.resolve(task, parameters));
    byte[] content = mapImageTaskExecutionService.renderMapImage(request);
    String contentUrl =
        "data:"
            + MediaType.IMAGE_PNG_VALUE
            + ";base64,"
            + Base64.getEncoder().encodeToString(content);
    Map<String, Object> context = new LinkedHashMap<>();
    context.put("contentUrl", contentUrl);
    context.put("url", contentUrl);
    context.put("mimeType", MediaType.IMAGE_PNG_VALUE);
    context.put("binary", true);
    context.put("embeddable", true);
    context.put(VALUE, "[contenido binario]");
    return TemplateTaskExecutionResponseDto.builder()
        .taskId(task.getId())
        .status(COMPLETED)
        .resultType("resource")
        .context(context)
        .rows(Collections.emptyList())
        .resourceUrl(contentUrl)
        .build();
  }
}
