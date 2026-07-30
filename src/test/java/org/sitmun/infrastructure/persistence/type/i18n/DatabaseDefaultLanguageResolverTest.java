package org.sitmun.infrastructure.persistence.type.i18n;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sitmun.SitmunConstants;
import org.sitmun.domain.configuration.ConfigurationParameter;
import org.sitmun.domain.configuration.ConfigurationParameterRepository;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@ExtendWith(MockitoExtension.class)
@DisplayName("DatabaseDefaultLanguageResolver")
class DatabaseDefaultLanguageResolverTest {

  @Mock private ConfigurationParameterRepository configurationParameterRepository;
  @Mock private LanguageRepository languageRepository;

  private DatabaseDefaultLanguageResolver resolver;
  private MockHttpServletRequest request;

  @BeforeEach
  void setUp() {
    resolver =
        new DatabaseDefaultLanguageResolver(
            configurationParameterRepository, languageRepository, "en");
    request = new MockHttpServletRequest();
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
  }

  @AfterEach
  void tearDown() {
    TranslationCache.removeRequestAttribute(request);
    RequestContextHolder.resetRequestAttributes();
  }

  @Test
  @DisplayName("uses language.default from configuration when present")
  void usesConfiguredDefault() {
    when(configurationParameterRepository.findByName(SitmunConstants.LANGUAGE_DEFAULT_CONF_KEY))
        .thenReturn(
            Optional.of(
                ConfigurationParameter.builder()
                    .name(SitmunConstants.LANGUAGE_DEFAULT_CONF_KEY)
                    .value("ca")
                    .build()));

    assertThat(resolver.resolveShortname()).isEqualTo("ca");
  }

  @Test
  @DisplayName("falls back to sitmun.language property when config missing")
  void fallsBackToProperty() {
    when(configurationParameterRepository.findByName(SitmunConstants.LANGUAGE_DEFAULT_CONF_KEY))
        .thenReturn(Optional.empty());

    assertThat(resolver.resolveShortname()).isEqualTo("en");
  }

  @Test
  @DisplayName("requireLanguage resolves STM_LANGUAGE row")
  void requireLanguage() {
    when(configurationParameterRepository.findByName(SitmunConstants.LANGUAGE_DEFAULT_CONF_KEY))
        .thenReturn(
            Optional.of(
                ConfigurationParameter.builder()
                    .name(SitmunConstants.LANGUAGE_DEFAULT_CONF_KEY)
                    .value("ca")
                    .build()));
    Language ca = Language.builder().id(3).shortname("ca").name("Català").build();
    when(languageRepository.findByShortname("ca")).thenReturn(Optional.of(ca));

    assertThat(resolver.requireLanguage()).isSameAs(ca);
  }

  @Test
  @DisplayName("requireLanguage fails when language row missing")
  void requireLanguageMissing() {
    when(configurationParameterRepository.findByName(SitmunConstants.LANGUAGE_DEFAULT_CONF_KEY))
        .thenReturn(Optional.empty());
    when(languageRepository.findByShortname("en")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> resolver.requireLanguage())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("en");
  }

  @Test
  @DisplayName("resolveShortname uses request cache and does not query STM_CONF")
  void resolveShortnameUsesRequestCache() {
    // DB would return a different value than the request cache — proves cache wins and DB is
    // skipped.
    // lenient: unused after green (cache short-circuit); used during red (resolver still hits DB).
    lenient()
        .when(
            configurationParameterRepository.findByName(SitmunConstants.LANGUAGE_DEFAULT_CONF_KEY))
        .thenReturn(
            Optional.of(
                ConfigurationParameter.builder()
                    .name(SitmunConstants.LANGUAGE_DEFAULT_CONF_KEY)
                    .value("es")
                    .build()));

    TranslationCache cache = new TranslationCache();
    cache.setDefaultLanguageShortname("ca");
    cache.populate(java.util.List.of());
    TranslationCache.setRequestAttribute(cache, request);

    assertThat(resolver.resolveShortname()).isEqualTo("ca");
    verify(configurationParameterRepository, never())
        .findByName(SitmunConstants.LANGUAGE_DEFAULT_CONF_KEY);
  }
}
