package org.sitmun.administration.service.template.export;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.jsoup.Jsoup;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.sitmun.administration.service.template.PdfRegionHtmlContract;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Prepares rendered HTML and paged-media CSS for OpenHTMLToPDF. */
final class PdfDocumentPreparer {

  static final double HEADER_TOP_SPACING_MM = 5;
  static final String RUNNING_HEADER_CLASS = "sitmun-pdf-running-header";
  static final String RUNNING_FOOTER_CLASS = "sitmun-pdf-running-footer";
  static final String MEASUREMENT_ATTRIBUTE = "data-sitmun-pdf-measure";
  static final String MEASUREMENT_HEADER = "header";
  static final String MEASUREMENT_FOOTER = "footer";
  private static final String FULL_BLEED_RUNNING_CLASS = "sitmun-pdf-running-full-bleed";
  private static final String CONTAINED_RUNNING_CLASS = "sitmun-pdf-running-contained";
  private static final String SYNTHETIC_CONTEXT_CLASS = "sitmun-pdf-running-context";
  private static final Set<String> TABLE_CELL_TAGS = Set.of("td", "th");

  private PdfDocumentPreparer() {}

  static Document prepare(
      String html, PdfPageConfig pageConfig, PdfRegionDimensions dimensions) {
    return prepare(Jsoup.parse(html == null ? "" : html), pageConfig, dimensions);
  }

  static Document prepare(
      Document document,
      PdfPageConfig pageConfig,
      PdfRegionDimensions dimensions) {
    PdfPageConfig effectivePageConfig = effectivePageConfig(pageConfig);
    PdfRegionDimensions effectiveDimensions =
        dimensions == null ? new PdfRegionDimensions(0, 0) : dimensions;
    RunningRegions regions = extractRunningRegions(document);
    addDocumentMetadata(document, buildFinalCss(effectivePageConfig, effectiveDimensions, regions));
    prependRunningRegions(document, regions);
    return document;
  }

  static MeasurementDocument prepareForMeasurement(
      String html, PdfPageConfig pageConfig) {
    return prepareForMeasurement(Jsoup.parse(html == null ? "" : html), pageConfig);
  }

  static MeasurementDocument prepareForMeasurement(
      Document document, PdfPageConfig pageConfig) {
    RunningRegions regions = extractRunningRegions(document);
    document.body().select("style, link[rel=stylesheet]").stream()
        .map(Element::clone)
        .forEach(document.head()::appendChild);
    document.body().empty();
    if (regions.header() != null) {
      regions.header().wrapper().attr(MEASUREMENT_ATTRIBUTE, MEASUREMENT_HEADER);
      document.body().appendChild(regions.header().wrapper());
    }
    if (regions.footer() != null) {
      regions.footer().wrapper().attr(MEASUREMENT_ATTRIBUTE, MEASUREMENT_FOOTER);
      document.body().appendChild(regions.footer().wrapper());
    }
    addDocumentMetadata(document, buildMeasurementCss(effectivePageConfig(pageConfig), regions));
    return new MeasurementDocument(
        document,
        regions.header() != null,
        regions.footer() != null,
        regions.header() != null && !regions.header().fullBleed());
  }

  private static PdfPageConfig effectivePageConfig(PdfPageConfig pageConfig) {
    return pageConfig == null
        ? new PdfPageConfig(PdfPageOptionsPolicy.DEFAULT_PAGE_SIZE,
            PdfPageOptionsPolicy.DEFAULT_ORIENTATION)
        : pageConfig;
  }

  private static RunningRegions extractRunningRegions(Document document) {
    List<Element> headers = selectRegions(document, PdfRegionHtmlContract.HEADER_CLASSES);
    List<Element> footers = selectRegions(document, PdfRegionHtmlContract.FOOTER_CLASSES);
    validateRegions(headers, footers);

    Element header = resolveRegion(headers, "header");
    Element footer = resolveRegion(footers, "footer");
    boolean fullBleedHeader =
        header != null && header.hasClass(PdfRegionHtmlContract.FULL_BLEED_HEADER_CLASS);
    boolean fullBleedFooter =
        footer != null && footer.hasClass(PdfRegionHtmlContract.FULL_BLEED_FOOTER_CLASS);

    return new RunningRegions(
        createRunningRegion(document, header, RUNNING_HEADER_CLASS, fullBleedHeader),
        createRunningRegion(document, footer, RUNNING_FOOTER_CLASS, fullBleedFooter));
  }

  private static List<Element> selectRegions(Document document, Set<String> classes) {
    if (classes.isEmpty()) {
      return List.of();
    }
    return new ArrayList<>(
        document
            .body()
            .select(
                classes.stream()
                    .map(className -> "." + className)
                    .collect(Collectors.joining(", "))));
  }

  private static RunningRegion createRunningRegion(
      Document document, Element region, String runningClass, boolean fullBleed) {
    if (region == null) {
      return null;
    }
    if (fullBleed) {
      region.remove();
      Element wrapper = region.addClass(runningClass).addClass(FULL_BLEED_RUNNING_CLASS);
      return new RunningRegion(wrapper, true);
    }

    Element runningRegion = region;
    Element pathElement = region;
    Element ancestor = region.parent();
    region.remove();

    while (ancestor != null && ancestor != document.body()) {
      Element parent = ancestor.parent();
      Element wrapper = ancestor.shallowClone().addClass(SYNTHETIC_CONTEXT_CLASS);
      appendWithTableLayoutContext(ancestor, pathElement, wrapper, runningRegion);
      runningRegion = wrapper;
      pathElement = ancestor;
      ancestor = parent;
    }

    Element wrapper = runningRegion.addClass(runningClass).addClass(CONTAINED_RUNNING_CLASS);
    return new RunningRegion(wrapper, false);
  }

  private static void prependRunningRegions(Document document, RunningRegions regions) {
    if (regions.footer() != null) {
      document.body().prependChild(regions.footer().wrapper());
    }
    if (regions.header() != null) {
      document.body().prependChild(regions.header().wrapper());
    }
  }

  private static void appendWithTableLayoutContext(
      Element source, Element pathElement, Element target, Element runningRegion) {
    if ("tr".equals(source.normalName())) {
      for (Element child : source.children()) {
        if (child == pathElement) {
          target.appendChild(runningRegion);
        } else if (TABLE_CELL_TAGS.contains(child.normalName())) {
          target.appendChild(child.shallowClone());
        }
      }
      return;
    }

    if ("table".equals(source.normalName())) {
      for (Element child : source.children()) {
        if (child == pathElement) {
          target.appendChild(runningRegion);
        } else if ("colgroup".equals(child.normalName())) {
          target.appendChild(child.clone());
        }
      }
      return;
    }

    target.appendChild(runningRegion);
  }

  private static void validateRegions(List<Element> headers, List<Element> footers) {
    List<Element> regions = new ArrayList<>(headers);
    footers.stream().filter(region -> !regions.contains(region)).forEach(regions::add);
    if (regions.stream().anyMatch(PdfDocumentPreparer::hasConflictingRegionClasses)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "The same element cannot have multiple PDF region types");
    }
    if (regions.stream().anyMatch(element -> !isValidRegion(element))) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "PDF headers and footers must be supported blocks and cannot contain each other");
    }
  }

  private static Element resolveRegion(List<Element> candidates, String regionName) {
    if (candidates.isEmpty()) {
      return null;
    }

    List<Element> rootCandidates = candidates.stream()
        .filter(candidate -> PdfRegionHtmlContract.ROOT_TEMPLATE_SCOPE.equals(
            candidate.attr(PdfRegionHtmlContract.TEMPLATE_SCOPE_ATTRIBUTE)))
        .toList();
    if (rootCandidates.size() > 1) {
      throw duplicateRegionException(regionName);
    }
    if (rootCandidates.size() == 1) {
      Element selected = rootCandidates.get(0);
      candidates.stream().filter(candidate -> candidate != selected).forEach(Element::remove);
      return selected;
    }

    if (candidates.size() > 1) {
      throw duplicateRegionException(regionName);
    }
    return candidates.get(0);
  }

  private static ResponseStatusException duplicateRegionException(String regionName) {
    return new ResponseStatusException(
        HttpStatus.BAD_REQUEST, "Only one PDF " + regionName + " is allowed");
  }

  private static boolean hasConflictingRegionClasses(Element element) {
    return PdfRegionHtmlContract.REGION_CLASSES.stream().filter(element::hasClass).count() > 1;
  }

  private static boolean isValidRegion(Element element) {
    return PdfRegionHtmlContract.SUPPORTED_TAGS.contains(element.tagName().toLowerCase(Locale.ROOT))
        && element.parents().stream()
            .noneMatch(
                parent -> PdfRegionHtmlContract.REGION_CLASSES.stream().anyMatch(parent::hasClass));
  }

  private static void addDocumentMetadata(Document document, String css) {
    document.head().appendElement("meta").attr("charset", "UTF-8");
    document
        .head()
        .appendElement("style")
        .attr("data-sitmun-pdf-layout", "true")
        .appendChild(new DataNode(css));
  }

  private static String buildMeasurementCss(
      PdfPageConfig pageConfig, RunningRegions regions) {
    return "@page { size: %s %s; margin: 0; }%n"
            .formatted(pageConfig.pageSize(), pageConfig.pageOrientation())
        + "html, body { margin: 0 !important; padding: 0 !important; }\n"
        + buildRegionCss(regions, false)
        + "table, tr { page-break-inside: avoid; }";
  }

  private static String buildFinalCss(
      PdfPageConfig pageConfig,
      PdfRegionDimensions dimensions,
      RunningRegions regions) {
    if (regions.header() == null && regions.footer() == null) {
      return "@page { size: %s %s; margin: 0; }%n"
              .formatted(pageConfig.pageSize(), pageConfig.pageOrientation())
          + "table, tr { page-break-inside: avoid; }";
    }

    String topMargin =
        regions.header() == null ? "0" : formatMillimeters(dimensions.headerHeightMm()) + "mm";
    String bottomMargin =
        regions.footer() == null ? "0" : formatMillimeters(dimensions.footerHeightMm()) + "mm";
    StringBuilder css =
        new StringBuilder()
            .append("@page {\n")
            .append("  size: ")
            .append(pageConfig.pageSize())
            .append(' ')
            .append(pageConfig.pageOrientation())
            .append(";\n")
            .append("  margin: ")
            .append(topMargin)
            .append(" 0 ")
            .append(bottomMargin)
            .append(" 0;\n");
    if (regions.header() != null) {
      css.append("  @top-center { content: element(sitmunPdfHeader); vertical-align: top; }\n");
    }
    if (regions.footer() != null) {
      css.append(
          "  @bottom-center { content: element(sitmunPdfFooter); vertical-align: bottom; }\n");
    }
    return css.append("}\n")
        .append(buildRegionCss(regions, true))
        .append("table, tr { page-break-inside: avoid; }")
        .toString();
  }

  private static String buildRegionCss(RunningRegions regions, boolean running) {
    StringBuilder css = new StringBuilder();
    if (regions.header() != null) {
      css.append('.').append(RUNNING_HEADER_CLASS).append(" { ");
      if (running) {
        css.append("position: running(sitmunPdfHeader) !important; ");
      }
      if (!regions.header().fullBleed()) {
        css.append("margin-top: ");
        css.append(running ? formatMillimeters(HEADER_TOP_SPACING_MM) + "mm" : "0");
        css.append(" !important; ");
      }
      css.append("}\n");
    }
    if (regions.footer() != null) {
      css.append('.').append(RUNNING_FOOTER_CLASS).append(" { ");
      if (running) {
        css.append("position: running(sitmunPdfFooter) !important; ");
      }
      css.append("}\n");
      css.append('.')
          .append(RUNNING_FOOTER_CLASS)
          .append(" .")
          .append(PdfRegionHtmlContract.PAGE_NUMBER_CLASS)
          .append(" { margin: 0; font-size: 0; }\n")
          .append('.')
          .append(RUNNING_FOOTER_CLASS)
          .append(" .")
          .append(PdfRegionHtmlContract.PAGE_NUMBER_CLASS)
          .append("::before { content: counter(page); font-size: 10pt; }\n");
    }
    if ((regions.header() != null && regions.header().fullBleed())
        || (regions.footer() != null && regions.footer().fullBleed())) {
      css.append('.')
          .append(FULL_BLEED_RUNNING_CLASS)
          .append(" { width: 100% !important; max-width: none !important;")
          .append(" box-sizing: border-box !important; margin: 0 !important; }\n");
    }
    if ((regions.header() != null && !regions.header().fullBleed())
        || (regions.footer() != null && !regions.footer().fullBleed())) {
      css.append('.')
          .append(CONTAINED_RUNNING_CLASS)
          .append(" { width: 100% !important; max-width: none !important;")
          .append(" box-sizing: border-box !important; }\n");
      css.append('.')
          .append(SYNTHETIC_CONTEXT_CLASS)
          .append(" { height: auto !important; min-height: 0 !important;")
          .append(" max-height: none !important; }\n");
    }
    return css.toString();
  }

  private static String formatMillimeters(double value) {
    return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
  }

  record PdfRegionDimensions(double headerHeightMm, double footerHeightMm) {}

  record MeasurementDocument(
      Document document, boolean hasHeader, boolean hasFooter, boolean headerHasTopSpacing) {}

  private record RunningRegion(Element wrapper, boolean fullBleed) {}

  private record RunningRegions(RunningRegion header, RunningRegion footer) {}
}
