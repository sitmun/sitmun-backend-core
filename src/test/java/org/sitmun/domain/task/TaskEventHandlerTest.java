package org.sitmun.domain.task;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.sitmun.domain.DomainConstants;
import org.sitmun.domain.task.type.TaskType;

class TaskEventHandlerTest {

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

    new TaskEventHandler(List.of()).handleTaskCreate(task);

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

    new TaskEventHandler(List.of()).handleTaskCreate(task);

    assertThat(task.getProperties()).containsEntry("pdfHeaderHeightMm", 25);
  }
}
