package org.sitmun.administration.controller.dto;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LiteralTranslationCsvExportRequestDto {
  private String targetLanguage;
  private List<Long> literalIds;
  private String fileName;
}
