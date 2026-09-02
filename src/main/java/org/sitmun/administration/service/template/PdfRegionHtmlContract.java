package org.sitmun.administration.service.template;

import java.util.Set;

/** Shared HTML contract for PDF header and footer regions. */
public final class PdfRegionHtmlContract {

  public static final String HEADER_CLASS = "sitmun-pdf-header";
  public static final String FOOTER_CLASS = "sitmun-pdf-footer";
  public static final String PAGE_NUMBER_CLASS = "sitmun-pdf-page-number";
  public static final String FULL_BLEED_HEADER_CLASS = "sitmun-pdf-header-full-bleed";
  public static final String FULL_BLEED_FOOTER_CLASS = "sitmun-pdf-footer-full-bleed";
  public static final String TEMPLATE_SCOPE_ATTRIBUTE = "data-sitmun-pdf-template-scope";
  public static final String ROOT_TEMPLATE_SCOPE = "root";
  public static final String NESTED_TEMPLATE_SCOPE = "nested";
  public static final Set<String> HEADER_CLASSES = Set.of(HEADER_CLASS, FULL_BLEED_HEADER_CLASS);
  public static final Set<String> FOOTER_CLASSES = Set.of(FOOTER_CLASS, FULL_BLEED_FOOTER_CLASS);
  public static final Set<String> REGION_CLASSES =
      Set.of(HEADER_CLASS, FOOTER_CLASS, FULL_BLEED_HEADER_CLASS, FULL_BLEED_FOOTER_CLASS);
  public static final Set<String> SUPPORTED_TAGS =
      Set.of("p", "h1", "h2", "h3", "h4", "h5", "h6", "ul", "ol", "table", "img");

  private PdfRegionHtmlContract() {}
}
