package org.sitmun.administration.service.template.export;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.jsoup.nodes.Document.OutputSettings;
import org.sitmun.domain.DomainConstants;
import org.sitmun.domain.task.Task;
import org.sitmun.domain.task.TaskRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/**
 * Handles template export to downloadable file formats.
 *
 * <p>Supports two modes:
 * <ul>
 *   <li><strong>HTML → PDF</strong>: converts rendered HTML to a PDF byte array.
 *   <li><strong>Task file</strong>: reads a Jasper/XML report file from the server filesystem
 *       based on the task's {@code downloadSource} property.
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class TemplateExportService {

  private final TaskRepository taskRepository;

  @Value("${sitmun.template.export.allowed-file-path-prefix:}")
  private String allowedFilePathPrefix;

  /**
   * Exports rendered HTML to a PDF or returns it as raw bytes.
   *
   * @param html   the rendered HTML string; when blank, the service falls back to the configured
   *     server-side XML path for the task if present
   * @param output the output format ({@code "pdf"} or other)
   * @param taskId optional document export task id used to validate the requested output against the
   *     task configuration
   * @return the file content as a byte array
   */
  public byte[] exportHtml(String html, String output, Long taskId) {
    validateOutputAllowed(taskId, output);
    String effectiveSource = resolveEffectiveExportSource(html, taskId);
    if ("pdf".equalsIgnoreCase(output)) {
      return convertHtmlToPdf(effectiveSource);
    }
    return effectiveSource.getBytes(java.nio.charset.StandardCharsets.UTF_8);
  }

  /**
   * Returns the file referenced by the task's {@code downloadSource} property.
   *
   * @param taskId the child task ID
   * @param output the expected output format (used for validation only)
   * @return the file content as a byte array
   */
  public byte[] exportTaskFile(Long taskId, String output) {
    validateOutputAllowed(taskId, output);
    Task task = getTask(taskId);

    return readConfiguredSource(task, taskId);
  }

  private String resolveEffectiveExportSource(String html, Long taskId) {
    if (StringUtils.hasText(html)) {
      return html;
    }

    if (taskId == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Either runtime HTML content or a task with configured XML source must be provided");
    }

    Task task = getTask(taskId);
    byte[] content = readConfiguredSource(task, taskId);
    return new String(content, java.nio.charset.StandardCharsets.UTF_8);
  }

  private byte[] readConfiguredSource(Task task, Long taskId) {
    Map<String, Object> properties = task.getProperties();
    if (properties == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Task " + taskId + " has no properties configured");
    }

    Object rawSource = properties.get(DomainConstants.Tasks.PROPERTY_DOWNLOAD_SOURCE);
    if (rawSource == null || !StringUtils.hasText(String.valueOf(rawSource))) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Task " + taskId + " does not have a downloadSource configured");
    }

    if (!StringUtils.hasText(allowedFilePathPrefix)) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
          "File-based export is not configured on this server");
    }

    String relativePath = String.valueOf(rawSource).trim();
    Path baseDir = Paths.get(allowedFilePathPrefix).toAbsolutePath().normalize();
    Path resolved = baseDir.resolve(relativePath).normalize();

    if (!resolved.startsWith(baseDir)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Invalid file path");
    }

    if (!Files.isRegularFile(resolved)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND,
          "Report file not found");
    }

    try {
      return Files.readAllBytes(resolved);
    } catch (IOException e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
          "Failed to read report file");
    }
  }

  private Task getTask(Long taskId) {
    return taskRepository.findById(taskId.intValue())
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND, "Task not found: " + taskId));
  }

  public String resolveExportFilename(Long taskId, String output) {
    return resolveExportFilename(null, taskId, output);
  }

  public String resolveExportFilename(Long templateTaskId, Long taskId, String output) {
    String extension = normalizeOutput(output);
    Long filenameTaskId = templateTaskId != null ? templateTaskId : taskId;
    if (filenameTaskId == null) {
      return "report." + extension;
    }

    Task task = taskRepository.findById(filenameTaskId.intValue())
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND, "Task not found: " + filenameTaskId));

    String taskName = StringUtils.hasText(task.getName())
        ? task.getName().trim()
        : "report";

    return sanitizeFilename(taskName) + "." + extension;
  }

  public void validateOutputAllowed(Long taskId, String output) {
    if (taskId == null) {
      return;
    }

    Task task = taskRepository.findById(taskId.intValue())
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND, "Task not found: " + taskId));

    if (!isOutputAllowed(task.getProperties(), output)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Output '" + output + "' is not enabled for task " + taskId);
    }
  }

  public static boolean isOutputAllowed(Map<String, Object> properties, String output) {
    String normalizedOutput = normalizeOutput(output);
    if (properties != null) {
      Object rawFormat = properties.get(DomainConstants.Tasks.PROPERTY_DOWNLOAD_FORMAT);
      if (rawFormat != null && StringUtils.hasText(String.valueOf(rawFormat))) {
        return normalizedOutput.equals(normalizeOutput(String.valueOf(rawFormat)));
      }
    }

    return false;
  }

  private static String normalizeOutput(String output) {
    return output == null ? "" : output.trim().toLowerCase(Locale.ROOT);
  }

  private static String sanitizeFilename(String value) {
    return value.replaceAll("[\\\\/:*?\"<>|]+", "_");
  }

  private byte[] convertHtmlToPdf(String html) {
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      // Use Jsoup to parse arbitrary HTML (including HTML5 / non-XHTML content) and convert
      // to a W3C DOM. This avoids the Oracle XML SAX parser (pulled in by the Oracle JDBC
      // driver) which rejects HTML content with DOCTYPE or unclosed tags.
      org.jsoup.nodes.Document jsoupDoc = Jsoup.parse(html);
      jsoupDoc.outputSettings().syntax(OutputSettings.Syntax.xml);
      org.w3c.dom.Document w3cDoc = new W3CDom().fromJsoup(jsoupDoc);

      PdfRendererBuilder builder = new PdfRendererBuilder();
      builder.useFastMode();
      builder.withW3cDocument(w3cDoc, null);
      builder.toStream(out);
      builder.run();
      return out.toByteArray();
    } catch (Exception e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
          "Failed to generate PDF");
    }
  }
}
