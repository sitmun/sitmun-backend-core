package org.sitmun.administration.service.template.export;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.sitmun.domain.DomainConstants;
import org.sitmun.domain.task.Task;

/** Resolves and validates page options independently from PDF rendering. */
final class PdfPageOptionsPolicy {
  static final String DEFAULT_PAGE_SIZE = "A4";
  static final String DEFAULT_ORIENTATION = "portrait";
  private static final Set<String> ALLOWED_PAGE_SIZES = Set.of("A3", "A4");
  private static final Set<String> ALLOWED_ORIENTATIONS = Set.of("portrait", "landscape");

  PdfPageConfig resolve(Task exportTask) {
    if (exportTask == null) {
      return defaults();
    }
    Map<String, Object> properties = exportTask.getProperties();
    return new PdfPageConfig(
        normalizePageSize(properties == null ? null : properties.get(DomainConstants.Tasks.PROPERTY_PAGE_SIZE)),
        normalizeOrientation(properties == null ? null : properties.get(DomainConstants.Tasks.PROPERTY_PAGE_ORIENTATION)));
  }

  static PdfPageConfig defaults() {
    return new PdfPageConfig(DEFAULT_PAGE_SIZE, DEFAULT_ORIENTATION);
  }

  static String normalizePageSize(Object value) {
    if (value == null) return DEFAULT_PAGE_SIZE;
    String normalized = String.valueOf(value).trim().toUpperCase(Locale.ROOT);
    return ALLOWED_PAGE_SIZES.contains(normalized) ? normalized : DEFAULT_PAGE_SIZE;
  }

  static String normalizeOrientation(Object value) {
    if (value == null) return DEFAULT_ORIENTATION;
    String normalized = String.valueOf(value).trim().toLowerCase(Locale.ROOT);
    return ALLOWED_ORIENTATIONS.contains(normalized) ? normalized : DEFAULT_ORIENTATION;
  }
}
