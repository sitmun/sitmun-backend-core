package org.sitmun.authorization.client.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.sitmun.domain.DomainConstants.Tasks.PROPERTY_DOWNLOAD_FORMAT;
import static org.sitmun.domain.DomainConstants.Tasks.PROPERTY_EXPORT_ENGINE;
import static org.sitmun.domain.DomainConstants.Tasks.PROPERTY_PAGE_ORIENTATION;
import static org.sitmun.domain.DomainConstants.Tasks.PROPERTY_PAGE_SIZE;
import static org.sitmun.domain.DomainConstants.Tasks.PROFILE_LAYER_ID_PREFIX;
import static org.sitmun.domain.DomainConstants.Tasks.SCOPE_RESOURCE;
import static org.sitmun.domain.DomainConstants.Tasks.TASK_PROFILE_ID_PREFIX;
import static org.sitmun.domain.DomainConstants.Tasks.TASK_TYPE_ID_DOCUMENT_EXPORT;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.authorization.client.dto.TaskDto;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.cartography.Cartography;
import org.sitmun.domain.task.Task;
import org.sitmun.domain.task.type.TaskType;
import org.sitmun.domain.territory.Territory;

@DisplayName("TaskDocumentExportService")
class TaskDocumentExportServiceTest {

  private final TaskDocumentExportService service = new TaskDocumentExportService();

  @Test
  void acceptReturnsTrueForDocumentExportTasks() {
    Task task = Task.builder().type(TaskType.builder().id(TASK_TYPE_ID_DOCUMENT_EXPORT).build()).build();

    assertTrue(service.accept(task));
  }

  @Test
  void acceptReturnsFalseForOtherTaskTypes() {
    Task task = Task.builder().type(TaskType.builder().id(15).build()).build();

    assertFalse(service.accept(task));
  }

  @Test
  void mapPublishesDocumentExportPropertiesInClientProfile() {
    Task task =
        Task.builder()
            .id(701)
            .name("Export PDF")
            .type(TaskType.builder().id(TASK_TYPE_ID_DOCUMENT_EXPORT).build())
            .properties(
                Map.of(
                    PROPERTY_EXPORT_ENGINE, "openhtmltopdf",
                    PROPERTY_DOWNLOAD_FORMAT, "pdf",
                    PROPERTY_PAGE_SIZE, "A3",
                    PROPERTY_PAGE_ORIENTATION, "landscape"))
            .build();

    TaskDto result = service.map(task, Application.builder().build(), Territory.builder().build());

    assertEquals(TASK_PROFILE_ID_PREFIX + "701", result.getId());
    assertEquals(TASK_TYPE_ID_DOCUMENT_EXPORT, result.getTypeId());
    assertEquals(SCOPE_RESOURCE, result.getScope());
    assertEquals("pdf", result.getParameters().get(PROPERTY_DOWNLOAD_FORMAT));
    assertEquals("pdf", result.getParameters().get("output"));
    assertEquals("openhtmltopdf", result.getParameters().get(PROPERTY_EXPORT_ENGINE));
    assertEquals("A3", result.getParameters().get(PROPERTY_PAGE_SIZE));
    assertEquals("landscape", result.getParameters().get(PROPERTY_PAGE_ORIENTATION));
  }

  @Test
  void mapPublishesPdfOutputAlias() {
    Task task =
        Task.builder()
            .id(704)
            .name("Export PDF")
            .type(TaskType.builder().id(TASK_TYPE_ID_DOCUMENT_EXPORT).build())
            .properties(
                Map.of(
                    PROPERTY_EXPORT_ENGINE, "openhtmltopdf",
                    PROPERTY_DOWNLOAD_FORMAT, "pdf"))
            .build();

    TaskDto result = service.map(task, Application.builder().build(), Territory.builder().build());

    assertEquals("pdf", result.getParameters().get(PROPERTY_DOWNLOAD_FORMAT));
    assertEquals("pdf", result.getParameters().get("output"));
  }

  @Test
  void mapOmitsParametersWhenPropertiesAreNull() {
    Task task =
        Task.builder()
            .id(702)
            .name("Export PDF")
            .type(TaskType.builder().id(TASK_TYPE_ID_DOCUMENT_EXPORT).build())
            .properties(null)
            .build();

    TaskDto result = service.map(task, Application.builder().build(), Territory.builder().build());

    assertNull(result.getParameters());
  }

  @Test
  void mapPublishesLayerWhenCartographyExists() {
    Task task =
        Task.builder()
            .id(703)
            .name("Export PDF")
            .type(TaskType.builder().id(TASK_TYPE_ID_DOCUMENT_EXPORT).build())
            .cartography(Cartography.builder().id(88).build())
            .properties(Map.of(PROPERTY_DOWNLOAD_FORMAT, "pdf"))
            .build();

    TaskDto result = service.map(task, Application.builder().build(), Territory.builder().build());

    assertEquals(PROFILE_LAYER_ID_PREFIX + "88", result.getLayer());
    assertEquals("pdf", result.getParameters().get(PROPERTY_DOWNLOAD_FORMAT));
    assertEquals("pdf", result.getParameters().get("output"));
  }
}
