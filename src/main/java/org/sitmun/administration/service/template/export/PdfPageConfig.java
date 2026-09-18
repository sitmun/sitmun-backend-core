package org.sitmun.administration.service.template.export;

/** Resolved PDF page settings shared by preparation and rendering. */
class PdfPageConfig {
  private final String pageSize;
  private final String pageOrientation;

  PdfPageConfig(String pageSize, String pageOrientation) {
    this.pageSize = pageSize;
    this.pageOrientation = pageOrientation;
  }

  String pageSize() {
    return pageSize;
  }

  String pageOrientation() {
    return pageOrientation;
  }
}
