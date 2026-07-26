package org.sitmun.administration.controller.dto;

public record LiteralTranslationCsvImportErrorDto(
    long rowNumber, String sourceLanguage, String literal, String message) {}
