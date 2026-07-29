package org.sitmun.administration.dto;

import java.util.List;

/**
 * Preview of a default language change operation.
 *
 * @param currentDefault Current database default language
 * @param requestedDefault Requested new default language
 * @param affectedValues Total number of localized values across all entities
 * @param backupUpserts Number of translation rows that will be created/updated to back up current
 *     values
 * @param restoredValues Number of main-table values that will be restored from target translations
 * @param missingTranslations Number of target translations that are missing
 * @param missing Detailed list of missing target translations
 * @param literalContinuitySeeds Number of dictionary literals that will receive a continuity value
 *     for the new default language
 */
public record DefaultLanguageChangePreview(
    String currentDefault,
    String requestedDefault,
    int affectedValues,
    int backupUpserts,
    int restoredValues,
    int missingTranslations,
    List<MissingTranslationDto> missing,
    int literalContinuitySeeds) {}
