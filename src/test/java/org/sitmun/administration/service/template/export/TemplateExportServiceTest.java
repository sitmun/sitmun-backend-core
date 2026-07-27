package org.sitmun.administration.service.template.export;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.net.InetAddress;
import java.net.Inet6Address;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import okhttp3.Dns;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.domain.DomainConstants;
import org.sitmun.domain.task.Task;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@DisplayName("TemplateExportService")
class TemplateExportServiceTest {

  @Test
  @DisplayName("exportHtml requires runtime HTML")
  void exportHtmlRequiresRuntimeHtml() {
    TemplateExportService service = newService();

    Task task = buildTask(301, Map.of(DomainConstants.Tasks.PROPERTY_DOWNLOAD_FORMAT, "pdf"));
    assertThatThrownBy(() -> service.exportHtml("", "pdf", task))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(
            exception -> {
              ResponseStatusException responseStatusException = (ResponseStatusException) exception;
              assertThat(responseStatusException.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            });
  }

  @Test
  @DisplayName("exportHtml normalizes output before dispatch")
  void exportHtmlNormalizesOutputBeforeDispatch() {
    TemplateExportService service = newService();
    Task task =
        buildTask(101, Map.of(DomainConstants.Tasks.PROPERTY_DOWNLOAD_FORMAT, "pdf"));

    byte[] content = service.exportHtml("<html><body>PDF</body></html>", " PDF ", task);

    assertThat(content).isNotEmpty();
    assertThat(new String(content, StandardCharsets.UTF_8)).doesNotContain("<html>");
  }

  @Test
  @DisplayName("prepareHtmlForPdf keeps zero margins when no PDF regions exist")
  void prepareHtmlForPdfKeepsZeroMarginsWithoutRegions() {
    Document document =
        PdfDocumentPreparer.prepare(
            "<p>content</p>",
            new TemplateExportService.PdfPageConfig("A4", "portrait"),
            new PdfDocumentPreparer.PdfRegionDimensions(25, 15));

    assertThat(document.selectFirst("style[data-sitmun-pdf-layout]").data())
        .contains("@page { size: A4 portrait; margin: 0; }");
  }

  @Test
  @DisplayName("prepareHtmlForPdf configures direct running PDF regions")
  void prepareHtmlForPdfConfiguresDirectRunningRegions() {
    Document document =
        PdfDocumentPreparer.prepare(
            "<main>content</main><p class=\"custom sitmun-pdf-footer\">Footer</p><table"
                + " class=\"sitmun-pdf-header\"><tbody><tr><td>Header</td></tr></tbody></table>",
            new TemplateExportService.PdfPageConfig("A3", "landscape"),
            new PdfDocumentPreparer.PdfRegionDimensions(32, 18));

    String css = document.selectFirst("style[data-sitmun-pdf-layout]").data();
    assertThat(css).contains("size: A3 landscape", "margin: 32mm 0 18mm 0");
    assertThat(css)
        .contains("@top-center { content: element(sitmunPdfHeader); vertical-align: top; }");
    assertThat(css)
        .contains("@bottom-center { content: element(sitmunPdfFooter); vertical-align: bottom; }");
    assertThat(css)
        .contains(
            ".sitmun-pdf-running-header { position: running(sitmunPdfHeader) !important;"
                + " margin-top: 5mm !important; }");
    assertThat(css)
        .contains(
            ".sitmun-pdf-running-contained { width: 100% !important; max-width: none !important;"
                + " box-sizing: border-box !important; }")
        .doesNotContain("margin: 0 !important");
    assertThat(document.body().children().get(0).hasClass("sitmun-pdf-running-header")).isTrue();
    assertThat(document.body().children().get(1).hasClass("sitmun-pdf-running-footer")).isTrue();
    assertThat(document.body().children().get(2).tagName()).isEqualTo("main");
    assertThat(document.selectFirst(".sitmun-pdf-footer").hasClass("custom")).isTrue();
  }

  @Test
  @DisplayName("prepareHtmlForPdf configures full-bleed regions without ancestor layout")
  void prepareHtmlForPdfConfiguresFullBleedRegions() {
    Document document =
        PdfDocumentPreparer.prepare(
            "<table id=\"layout\" style=\"width: 768px\"><tbody><tr><td style=\"padding: 0 40px\">"
                + "<p class=\"sitmun-pdf-header-full-bleed\">Header</p></td></tr></tbody></table>"
                + "<p class=\"sitmun-pdf-footer-full-bleed\">Footer</p>",
            new TemplateExportService.PdfPageConfig("A4", "portrait"),
            new PdfDocumentPreparer.PdfRegionDimensions(20, 10));

    String css = document.selectFirst("style[data-sitmun-pdf-layout]").data();
    Element runningHeader = document.selectFirst(".sitmun-pdf-running-header");

    assertThat(runningHeader.tagName()).isEqualTo("p");
    assertThat(runningHeader.parents()).noneMatch(parent -> parent.id().equals("layout"));
    assertThat(css)
        .contains(
            ".sitmun-pdf-running-full-bleed { width: 100% !important; max-width: none !important;"
                + " box-sizing: border-box !important; margin: 0 !important; }");
    assertThat(css).doesNotContain("margin-top: 5mm");
  }

  @Test
  @DisplayName("prepareHtmlForPdf prefers root regions over nested regions")
  void prepareHtmlForPdfPrefersRootRegionsOverNestedRegions() {
    Document document =
        PdfDocumentPreparer.prepare(
            "<p class=\"sitmun-pdf-header-full-bleed\" data-sitmun-pdf-template-scope=\"nested\">"
                + "Nested</p><p class=\"sitmun-pdf-header\" "
                + "data-sitmun-pdf-template-scope=\"root\">Root</p><main>Body</main>",
            new TemplateExportService.PdfPageConfig("A4", "portrait"),
            new PdfDocumentPreparer.PdfRegionDimensions(20, 0));

    assertThat(document.selectFirst(".sitmun-pdf-running-header").text()).isEqualTo("Root");
    assertThat(document.text()).doesNotContain("Nested");
    assertThat(document.selectFirst("style[data-sitmun-pdf-layout]").data())
        .contains("margin-top: 5mm")
        .doesNotContain("sitmun-pdf-running-full-bleed");
  }

  @Test
  @DisplayName("prepareHtmlForPdf resolves root header and nested footer independently")
  void prepareHtmlForPdfResolvesHeaderAndFooterIndependently() {
    Document document =
        PdfDocumentPreparer.prepare(
            "<p class=\"sitmun-pdf-header\" data-sitmun-pdf-template-scope=\"root\">Root header</p>"
                + "<p class=\"sitmun-pdf-footer-full-bleed\" "
                + "data-sitmun-pdf-template-scope=\"nested\">Nested footer</p>",
            new TemplateExportService.PdfPageConfig("A4", "portrait"),
            new PdfDocumentPreparer.PdfRegionDimensions(20, 10));

    assertThat(document.selectFirst(".sitmun-pdf-running-header").text())
        .isEqualTo("Root header");
    assertThat(document.selectFirst(".sitmun-pdf-running-footer").text())
        .isEqualTo("Nested footer");
  }

  @Test
  @DisplayName("prepareHtmlForPdf rejects multiple nested regions without a root region")
  void prepareHtmlForPdfRejectsMultipleNestedRegionsWithoutRootRegion() {
    assertThatThrownBy(
            () ->
                PdfDocumentPreparer.prepare(
                    "<p class=\"sitmun-pdf-header\" data-sitmun-pdf-template-scope=\"nested\">One</p>"
                        + "<p class=\"sitmun-pdf-header\" "
                        + "data-sitmun-pdf-template-scope=\"nested\">Two</p>",
                    new TemplateExportService.PdfPageConfig("A4", "portrait"),
                    new PdfDocumentPreparer.PdfRegionDimensions(20, 0)))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Only one PDF header");
  }

  @Test
  @DisplayName("prepareForMeasurement preserves styles declared in the body")
  void prepareForMeasurementPreservesBodyStyles() {
    PdfDocumentPreparer.MeasurementDocument measurement =
        PdfDocumentPreparer.prepareForMeasurement(
            "<body class=\"report\"><style>.sitmun-pdf-header { font-size: 30px; }</style>"
                + "<p class=\"sitmun-pdf-header\">Header</p></body>",
            new TemplateExportService.PdfPageConfig("A4", "portrait"));

    assertThat(measurement.document().body().className()).isEqualTo("report");
    assertThat(measurement.document().head().select("style").stream()
            .anyMatch(style -> style.data().contains("font-size: 30px")))
        .isTrue();
  }

  @Test
  @DisplayName("prepareForMeasurement measures responsive layout context")
  void prepareForMeasurementMeasuresResponsiveLayoutContext() {
    PdfDocumentPreparer.MeasurementDocument measurement =
        PdfDocumentPreparer.prepareForMeasurement(
            "<table id=\"layout\" style=\"width: 768px; height: 200mm\"><tbody><tr><td>"
                + "<p class=\"sitmun-pdf-header\" style=\"height: 10mm; margin: 0\">Header</p>"
                + "</td></tr></tbody></table>",
            new TemplateExportService.PdfPageConfig("A3", "portrait"));

    Element measurementBox =
        measurement.document().selectFirst("[data-sitmun-pdf-measure=header]");
    Element runningWrapper = measurement.document().selectFirst(".sitmun-pdf-running-header");
    String css = measurement.document().selectFirst("style[data-sitmun-pdf-layout]").data();

    assertThat(measurementBox).isSameAs(runningWrapper);
    assertThat(runningWrapper.id()).isEqualTo("layout");
    assertThat(css).contains(".sitmun-pdf-running-contained { width: 100% !important");
    assertThat(css)
        .contains(
            ".sitmun-pdf-running-context { height: auto !important; min-height: 0 !important;"
                + " max-height: none !important; }");
  }

  @Test
  @DisplayName("prepareHtmlForPdf rejects duplicate PDF headers")
  void prepareHtmlForPdfRejectsDuplicateHeaders() {
    assertThatThrownBy(
            () ->
                PdfDocumentPreparer.prepare(
                    "<p class=\"sitmun-pdf-header\">One</p><p class=\"sitmun-pdf-header\">Two</p>",
                    new TemplateExportService.PdfPageConfig("A4", "portrait"),
                    new PdfDocumentPreparer.PdfRegionDimensions(25, 15)))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(
            exception ->
                assertThat(((ResponseStatusException) exception).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST));
  }

  @Test
  @DisplayName("prepareHtmlForPdf preserves nested PDF header layout context")
  void prepareHtmlForPdfPreservesNestedHeaderLayoutContext() {
    Document document =
        PdfDocumentPreparer.prepare(
            "<table id=\"template-content\" class=\"template-wrapper\" style=\"width:"
                + " 768px\"><colgroup id=\"layout-columns\"><col style=\"width:"
                + " 100px\"><col></colgroup><tbody><tr><td id=\"sidebar\" style=\"width:"
                + " 100px\">Sidebar</td><td style=\"padding: 0 41px\"><p>Content</p><table"
                + " class=\"custom sitmun-pdf-header\" style=\"width: 100%\"><tbody><tr><td><img"
                + " src=\"logo.png\" width=\"110\" style=\"width: 110px\">Header</td><td><img"
                + " src=\"data:image/png;base64,AA==\" width=\"80\" style=\"width: 80px\"></td>"
                + "</tr></tbody></table></td></tr></tbody></table>",
            new TemplateExportService.PdfPageConfig("A4", "portrait"),
            new PdfDocumentPreparer.PdfRegionDimensions(25, 15));

    Element runningHeader = document.selectFirst(".sitmun-pdf-running-header");
    Element originalWrapper = document.body().children().get(1);
    Element outerRow =
        runningHeader.children().stream()
            .filter(element -> "tbody".equals(element.normalName()))
            .findFirst()
            .orElseThrow()
            .child(0);

    assertThat(runningHeader.tagName()).isEqualTo("table");
    assertThat(runningHeader.id()).isEqualTo("template-content");
    assertThat(runningHeader.attr("style")).isEqualTo("width: 768px");
    assertThat(runningHeader.select("colgroup col")).hasSize(2);
    assertThat(runningHeader.selectFirst("colgroup").id()).isEqualTo("layout-columns");
    assertThat(outerRow.children()).hasSize(2);
    assertThat(outerRow.child(0).text()).isEmpty();
    assertThat(outerRow.child(0).id()).isEqualTo("sidebar");
    assertThat(outerRow.child(1).attr("style")).isEqualTo("padding: 0 41px");
    assertThat(runningHeader.select(".sitmun-pdf-header")).hasSize(1);
    assertThat(runningHeader.selectFirst(".sitmun-pdf-header").hasClass("custom")).isTrue();
    assertThat(runningHeader.selectFirst(".sitmun-pdf-header").parent().tagName()).isEqualTo("td");
    assertThat(runningHeader.selectFirst(".sitmun-pdf-header").attr("style"))
        .isEqualTo("width: 100%");
    assertThat(runningHeader.select(".sitmun-pdf-header img").get(0).attr("width"))
        .isEqualTo("110");
    assertThat(runningHeader.select(".sitmun-pdf-header img").get(1).attr("width")).isEqualTo("80");
    assertThat(runningHeader.select(".sitmun-pdf-header img").get(1).attr("src"))
        .isEqualTo("data:image/png;base64,AA==");
    assertThat(originalWrapper.hasClass("template-wrapper")).isTrue();
    assertThat(originalWrapper.id()).isEqualTo("template-content");
    assertThat(originalWrapper.select(".sitmun-pdf-header")).isEmpty();
    assertThat(originalWrapper.text()).isEqualTo("Sidebar Content");
  }

  @Test
  @DisplayName("prepareHtmlForPdf rejects PDF regions nested within each other")
  void prepareHtmlForPdfRejectsRegionsNestedWithinEachOther() {
    assertThatThrownBy(
            () ->
                PdfDocumentPreparer.prepare(
                    "<table class=\"sitmun-pdf-header\"><tbody><tr><td>"
                        + "<p class=\"sitmun-pdf-footer\">Footer</p></td></tr></tbody></table>",
                    new TemplateExportService.PdfPageConfig("A4", "portrait"),
                    new PdfDocumentPreparer.PdfRegionDimensions(25, 15)))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(
            exception ->
                assertThat(((ResponseStatusException) exception).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST));
  }

  @Test
  @DisplayName("prepareHtmlForPdf rejects every invalid PDF region shape")
  void prepareHtmlForPdfRejectsInvalidRegionShapes() {
    TemplateExportService.PdfPageConfig pageConfig =
        new TemplateExportService.PdfPageConfig("A4", "portrait");
    PdfDocumentPreparer.PdfRegionDimensions layoutConfig =
        new PdfDocumentPreparer.PdfRegionDimensions(25, 15);

    assertThatThrownBy(
            () ->
                PdfDocumentPreparer.prepare(
                    "<p class=\"sitmun-pdf-footer\">A</p><p class=\"sitmun-pdf-footer\">B</p>",
                    pageConfig,
                    layoutConfig))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Only one PDF footer");
    assertThatThrownBy(
            () ->
                PdfDocumentPreparer.prepare(
                    "<p class=\"sitmun-pdf-header sitmun-pdf-footer\">A</p>",
                    pageConfig,
                    layoutConfig))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("multiple PDF region types");
    assertThatThrownBy(
            () ->
                PdfDocumentPreparer.prepare(
                    "<div class=\"sitmun-pdf-header\">A</div>", pageConfig, layoutConfig))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("must be supported blocks");
  }

  @Test
  @DisplayName("measurePdfRegions uses rendered content height without configured limits")
  void measurePdfRegionsUsesRenderedContentHeight() {
    TemplateExportService service = newService();
    TemplateExportService.PdfPageConfig pageConfig =
        new TemplateExportService.PdfPageConfig("A4", "portrait");

    PdfDocumentPreparer.PdfRegionDimensions shortRegions =
        service.measurePdfRegions(
            "<table class=\"sitmun-pdf-header\"><tbody><tr><td>Header</td></tr></tbody></table>",
            pageConfig);
    PdfDocumentPreparer.PdfRegionDimensions tallRegions =
        service.measurePdfRegions(
            "<table class=\"sitmun-pdf-header\"><tbody>"
                + "<tr><td>One</td></tr><tr><td>Two</td></tr><tr><td>Three</td></tr>"
                + "</tbody></table>",
            pageConfig);
    PdfDocumentPreparer.PdfRegionDimensions fullBleedRegions =
        service.measurePdfRegions(
            "<table class=\"sitmun-pdf-header-full-bleed\"><tbody><tr><td>Header</td></tr>"
                + "</tbody></table>",
            pageConfig);

    assertThat(shortRegions.headerHeightMm()).isGreaterThan(5);
    assertThat(tallRegions.headerHeightMm()).isGreaterThan(shortRegions.headerHeightMm());
    assertThat(shortRegions.headerHeightMm() - fullBleedRegions.headerHeightMm())
        .isBetween(4.99, 5.01);
  }

  @Test
  @DisplayName("measurePdfRegions ignores tall fixed layout ancestors across page formats")
  void measurePdfRegionsIgnoresTallLayoutAncestorsAcrossPageFormats() {
    TemplateExportService service = newService();
    String baselineHtml =
        "<table style=\"width: 768px\"><tbody><tr><td>"
            + "<p class=\"sitmun-pdf-header\" style=\"height: 10mm; margin: 0\">Header</p>"
            + "</td></tr></tbody></table>";
    String html =
        "<table style=\"width: 768px; height: 200mm\"><tbody><tr><td>"
            + "<p class=\"sitmun-pdf-header\" style=\"height: 10mm; margin: 0\">Header</p>"
            + "</td></tr></tbody></table>";

    double baseline =
        service
            .measurePdfRegions(
                baselineHtml, new TemplateExportService.PdfPageConfig("A4", "portrait"))
            .headerHeightMm();
    double a4Portrait =
        service
            .measurePdfRegions(
                html, new TemplateExportService.PdfPageConfig("A4", "portrait"))
            .headerHeightMm();
    double a3Portrait =
        service
            .measurePdfRegions(
                html, new TemplateExportService.PdfPageConfig("A3", "portrait"))
            .headerHeightMm();
    double a3Landscape =
        service
            .measurePdfRegions(
                html, new TemplateExportService.PdfPageConfig("A3", "landscape"))
            .headerHeightMm();

    assertThat(a4Portrait).isBetween(baseline - 0.1, baseline + 0.1);
    assertThat(a3Portrait).isBetween(a4Portrait - 0.1, a4Portrait + 0.1);
    assertThat(a3Landscape).isBetween(a4Portrait - 0.1, a4Portrait + 0.1);
  }

  @Test
  @DisplayName("measurePdfRegions ignores tall fixed footer ancestors")
  void measurePdfRegionsIgnoresTallFooterAncestors() {
    TemplateExportService service = newService();
    String baselineHtml =
        "<table style=\"width: 768px\"><tbody><tr><td>"
            + "<p class=\"sitmun-pdf-footer\" style=\"height: 10mm; margin: 0\">Footer</p>"
            + "</td></tr></tbody></table>";
    String html =
        "<table style=\"width: 768px; height: 200mm\"><tbody><tr><td>"
            + "<p class=\"sitmun-pdf-footer\" style=\"height: 10mm; margin: 0\">Footer</p>"
            + "</td></tr></tbody></table>";

    double baseline =
        service
            .measurePdfRegions(
                baselineHtml, new TemplateExportService.PdfPageConfig("A3", "landscape"))
            .footerHeightMm();
    PdfDocumentPreparer.PdfRegionDimensions dimensions =
        service.measurePdfRegions(
            html, new TemplateExportService.PdfPageConfig("A3", "landscape"));

    assertThat(dimensions.footerHeightMm()).isBetween(baseline - 0.1, baseline + 0.1);
  }

  @Test
  @DisplayName("measurePdfRegions includes vertical spacing from layout ancestors")
  void measurePdfRegionsIncludesVerticalLayoutSpacing() {
    TemplateExportService service = newService();
    String html =
        "<div style=\"height: 200mm; padding-top: 20mm; padding-bottom: 20mm\">"
            + "<p class=\"sitmun-pdf-header\" style=\"height: 10mm; margin: 0\">Header</p>"
            + "</div>";

    PdfDocumentPreparer.PdfRegionDimensions dimensions =
        service.measurePdfRegions(
            html, new TemplateExportService.PdfPageConfig("A3", "portrait"));

    assertThat(dimensions.headerHeightMm()).isBetween(54.9, 55.1);
  }

  @Test
  @DisplayName("resolvePdfPageConfig reads configured A3 landscape properties")
  void resolvePdfPageConfigReadsConfiguredA3LandscapeProperties() {
    TemplateExportService service = newService();
    Task task =
        buildTask(
            403,
            Map.of(
                DomainConstants.Tasks.PROPERTY_DOWNLOAD_FORMAT,
                "pdf",
                DomainConstants.Tasks.PROPERTY_PAGE_SIZE,
                "A3",
                DomainConstants.Tasks.PROPERTY_PAGE_ORIENTATION,
                "landscape"));

    TemplateExportService.PdfPageConfig config = service.resolvePdfPageConfig(task);

    assertThat(config.pageSize()).isEqualTo("A3");
    assertThat(config.pageOrientation()).isEqualTo("landscape");
  }

  @Test
  @DisplayName("resolvePdfPageConfig falls back to defaults for invalid values")
  void resolvePdfPageConfigFallsBackToDefaultsForInvalidValues() {
    TemplateExportService service = newService();
    Task task =
        buildTask(
            404,
            Map.of(
                DomainConstants.Tasks.PROPERTY_DOWNLOAD_FORMAT,
                "pdf",
                DomainConstants.Tasks.PROPERTY_PAGE_SIZE,
                "LETTER",
                DomainConstants.Tasks.PROPERTY_PAGE_ORIENTATION,
                "diagonal"));

    TemplateExportService.PdfPageConfig config = service.resolvePdfPageConfig(task);

    assertThat(config.pageSize()).isEqualTo(TemplateExportService.DEFAULT_PDF_PAGE_SIZE);
    assertThat(config.pageOrientation())
        .isEqualTo(TemplateExportService.DEFAULT_PDF_PAGE_ORIENTATION);
  }

  @Test
  @DisplayName(
      "exportHtml allows pdf task without configured download source when runtime HTML exists")
  void exportHtmlAllowsPdfTaskWithoutConfiguredDownloadSourceWhenRuntimeHtmlExists() {
    TemplateExportService service = newService();
    Task task =
        buildTask(402, Map.of(DomainConstants.Tasks.PROPERTY_DOWNLOAD_FORMAT, "pdf"));

    byte[] content = service.exportHtml("<html><body>pdf</body></html>", "pdf", task);

    assertThat(content).isNotEmpty();
  }

  @Test
  @DisplayName("exportHtml repeats PDF header and footer on every page")
  void exportHtmlRepeatsHeaderAndFooterOnEveryPage() throws Exception {
    TemplateExportService service = newService();
    Task task =
        buildTask(402, Map.of(DomainConstants.Tasks.PROPERTY_DOWNLOAD_FORMAT, "pdf"));
    byte[] content =
        service.exportHtml(
            "<p class=\"sitmun-pdf-header\">Repeated header</p>"
                + "<p class=\"sitmun-pdf-footer\">Repeated footer</p>"
                + "<p>First page</p>"
                + "<p style=\"page-break-before: always\">Second page</p>",
            "pdf",
            task);

    try (PDDocument pdf = PDDocument.load(new ByteArrayInputStream(content))) {
      assertThat(pdf.getNumberOfPages()).isGreaterThanOrEqualTo(2);
      for (int page = 1; page <= pdf.getNumberOfPages(); page++) {
        PDFTextStripper textStripper = new PDFTextStripper();
        textStripper.setStartPage(page);
        textStripper.setEndPage(page);
        assertThat(textStripper.getText(pdf))
            .contains("Repeated header")
            .contains("Repeated footer");
      }
    }
  }

  @Test
  @DisplayName("exportHtml rejects XML output")
  void exportHtmlRejectsXmlOutput() {
    TemplateExportService service = newService();
    Task task =
        buildTask(401, Map.of(DomainConstants.Tasks.PROPERTY_DOWNLOAD_FORMAT, "pdf"));

    assertThatThrownBy(() -> service.exportHtml("<xml/>", "xml", task))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(
            exception -> {
              ResponseStatusException responseStatusException = (ResponseStatusException) exception;
              assertThat(responseStatusException.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
              assertThat(responseStatusException.getReason())
                  .isEqualTo("Unsupported template export output: xml");
            });
  }

  @Test
  @DisplayName("resolveExportFilename prefers template task name and uses report fallback")
  void resolveExportFilenamePrefersTemplateTaskNameAndUsesReportFallback() {
    TemplateExportService service = newService();
    Task exportTask = buildTask(901, Map.of());
    Task customExportTask = buildTaskWithName(902, "Quarterly:/Report?*", Map.of());
    Task templateTask = buildTaskWithName(903, "Plantilla territori", Map.of());

    assertThat(service.resolveExportFilename(null, null, "pdf")).isEqualTo("report.pdf");
    assertThat(service.resolveExportFilename(null, exportTask, "pdf")).isEqualTo("Export task.pdf");
    assertThat(service.resolveExportFilename(null, customExportTask, "pdf"))
        .isEqualTo("Quarterly_Report_.pdf");
    assertThat(service.resolveExportFilename(templateTask, exportTask, "pdf"))
        .isEqualTo("Plantilla territori.pdf");
  }

  @Test
  @DisplayName("exportHtml rejects unsupported output formats")
  void exportHtmlRejectsUnsupportedOutputFormats() {
    TemplateExportService service = newService();

    assertThatThrownBy(() -> service.exportHtml("content", "csv", null))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Unsupported template export output");
  }

  @Test
  @DisplayName("exportHtml rejects oversized runtime sources")
  void exportHtmlRejectsOversizedRuntimeSources() {
    TemplateExportService service = newService();
    String source = "x".repeat(DomainConstants.Tasks.MAX_TEMPLATE_EXPORT_SOURCE_CHARACTERS + 1);

    assertThatThrownBy(() -> service.exportHtml(source, "pdf", null))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Export source is too large");
  }

  @Test
  @DisplayName("exportHtml rejects documents above the DOM complexity limit")
  void exportHtmlRejectsExcessiveDomComplexity() {
    TemplateExportService service = newService();
    Task task =
        buildTask(406, Map.of(DomainConstants.Tasks.PROPERTY_DOWNLOAD_FORMAT, "pdf"));
    String html =
        "<i></i>".repeat(DomainConstants.Tasks.MAX_TEMPLATE_EXPORT_DOM_ELEMENTS + 1);

    assertThatThrownBy(() -> service.exportHtml(html, "pdf", task))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Export document is too complex");
  }

  @Test
  @DisplayName("PDF resources allow data and public web schemes only")
  void pdfResourcesAllowDataAndPublicWebSchemesOnly() {
    TemplateExportService service = newService();

    assertThat(service.isAllowedResource("data:image/png;base64,AA==")).isTrue();
    assertThat(service.isAllowedResource("http://example.org/logo.png")).isTrue();
    assertThat(service.isAllowedResource("https://example.org/logo.png")).isTrue();
    assertThat(service.isAllowedResource("HTTPS://cdn.example.org/font.woff2")).isTrue();
    assertThat(service.isAllowedResource("file:///etc/passwd")).isFalse();
    assertThat(service.isAllowedResource("ftp://example.org/logo.png")).isFalse();
    assertThat(service.isAllowedResource("not a uri")).isFalse();
  }

  @Test
  @DisplayName("PDF resources decode bounded base64 data URLs")
  void pdfResourcesDecodeBase64DataUrls() {
    TemplateExportService service = newService();

    assertThat(service.fetchExternalResource("data:image/png;base64,aW1hZ2U="))
        .isEqualTo("image".getBytes(StandardCharsets.UTF_8));
    assertThat(service.fetchExternalResource("data:image/svg+xml,%3Csvg%3E%2B%3C%2Fsvg%3E"))
        .isEqualTo("<svg>+</svg>".getBytes(StandardCharsets.UTF_8));
    assertThat(service.fetchExternalResource("data:image/png,%89PNG"))
        .isEqualTo(new byte[] {(byte) 0x89, 'P', 'N', 'G'});
    assertThat(service.fetchExternalResource("data:image/png;base64,not-base64")).isEmpty();
  }

  @Test
  @DisplayName("PDF resources load a hostname only when every resolved IP is public")
  void pdfResourcesRequireEveryResolvedIpToBePublic() throws Exception {
    AtomicInteger requests = new AtomicInteger();
    TemplateExportService publicService =
        serviceWith(
            host -> List.of(InetAddress.getByName("93.184.216.34")),
            request -> successResponse(request, "image"));
    TemplateExportService mixedService =
        serviceWith(
            host ->
                List.of(
                    InetAddress.getByName("93.184.216.34"),
                    InetAddress.getByName("10.0.0.1")),
            request -> {
              requests.incrementAndGet();
              return successResponse(request, "blocked");
            });

    assertThat(publicService.fetchExternalResource("https://public.test/image"))
        .isEqualTo("image".getBytes(StandardCharsets.UTF_8));
    assertThat(mixedService.fetchExternalResource("https://mixed.test/image")).isEmpty();
    assertThat(requests).hasValue(0);
  }

  @Test
  @DisplayName("PDF resources block direct loopback and private DNS results")
  void pdfResourcesBlockLoopbackAndPrivateDnsResults() throws Exception {
    AtomicInteger requests = new AtomicInteger();
    TemplateExportService service =
        serviceWith(
            host -> List.of(InetAddress.getByName("10.0.0.5")),
            request -> {
              requests.incrementAndGet();
              return successResponse(request, "blocked");
            });

    assertThat(service.fetchExternalResource("http://private.test/image")).isEmpty();
    assertThat(newService().fetchExternalResource("http://127.0.0.1/image")).isEmpty();
    assertThat(requests).hasValue(0);
  }

  @Test
  @DisplayName("PDF resources validate private redirect targets before connecting")
  void pdfResourcesBlockPrivateRedirectTargets() throws Exception {
    AtomicInteger requests = new AtomicInteger();
    Dns dns =
        host ->
            List.of(
                InetAddress.getByName(
                    "private.test".equals(host) ? "192.168.1.10" : "93.184.216.34"));
    TemplateExportService service =
        serviceWith(
            dns,
            request -> {
              requests.incrementAndGet();
              return redirectResponse(request, "http://private.test/secret");
            });

    assertThat(service.fetchExternalResource("https://public.test/start")).isEmpty();
    assertThat(requests).hasValue(1);
  }

  @Test
  @DisplayName("PDF resources stop after the explicit redirect limit")
  void pdfResourcesStopAfterRedirectLimit() throws Exception {
    AtomicInteger requests = new AtomicInteger();
    TemplateExportService service =
        serviceWith(
            host -> List.of(InetAddress.getByName("93.184.216.34")),
            request -> {
              requests.incrementAndGet();
              return redirectResponse(request, "/again");
            });

    assertThat(service.fetchExternalResource("https://public.test/start")).isEmpty();
    assertThat(requests).hasValue(6);
  }

  @Test
  @DisplayName("PDF resource policy blocks reserved IPv4 and IPv6 ranges")
  void pdfResourcePolicyBlocksReservedIpRanges() throws Exception {
    assertThat(TemplateExportService.isPubliclyRoutable(InetAddress.getByName("8.8.8.8")))
        .isTrue();
    assertThat(TemplateExportService.isPubliclyRoutable(InetAddress.getByName("100.64.0.1")))
        .isFalse();
    assertThat(TemplateExportService.isPubliclyRoutable(InetAddress.getByName("192.0.2.1")))
        .isFalse();
    assertThat(TemplateExportService.isPubliclyRoutable(InetAddress.getByName("2001:4860:4860::8888")))
        .isTrue();
    assertThat(TemplateExportService.isPubliclyRoutable(InetAddress.getByName("fc00::1")))
        .isFalse();
    assertThat(TemplateExportService.isPubliclyRoutable(InetAddress.getByName("2001:db8::1")))
        .isFalse();
    assertThat(TemplateExportService.isPubliclyRoutable(InetAddress.getByName("64:ff9b::a00:1")))
        .isFalse();
    assertThat(TemplateExportService.isPubliclyRoutable(InetAddress.getByName("64:ff9b:1::a00:1")))
        .isFalse();

    byte[] mapped = new byte[16];
    mapped[10] = (byte) 0xff;
    mapped[11] = (byte) 0xff;
    mapped[12] = 8;
    mapped[13] = 8;
    mapped[14] = 8;
    mapped[15] = 8;
    InetAddress mappedAddress = Inet6Address.getByAddress(null, mapped, -1);
    assertThat(TemplateExportService.isPubliclyRoutable(mappedAddress)).isFalse();
  }

  @Test
  @DisplayName("exportHtml rejects PDFs above the page limit")
  void exportHtmlRejectsPdfAbovePageLimit() {
    TemplateExportService service = newService();
    Task task =
        buildTask(405, Map.of(DomainConstants.Tasks.PROPERTY_DOWNLOAD_FORMAT, "pdf"));
    String page = "<p style=\"page-break-before: always\">Page</p>";
    String html = page.repeat(DomainConstants.Tasks.MAX_TEMPLATE_EXPORT_PDF_PAGES + 1);

    assertThatThrownBy(() -> service.exportHtml(html, "pdf", task))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Export PDF has too many pages");
  }

  private static Task buildTask(int id, Map<String, Object> properties) {
    return buildTaskWithName(id, "Export task", properties);
  }

  private TemplateExportService newService() {
    return new TemplateExportService();
  }

  private TemplateExportService serviceWith(
      Dns dns, java.util.function.Function<okhttp3.Request, Response> responder) {
    OkHttpClient client =
        new OkHttpClient.Builder()
            .addInterceptor(chain -> responder.apply(chain.request()))
            .build();
    return new TemplateExportService(dns, client);
  }

  private static Response successResponse(okhttp3.Request request, String body) {
    return responseBuilder(request, 200)
        .body(ResponseBody.create(body, MediaType.parse("application/octet-stream")))
        .build();
  }

  private static Response redirectResponse(okhttp3.Request request, String location) {
    return responseBuilder(request, 302).header("Location", location).build();
  }

  private static Response.Builder responseBuilder(okhttp3.Request request, int status) {
    return new Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(status)
        .message(String.valueOf(status));
  }

  private static Task buildTaskWithName(int id, String name, Map<String, Object> properties) {
    Task task = new Task();
    task.setId(id);
    task.setName(name);
    task.setProperties(properties);
    return task;
  }
}
