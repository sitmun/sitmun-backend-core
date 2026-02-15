package org.sitmun.infrastructure.persistence.type.i18n;

/** Scalar projection for bulk-loading translations without hydrating entities. */
public record TranslationRow(Integer element, String column, String translation) {}
