package org.sitmun.administration.service.template.export;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sitmun.domain.DomainConstants;
import org.sitmun.domain.task.Task;
import org.sitmun.domain.task.TaskRepository;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
@DisplayName("TemplateExportService")
class TemplateExportServiceTest {

  @Mock private TaskRepository taskRepository;

  @TempDir private Path tempDir;

  @Test
  @DisplayName("exportHtml prioritizes runtime HTML over configured XML path")
  void exportHtmlPrioritizesRuntimeHtmlOverConfiguredXmlPath() throws Exception {
    TemplateExportService service = new TemplateExportService(taskRepository);
    ReflectionTestUtils.setField(service, "allowedFilePathPrefix", tempDir.toString());

    Path xmlFile = tempDir.resolve("reports/export.xml");
    Files.createDirectories(xmlFile.getParent());
    Files.writeString(xmlFile, "<html><body>server file</body></html>", StandardCharsets.UTF_8);

    Task task = buildTask(101, Map.of(
        DomainConstants.Tasks.PROPERTY_DOWNLOAD_FORMAT, "pdf",
        DomainConstants.Tasks.PROPERTY_DOWNLOAD_SOURCE, "reports/export.xml"));
    when(taskRepository.findById(101)).thenReturn(Optional.of(task));

    byte[] content = service.exportHtml("<html><body>runtime html</body></html>", "pdf", 101L);

    assertThat(content).isNotEmpty();
    verify(taskRepository, atLeastOnce()).findById(101);
  }

  @Test
  @DisplayName("exportHtml falls back to configured XML path when runtime HTML is missing")
  void exportHtmlFallsBackToConfiguredXmlPath() throws Exception {
    TemplateExportService service = new TemplateExportService(taskRepository);
    ReflectionTestUtils.setField(service, "allowedFilePathPrefix", tempDir.toString());

    Path xmlFile = tempDir.resolve("reports/export.xml");
    Files.createDirectories(xmlFile.getParent());
    Files.writeString(xmlFile, "<html><body>server file</body></html>", StandardCharsets.UTF_8);

    Task task = buildTask(201, Map.of(
        DomainConstants.Tasks.PROPERTY_DOWNLOAD_FORMAT, "pdf",
        DomainConstants.Tasks.PROPERTY_DOWNLOAD_SOURCE, "reports/export.xml"));
    when(taskRepository.findById(201)).thenReturn(Optional.of(task));

    byte[] content = service.exportHtml("   ", "pdf", 201L);

    assertThat(content).isNotEmpty();
    verify(taskRepository, atLeastOnce()).findById(201);
  }

  @Test
  @DisplayName("exportHtml fails when neither runtime HTML nor configured XML path is available")
  void exportHtmlFailsWhenNoRuntimeHtmlOrConfiguredXmlPathExists() {
    TemplateExportService service = new TemplateExportService(taskRepository);

    Task task = buildTask(301, Map.of(
        DomainConstants.Tasks.PROPERTY_DOWNLOAD_FORMAT, "pdf"));
    when(taskRepository.findById(301)).thenReturn(Optional.of(task));

    assertThatThrownBy(() -> service.exportHtml("", "pdf", 301L))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(exception -> {
          ResponseStatusException responseStatusException = (ResponseStatusException) exception;
          assertThat(responseStatusException.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        });
  }

  @Test
  @DisplayName("exportHtml returns raw UTF-8 bytes for xml output")
  void exportHtmlReturnsRawUtf8BytesForXmlOutput() {
    TemplateExportService service = new TemplateExportService(taskRepository);

    byte[] content = service.exportHtml("<xml>ok</xml>", "xml", null);

    assertThat(new String(content, StandardCharsets.UTF_8)).isEqualTo("<xml>ok</xml>");
    verify(taskRepository, never()).findById(org.mockito.ArgumentMatchers.anyInt());
  }

  @Test
  @DisplayName("exportHtml rejects output not enabled for task")
  void exportHtmlRejectsOutputNotEnabledForTask() {
    TemplateExportService service = new TemplateExportService(taskRepository);
    when(taskRepository.findById(401)).thenReturn(Optional.of(buildTask(401, Map.of(
        DomainConstants.Tasks.PROPERTY_DOWNLOAD_FORMAT, "pdf"))));

    assertThatThrownBy(() -> service.exportHtml("<xml/>", "xml", 401L))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(exception -> {
          ResponseStatusException responseStatusException = (ResponseStatusException) exception;
          assertThat(responseStatusException.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
          assertThat(responseStatusException.getReason()).contains("Output 'xml' is not enabled");
        });
  }

  @Test
  @DisplayName("exportTaskFile rejects path traversal outside allowed directory")
  void exportTaskFileRejectsPathTraversalOutsideAllowedDirectory() {
    TemplateExportService service = new TemplateExportService(taskRepository);
    ReflectionTestUtils.setField(service, "allowedFilePathPrefix", tempDir.toString());
    when(taskRepository.findById(501)).thenReturn(Optional.of(buildTask(501, Map.of(
        DomainConstants.Tasks.PROPERTY_DOWNLOAD_FORMAT, "xml",
        DomainConstants.Tasks.PROPERTY_DOWNLOAD_SOURCE, "../secret.xml"))));

    assertThatThrownBy(() -> service.exportTaskFile(501L, "xml"))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(exception -> {
          ResponseStatusException responseStatusException = (ResponseStatusException) exception;
          assertThat(responseStatusException.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
          assertThat(responseStatusException.getReason()).isEqualTo("Invalid file path");
        });
  }

  @Test
  @DisplayName("exportTaskFile fails when file export base path is not configured")
  void exportTaskFileFailsWhenFileExportBasePathIsNotConfigured() {
    TemplateExportService service = new TemplateExportService(taskRepository);
    ReflectionTestUtils.setField(service, "allowedFilePathPrefix", " ");
    when(taskRepository.findById(601)).thenReturn(Optional.of(buildTask(601, Map.of(
        DomainConstants.Tasks.PROPERTY_DOWNLOAD_FORMAT, "xml",
        DomainConstants.Tasks.PROPERTY_DOWNLOAD_SOURCE, "reports/export.xml"))));

    assertThatThrownBy(() -> service.exportTaskFile(601L, "xml"))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(exception -> {
          ResponseStatusException responseStatusException = (ResponseStatusException) exception;
          assertThat(responseStatusException.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
          assertThat(responseStatusException.getReason()).isEqualTo("File-based export is not configured on this server");
        });
  }

  @Test
  @DisplayName("exportTaskFile fails when configured source file does not exist")
  void exportTaskFileFailsWhenConfiguredSourceFileDoesNotExist() {
    TemplateExportService service = new TemplateExportService(taskRepository);
    ReflectionTestUtils.setField(service, "allowedFilePathPrefix", tempDir.toString());
    when(taskRepository.findById(701)).thenReturn(Optional.of(buildTask(701, Map.of(
        DomainConstants.Tasks.PROPERTY_DOWNLOAD_FORMAT, "xml",
        DomainConstants.Tasks.PROPERTY_DOWNLOAD_SOURCE, "reports/missing.xml"))));

    assertThatThrownBy(() -> service.exportTaskFile(701L, "xml"))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(exception -> {
          ResponseStatusException responseStatusException = (ResponseStatusException) exception;
          assertThat(responseStatusException.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
          assertThat(responseStatusException.getReason()).isEqualTo("Report file not found");
        });
  }

  @Test
  @DisplayName("exportTaskFile validates enabled output before reading task properties")
  void exportTaskFileValidatesEnabledOutputBeforeReadingTaskProperties() {
    TemplateExportService service = new TemplateExportService(taskRepository);
    Task task = new Task();
    task.setId(801);
    task.setProperties(null);
    when(taskRepository.findById(801)).thenReturn(Optional.of(task));

    assertThatThrownBy(() -> service.exportTaskFile(801L, "xml"))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(exception -> {
          ResponseStatusException responseStatusException = (ResponseStatusException) exception;
          assertThat(responseStatusException.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
          assertThat(responseStatusException.getReason()).contains("Output 'xml' is not enabled");
        });
  }

  @Test
  @DisplayName("resolveExportFilename sanitizes task name and uses report fallback")
  void resolveExportFilenameSanitizesTaskNameAndUsesReportFallback() {
    TemplateExportService service = new TemplateExportService(taskRepository);
    when(taskRepository.findById(901)).thenReturn(Optional.of(buildTask(901, Map.of())));
    when(taskRepository.findById(902)).thenReturn(Optional.of(buildTaskWithName(902, "Quarterly:/Report?*", Map.of())));

    assertThat(service.resolveExportFilename(null, "pdf")).isEqualTo("report.pdf");
    assertThat(service.resolveExportFilename(901L, "xml")).isEqualTo("Export task.xml");
    assertThat(service.resolveExportFilename(902L, "pdf")).isEqualTo("Quarterly_Report_.pdf");
  }

  @Test
  @DisplayName("validateOutputAllowed ignores null task id")
  void validateOutputAllowedIgnoresNullTaskId() {
    TemplateExportService service = new TemplateExportService(taskRepository);

    Throwable thrown = catchThrowable(() -> service.validateOutputAllowed(null, "pdf"));

    assertThat(thrown).isNull();
    verify(taskRepository, never()).findById(org.mockito.ArgumentMatchers.anyInt());
  }

  private static Task buildTask(int id, Map<String, Object> properties) {
    return buildTaskWithName(id, "Export task", properties);
  }

  private static Task buildTaskWithName(int id, String name, Map<String, Object> properties) {
    Task task = new Task();
    task.setId(id);
    task.setName(name);
    task.setProperties(properties);
    return task;
  }
}
