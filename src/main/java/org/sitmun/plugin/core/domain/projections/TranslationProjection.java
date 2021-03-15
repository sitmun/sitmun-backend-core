package org.sitmun.plugin.core.domain.projections;

import org.sitmun.plugin.core.domain.Translation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/**
 * Projections for REST views of translation.
 */
@Projection(name = "view", types = {Translation.class})
public interface TranslationProjection {

  @Value("#{target.id}")
  Integer getId();

  /**
   * Row identifier.
   */
  @Value("#{target.element}")
  Integer getElement();

  /**
   * Table and Column identifier.
   */
  @Value("#{target.column}")
  String getColumn();

  /**
   * Translation language.
   */
  @Value("#{target.language?.name}")
  String getLanguageName();

  /**
   * Translation.
   */
  @Value("#{target.translation}")
  String getTranslation();
}
