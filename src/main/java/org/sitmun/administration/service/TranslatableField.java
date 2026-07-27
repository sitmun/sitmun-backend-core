package org.sitmun.administration.service;

/**
 * Metadata for a translatable field in an entity table.
 *
 * @param entity Entity label used in STM_TRANSLATION.TRA_COLUMN (e.g., "Language", "Application")
 * @param table Database table name
 * @param idColumn Primary key column name
 * @param field Field name in the entity
 * @param column Database column name for the translatable field
 */
record TranslatableField(
    String entity, String table, String idColumn, String field, String column) {

  /** Returns the translation column identifier (e.g., "Language.name"). */
  String translationColumn() {
    return entity + "." + field;
  }
}
