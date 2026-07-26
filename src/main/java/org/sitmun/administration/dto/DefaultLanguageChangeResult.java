package org.sitmun.administration.dto;

import java.util.List;

/**
 * Result of a successful default language change operation.
 *
 * @param previousDefault Previous database default language
 * @param currentDefault New database default language
 * @param backupUpserts Number of translation rows created/updated to back up previous default
 *     values
 * @param restoredValues Number of main-table values restored from target translations
 * @param preservedValues Number of main-table values preserved because target translations were
 *     missing
 * @param preservedMissing List of preserved values (only populated when preservedValues > 0)
 */
public record DefaultLanguageChangeResult(
    String previousDefault,
    String currentDefault,
    int backupUpserts,
    int restoredValues,
    int preservedValues,
    List<MissingTranslationDto> preservedMissing) {}
