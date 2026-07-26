package org.sitmun.administration.controller.dto;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class LiteralTranslationCsvImportResponseDto {
  String targetLanguage;
  long totalRows;
  long createdLiterals;
  long createdTranslations;
  long updatedTranslations;
  long emptiedTranslations;
  long unchangedRows;
  long existingKeysNotInCsv;
  long emptyValueRows;
  long failedRows;
  List<String> sourceLanguages;
  List<LiteralTranslationCsvImportErrorDto> errors;
}
