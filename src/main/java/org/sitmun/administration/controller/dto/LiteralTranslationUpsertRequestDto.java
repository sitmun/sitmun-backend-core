package org.sitmun.administration.controller.dto;

import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LiteralTranslationUpsertRequestDto {

  private String literal;
  private String translation;
  private String language;
  private String sourceLanguage;
  private Map<String, String> translations;
}
