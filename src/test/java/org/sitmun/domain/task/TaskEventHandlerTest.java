package org.sitmun.domain.task;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.administration.service.i18n.LiteralTranslationEnsureService;

@DisplayName("TaskEventHandler literal ensure")
class TaskEventHandlerTest {

  private LiteralTranslationEnsureService literalTranslationEnsureService;
  private TaskEventHandler handler;

  @BeforeEach
  void setUp() {
    literalTranslationEnsureService = mock(LiteralTranslationEnsureService.class);
    handler = new TaskEventHandler(Collections.emptyList(), literalTranslationEnsureService);
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
