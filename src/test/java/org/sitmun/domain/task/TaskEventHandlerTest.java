package org.sitmun.domain.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.administration.service.i18n.LiteralTranslationEnsureService;
import org.sitmun.domain.DomainConstants;
import org.sitmun.domain.task.type.TaskType;

class TaskEventHandlerTest {

  private LiteralTranslationEnsureService literalTranslationEnsureService;
  private TaskEventHandler handler;

  @BeforeEach
  void setUp() {
    literalTranslationEnsureService = mock(LiteralTranslationEnsureService.class);
    handler = new TaskEventHandler(Collections.emptyList(), literalTranslationEnsureService);
  }

  @Test
  void removesDeprecatedPdfRegionHeightsBeforePersistence() {
    Map<String, Object> properties =
        new HashMap<>(
            Map.of(
                "templateHtml", "<p>Template</p>",
                "pdfHeaderHeightMm", 25,
                "pdfFooterHeightMm", 15));
    Task task =
        Task.builder()
            .type(TaskType.builder().id(DomainConstants.Tasks.TASK_TYPE_ID_TEMPLATE).build())
            .properties(properties)
            .build();

    handler.handleTaskCreate(task);

    assertThat(task.getProperties())
        .containsEntry("templateHtml", "<p>Template</p>")
        .doesNotContainKeys("pdfHeaderHeightMm", "pdfFooterHeightMm");
  }

  @Test
  void preservesSamePropertyNamesOnOtherTaskTypes() {
    Task task =
        Task.builder()
            .type(TaskType.builder().id(DomainConstants.Tasks.TASK_TYPE_ID_QUERY).build())
            .properties(Map.of("pdfHeaderHeightMm", 25))
            .build();

    handler.handleTaskCreate(task);

    assertThat(task.getProperties()).containsEntry("pdfHeaderHeightMm", 25);
  }

  @Test
  @DisplayName("B3: ensure failure propagates so Task save aborts")
  void ensureFailurePropagates() {
    doThrow(new IllegalStateException("Default language not found in STM_LANGUAGE: xx"))
        .when(literalTranslationEnsureService)
        .ensureLiteralsFromTemplateHtml(anyString());

    Map<String, Object> properties = new HashMap<>();
    properties.put("templateHtml", "<p><t>key</t></p>");
    Task task = Task.builder().name("t").properties(properties).build();

    assertThatThrownBy(() -> handler.handleTaskCreate(task))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Default language");
  }

  @Test
  @DisplayName("skips ensure when templateHtml absent")
  void skipsWhenNoTemplateHtml() {
    Task task =
        Task.builder().name("t").properties(Map.of("parameters", Collections.emptyList())).build();

    handler.handleTaskCreate(task);

    verify(literalTranslationEnsureService, never()).ensureLiteralsFromTemplateHtml(anyString());
  }
}
