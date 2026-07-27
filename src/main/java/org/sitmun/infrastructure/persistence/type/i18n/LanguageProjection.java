package org.sitmun.infrastructure.persistence.type.i18n;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/** Projections for REST views of a language. */
@Projection(name = "view", types = Language.class)
public interface LanguageProjection {

  @Value("#{target.id}")
  Integer getId();

  /** BCP 47 language tag. */
  @Value("#{target.shortname}")
  String getShortname();

  /** Endonym (own name of the language). */
  @Value("#{target.name}")
  String getName();

  /** Locale label for the current {@code ?lang=} (read-only; never persisted). */
  @Value("#{target.translatedName}")
  String getTranslatedName();

  /** Display order in language selectors (lower first). */
  @Value("#{target.order}")
  Integer getOrder();

  /** Whether the language is available for UI locale selection. */
  @Value("#{target.enabled}")
  Boolean getEnabled();
}
