package org.sitmun.infrastructure.persistence.type.i18n;

import org.sitmun.administration.controller.dto.LiteralTranslationListItemDto;
import org.sitmun.administration.service.i18n.LiteralTranslationFilterModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LiteralTranslationQuerydslRepository {

  Page<LiteralTranslationListItemDto> findPageByLanguage(
      String language, LiteralTranslationFilterModel filter, Pageable pageable);
}
