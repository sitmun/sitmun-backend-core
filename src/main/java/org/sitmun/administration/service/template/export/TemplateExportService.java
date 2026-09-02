package org.sitmun.administration.service.template.export;

import com.openhtmltopdf.extend.FSStream;
import com.openhtmltopdf.layout.LayoutContext;
import com.openhtmltopdf.outputdevice.helper.ExternalResourceControlPriority;
import com.openhtmltopdf.pdfboxout.PdfBoxRenderer;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.render.Box;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Document.OutputSettings;
import org.sitmun.domain.DomainConstants;
import org.sitmun.domain.task.Task;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/** Converts rendered template HTML to PDF. */
@Service
public class TemplateExportService {

  static final String DEFAULT_PDF_PAGE_SIZE = PdfPageOptionsPolicy.DEFAULT_PAGE_SIZE;
  static final String DEFAULT_PDF_PAGE_ORIENTATION = PdfPageOptionsPolicy.DEFAULT_ORIENTATION;
  private final PdfPageOptionsPolicy pageOptionsPolicy;
  private final PdfExternalResourceLoader externalResourceLoader;

  public TemplateExportService() {
    this.pageOptionsPolicy = new PdfPageOptionsPolicy();
    this.externalResourceLoader = new PdfExternalResourceLoader();
  }

  /**
   * Exports rendered HTML to PDF.
   *
   * @param html rendered HTML string, which is mandatory
   * @param output output format; only {@code "pdf"} is supported
   * @param exportTask optional authorized document export task used for output and page settings
   * @return the file content as a byte array
   */
  public byte[] exportHtml(String html, String output, Task exportTask) {
    String normalizedOutput = normalizeOutput(output);
    validateOutput(normalizedOutput);
    validateOutputAllowed(exportTask, normalizedOutput);
    if (!StringUtils.hasText(html)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Template HTML is required");
    }
    validateSourceLength(html);
    PdfPageConfig pageConfig = resolvePdfPageConfig(exportTask);
    Document sourceDocument = Jsoup.parse(html);
    validateDocumentComplexity(sourceDocument);
    PdfExternalResourceLoader.Session resourceLoader = externalResourceLoader.newSession();
    PdfDocumentPreparer.PdfRegionDimensions dimensions =
        measurePdfRegions(sourceDocument.clone(), pageConfig, resourceLoader);
    Document document = PdfDocumentPreparer.prepare(sourceDocument, pageConfig, dimensions);
    return convertHtmlToPdf(document, resourceLoader);
  }

  public String resolveExportFilename(Task templateTask, Task exportTask, String output) {
    String extension = normalizeOutput(output);
    validateOutput(extension);
    Task filenameTask = templateTask != null ? templateTask : exportTask;
    if (filenameTask == null) {
      return "report." + extension;
    }

    String taskName =
        StringUtils.hasText(filenameTask.getName()) ? filenameTask.getName().trim() : "report";

    return sanitizeFilename(taskName) + "." + extension;
  }

  private static void validateOutputAllowed(Task exportTask, String output) {
    if (exportTask == null) {
      return;
    }
    if (!isOutputAllowed(exportTask.getProperties(), output)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Output '" + output + "' is not enabled for task " + exportTask.getId());
    }
  }

  private static boolean isOutputAllowed(Map<String, Object> properties, String output) {
    String normalizedOutput = normalizeOutput(output);
    if (properties != null) {
      Object rawFormat = properties.get(DomainConstants.Tasks.PROPERTY_DOWNLOAD_FORMAT);
      if (rawFormat != null && StringUtils.hasText(String.valueOf(rawFormat))) {
        return normalizedOutput.equals(normalizeOutput(String.valueOf(rawFormat)));
      }
    }

    return false;
  }

  public static String normalizeOutput(String output) {
    return output == null ? "" : output.trim().toLowerCase(Locale.ROOT);
  }

  private static String sanitizeFilename(String value) {
    return value.replaceAll("[\\\\/:*?\"<>|]+", "_");
  }

  PdfPageConfig resolvePdfPageConfig(Task exportTask) {
    org.sitmun.administration.service.template.export.PdfPageConfig config =
        pageOptionsPolicy.resolve(exportTask);
    return new PdfPageConfig(config.pageSize(), config.pageOrientation());
  }

  private byte[] convertHtmlToPdf(
      Document jsoupDoc, PdfExternalResourceLoader.Session resourceLoader) {
    try (ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfBoxRenderer renderer =
            createRendererBuilder(jsoupDoc, out, resourceLoader).buildPdfRenderer()) {
      renderer.layout();
      validatePageCount(renderer);
      renderer.createPDF();
      return out.toByteArray();
    } catch (ResponseStatusException e) {
      throw e;
    } catch (Exception e) {
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate PDF", e);
    }
  }

  PdfDocumentPreparer.PdfRegionDimensions measurePdfRegions(String html, PdfPageConfig pageConfig) {
    return measurePdfRegions(
        Jsoup.parse(html == null ? "" : html),
        pageConfig,
        externalResourceLoader.newSession());
  }

  private static void validateOutput(String output) {
    if (!"pdf".equals(output)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Unsupported template export output: " + output);
    }
  }

  private static void validateSourceLength(String source) {
    if (source.length() > DomainConstants.Tasks.MAX_TEMPLATE_EXPORT_SOURCE_CHARACTERS) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Export source is too large");
    }
  }

  private static void validateDocumentComplexity(Document document) {
    if (document.getAllElements().size() > DomainConstants.Tasks.MAX_TEMPLATE_EXPORT_DOM_ELEMENTS) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Export document is too complex");
    }
  }

  private PdfDocumentPreparer.PdfRegionDimensions measurePdfRegions(
      Document sourceDocument,
      PdfPageConfig pageConfig,
      PdfExternalResourceLoader.Session resourceLoader) {
    PdfDocumentPreparer.MeasurementDocument measurement =
        PdfDocumentPreparer.prepareForMeasurement(sourceDocument, pageConfig);
    if (!measurement.hasHeader() && !measurement.hasFooter()) {
      return new PdfDocumentPreparer.PdfRegionDimensions(0, 0);
    }

    try (PDDocument pdfDocument = new PDDocument();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfBoxRenderer renderer =
            createRendererBuilder(measurement.document(), out, resourceLoader)
                .usePDDocument(pdfDocument)
                .buildPdfRenderer()) {
      renderer.layout();
      validatePageCount(renderer);
      LayoutContext context = renderer.getSharedContext().newLayoutContextInstance();
      double headerHeight =
          measurement.hasHeader()
              ? measureElementHeight(renderer, context, PdfDocumentPreparer.MEASUREMENT_HEADER)
              : 0;
      if (measurement.headerHasTopSpacing()) {
        headerHeight += PdfDocumentPreparer.HEADER_TOP_SPACING_MM;
      }
      double footerHeight =
          measurement.hasFooter()
              ? measureElementHeight(renderer, context, PdfDocumentPreparer.MEASUREMENT_FOOTER)
              : 0;
      return new PdfDocumentPreparer.PdfRegionDimensions(headerHeight, footerHeight);
    } catch (ResponseStatusException e) {
      throw e;
    } catch (Exception e) {
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Failed to measure PDF header and footer", e);
    }
  }

  private static double measureElementHeight(
      PdfBoxRenderer renderer, LayoutContext context, String regionName) {
    Box box = findMeasurementBox(renderer.getRootBox(), regionName);
    if (box == null) {
      throw new IllegalStateException("PDF region was not laid out: " + regionName);
    }
    return box.getMarginEdge(context, 0, 0).getHeight() * context.getMmPerDot();
  }

  private static Box findMeasurementBox(Box box, String regionName) {
    if (box.getElement() != null
        && regionName.equals(
            box.getElement().getAttribute(PdfDocumentPreparer.MEASUREMENT_ATTRIBUTE))) {
      return box;
    }
    for (Box child : box.getChildren()) {
      Box match = findMeasurementBox(child, regionName);
      if (match != null) {
        return match;
      }
    }
    return null;
  }

  private PdfRendererBuilder createRendererBuilder(
      Document jsoupDoc,
      ByteArrayOutputStream out,
      PdfExternalResourceLoader.Session resourceLoader) {
    // Convert HTML5 through Jsoup to avoid the Oracle XML parser rejecting non-XHTML input.
    jsoupDoc.outputSettings().syntax(OutputSettings.Syntax.xml);
    org.w3c.dom.Document w3cDoc = new W3CDom().fromJsoup(jsoupDoc);
    PdfRendererBuilder builder = new PdfRendererBuilder();
    builder.useFastMode();
    builder.useHttpStreamImplementation(uri -> new ByteArrayResourceStream(resourceLoader.fetch(uri)));
    builder.useExternalResourceAccessControl(
        (uri, resourceType) -> externalResourceLoader.isAllowed(uri),
        ExternalResourceControlPriority.RUN_AFTER_RESOLVING_URI);
    builder.withW3cDocument(w3cDoc, null);
    builder.toStream(out);
    return builder;
  }

  private static void validatePageCount(PdfBoxRenderer renderer) {
    int pageCount = renderer.getRootBox().getLayer().getPages().size();
    if (pageCount > DomainConstants.Tasks.MAX_TEMPLATE_EXPORT_PDF_PAGES) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Export PDF has too many pages");
    }
  }

  private record ByteArrayResourceStream(byte[] content) implements FSStream {
    @Override
    public ByteArrayInputStream getStream() {
      return new ByteArrayInputStream(content);
    }

    @Override
    public Reader getReader() {
      return new InputStreamReader(getStream(), StandardCharsets.UTF_8);
    }
  }

  /** Keeps package-local callers source-compatible while the shared type lives independently. */
  @Deprecated
  static final class PdfPageConfig
      extends org.sitmun.administration.service.template.export.PdfPageConfig {
    PdfPageConfig(String pageSize, String pageOrientation) {
      super(pageSize, pageOrientation);
    }
  }
}
