package org.sitmun.administration.controller.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TemplatePreviewDtoTest {

  @Test
  void previewResponseRetainsHtmlAndPlaceholders() {
    TemplatePreviewResponseDto dto =
        TemplatePreviewResponseDto.builder()
            .html("<h1>{{task_13.name}}</h1>")
            .placeholders(List.of("task_13.name", "task_13.$param1"))
            .build();

    assertEquals("<h1>{{task_13.name}}</h1>", dto.getHtml());
    assertEquals(List.of("task_13.name", "task_13.$param1"), dto.getPlaceholders());
  }

  @Test
  void executionRequestRetainsManualParameters() {
    TemplateTaskExecutionRequestDto request = new TemplateTaskExecutionRequestDto();
    request.setTemplateTaskId(7);
    request.setLinkedTaskId(13);
    request.setParameters(Map.of("param1", "value"));

    assertEquals(7, request.getTemplateTaskId());
    assertEquals(13, request.getLinkedTaskId());
    assertEquals(Map.of("param1", "value"), request.getParameters());
  }

  @Test
  void previewRequestRetainsOptionalAppAndTerritoryCoordinates() {
    TemplatePreviewRequestDto request = new TemplatePreviewRequestDto();
    request.setTemplateHtml("<p>{{#APP_NAME}}</p>");
    request.setAppId(1);
    request.setTerId(4);

    assertEquals("<p>{{#APP_NAME}}</p>", request.getTemplateHtml());
    assertEquals(1, request.getAppId());
    assertEquals(4, request.getTerId());
  }
}
