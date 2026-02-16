package org.sitmun.infrastructure.persistence.type.i18n;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Test configuration that provides i18n beans for @DataJpaTest (TranslationService, I18nListener).
 */
@Configuration
@Import({TranslationService.class, I18nListener.class})
public class I18nTestConfiguration {}
