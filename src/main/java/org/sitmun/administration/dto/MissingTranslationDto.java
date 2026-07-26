package org.sitmun.administration.dto;

/**
 * Description of a missing translation for preview and result reporting.
 *
 * @param entity Entity label (e.g., "Application", "Language")
 * @param element Entity ID
 * @param column Translation column (e.g., "Application.name")
 * @param currentValue Current value in the main table
 */
public record MissingTranslationDto(
    String entity, Integer element, String column, String currentValue) {}
