package org.sitmun.infrastructure.persistence.type.i18n;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/** Projections for REST views of translation. */
@Projection(name = "view", types = Translation.class)
public interface TranslationProjection {

  @Value("#{target.id}")
  Integer getId();

  /** Row identifier. */
  @Value("#{target.element}")
  Integer getElement();

  /** Table and Column identifier. */
  @Value("#{target.column}")
  String getColumn();

  /** Translation language. */
  @Value("#{target.language?.name}")
  String getLanguageName();

  /** Translation language. */
  @Value("#{target.language?.shortname}")
  String getLanguageShortname();

  /** Translation. */
  @Value("#{target.translation}")
  String getTranslation();
}
