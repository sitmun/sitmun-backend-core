package org.sitmun.administration.dto;

/**
 * Request to change the database default language.
 *
 * @param from Source language shortname (BCP-47 tag)
 * @param to Target language shortname (BCP-47 tag)
 * @param continueOnMissingTranslations If true, preserve current main-table values where target
 *     translations are missing; if false, block the change when translations are incomplete
 */
public record DefaultLanguageChangeRequest(
    String from, String to, boolean continueOnMissingTranslations) {}
