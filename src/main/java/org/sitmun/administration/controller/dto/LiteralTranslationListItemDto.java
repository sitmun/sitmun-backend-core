package org.sitmun.administration.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LiteralTranslationListItemDto {

  private final Integer id;
  private final String literal;
  private final String translation;
  private final String sourceLanguage;
  private final Boolean complete;
}
