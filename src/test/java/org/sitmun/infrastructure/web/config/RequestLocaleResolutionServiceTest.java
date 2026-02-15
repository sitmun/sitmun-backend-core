package org.sitmun.infrastructure.web.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sitmun.domain.configuration.ConfigurationParameter;
import org.sitmun.domain.configuration.ConfigurationParameterRepository;
import org.sitmun.infrastructure.persistence.type.i18n.Language;
import org.sitmun.infrastructure.persistence.type.i18n.LanguageRepository;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

@ExtendWith(MockitoExtension.class)
@DisplayName("RequestLocaleResolutionService unit tests")
class RequestLocaleResolutionServiceTest {

  @Mock private HttpServletRequest request;
  @Mock private HttpServletResponse response;
  @Mock private LocaleResolver localeResolver;
  @Mock private LocaleChangeInterceptor localeChangeInterceptor;
  @Mock private LanguageRepository languageRepository;
  @Mock private ConfigurationParameterRepository configurationParameterRepository;

  private RequestLocaleResolutionService service;
  private static final String DEFAULT_LANGUAGE = "en";

  private List<Language> supportedLanguages;

  @BeforeEach
  void setUp() {
    service =
        new RequestLocaleResolutionService(
            localeResolver,
            localeChangeInterceptor,
            languageRepository,
            configurationParameterRepository);

    ReflectionTestUtils.setField(service, "sitmunLanguage", "en");

    // Setup supported languages in database
    supportedLanguages =
        Arrays.asList(
            createLanguage("en", "English"),
            createLanguage("es", "Spanish"),
            createLanguage("ca", "Catalan"),
            createLanguage("fr", "French"),
            createLanguage("oc-aranes", "Occitan Aranese"));

    lenient().when(languageRepository.findAll()).thenReturn(supportedLanguages);
    lenient().when(configurationParameterRepository.findAll()).thenReturn(Collections.emptyList());
    lenient().when(localeChangeInterceptor.getParamName()).thenReturn("lang");
    lenient().when(localeChangeInterceptor.getHttpMethods()).thenReturn(null);
    lenient().when(localeChangeInterceptor.isIgnoreInvalidLocale()).thenReturn(true);
  }

  private Language createLanguage(String shortname, String name) {
    return Language.builder().id(shortname.hashCode()).shortname(shortname).name(name).build();
  }

  @Nested
  @DisplayName("lang query parameter present")
  class LangParamPresent {

    @Test
    @DisplayName("returns matched language from lang param and sets locale on resolver")
    void langParamSetsLocaleAndReturnsTag() {
      when(request.getParameter("lang")).thenReturn("ca");
      when(request.getMethod()).thenReturn("GET");

      String result = service.resolveLanguage(request, response, null, DEFAULT_LANGUAGE);

      assertThat(result).isEqualTo("ca");
      verify(localeResolver).setLocale(eq(request), eq(response), any(Locale.class));
    }

    @Test
    @DisplayName("preserves full tag oc-aranes when exact match exists")
    void fullTagOcAranesPreserved() {
      when(request.getParameter("lang")).thenReturn("oc-aranes");
      when(request.getMethod()).thenReturn("GET");

      String result = service.resolveLanguage(request, response, null, DEFAULT_LANGUAGE);

      assertThat(result).isEqualTo("oc-aranes");
    }

    @Test
    @DisplayName("maps fr-FR to base fr when fr is in database")
    void regionalMapsToBase() {
      when(request.getParameter("lang")).thenReturn("fr-FR");
      when(request.getMethod()).thenReturn("GET");

      String result = service.resolveLanguage(request, response, null, DEFAULT_LANGUAGE);

      assertThat(result).isEqualTo("fr");
    }

    @Test
    @DisplayName("falls back to database default when lang param not in supported languages")
    void unsupportedLangFallsBackToDefault() {
      when(request.getParameter("lang")).thenReturn("de");
      when(request.getMethod()).thenReturn("GET");
      when(request.getLocales()).thenReturn(Collections.emptyEnumeration());
      when(localeResolver.resolveLocale(request)).thenReturn(null);

      String result = service.resolveLanguage(request, response, null, DEFAULT_LANGUAGE);

      // Should fallback to sitmun.language since de is not supported
      assertThat(result).isEqualTo("en");
    }
  }

  @Nested
  @DisplayName("lang absent, Accept-Language header present")
  class AcceptLanguageFallback {

    @Test
    @DisplayName("uses first Accept-Language when lang is missing and matches supported language")
    void usesFirstAcceptLanguage() {
      when(request.getParameter("lang")).thenReturn(null);
      when(request.getLocales())
          .thenReturn(
              Collections.enumeration(Collections.singletonList(Locale.forLanguageTag("es"))));

      String result = service.resolveLanguage(request, response, null, DEFAULT_LANGUAGE);

      assertThat(result).isEqualTo("es");
      verify(localeResolver, never()).setLocale(any(), any(), any());
    }

    @Test
    @DisplayName("maps fr-FR from Accept-Language to base fr when fr is supported")
    void acceptLanguageRegionalToBase() {
      when(request.getParameter("lang")).thenReturn(null);
      when(request.getLocales())
          .thenReturn(Collections.enumeration(Collections.singletonList(Locale.FRANCE)));

      String result = service.resolveLanguage(request, response, null, DEFAULT_LANGUAGE);

      assertThat(result).isEqualTo("fr");
    }

    @Test
    @DisplayName("preserves oc-aranes from Accept-Language when exact match exists")
    void acceptLanguageOcAranesPreserved() {
      when(request.getParameter("lang")).thenReturn(null);
      when(request.getLocales())
          .thenReturn(
              Collections.enumeration(
                  Collections.singletonList(Locale.forLanguageTag("oc-aranes"))));

      String result = service.resolveLanguage(request, response, null, DEFAULT_LANGUAGE);

      assertThat(result).isEqualTo("oc-aranes");
    }
  }

  @Nested
  @DisplayName("neither lang nor Accept-Language")
  class FallbackChain {

    @Test
    @DisplayName("falls back to resolver then sitmun.language")
    void fallbackToResolverThenDefault() {
      when(request.getParameter("lang")).thenReturn(null);
      when(request.getLocales()).thenReturn(Collections.emptyEnumeration());
      when(localeResolver.resolveLocale(request)).thenReturn(Locale.ENGLISH);

      String result = service.resolveLanguage(request, response, null, DEFAULT_LANGUAGE);

      assertThat(result).isEqualTo("en");
    }

    @Test
    @DisplayName("uses resolver when lang and Accept-Language absent and matches supported")
    void usesResolverWhenBothAbsent() {
      when(request.getParameter("lang")).thenReturn(null);
      when(request.getLocales()).thenReturn(Collections.emptyEnumeration());
      when(localeResolver.resolveLocale(request)).thenReturn(Locale.forLanguageTag("ca"));

      String result = service.resolveLanguage(request, response, null, DEFAULT_LANGUAGE);

      assertThat(result).isEqualTo("ca");
    }

    @Test
    @DisplayName(
        "falls back to database default.language parameter when resolver returns unsupported")
    void fallbackToDatabaseDefaultParameter() {
      when(request.getParameter("lang")).thenReturn(null);
      when(request.getLocales()).thenReturn(Collections.emptyEnumeration());
      when(localeResolver.resolveLocale(request)).thenReturn(Locale.forLanguageTag("de"));

      ConfigurationParameter defaultLangParam =
          ConfigurationParameter.builder().name("default.language").value("es").build();

      // Reset and setup mocks for this test
      reset(configurationParameterRepository);
      reset(languageRepository);
      when(configurationParameterRepository.findAll())
          .thenReturn(Collections.singletonList(defaultLangParam));
      when(languageRepository.findAll()).thenReturn(supportedLanguages);

      // Ensure step 4 (context holder) does not return early: JVM default locale (e.g. "en")
      // would otherwise match and we would never reach the database default.
      Locale previousLocale = LocaleContextHolder.getLocale();
      try {
        LocaleContextHolder.setLocale(Locale.GERMANY); // "de" not in supported list
        String result = service.resolveLanguage(request, response, null, DEFAULT_LANGUAGE);
        assertThat(result).isEqualTo("es");
      } finally {
        LocaleContextHolder.setLocale(previousLocale);
      }
    }

    @Test
    @DisplayName("falls back to sitmun.language when all else fails")
    void fallbackToSitmunLanguageProperty() {
      when(request.getParameter("lang")).thenReturn(null);
      when(request.getLocales()).thenReturn(Collections.emptyEnumeration());
      when(localeResolver.resolveLocale(request)).thenReturn(null);

      String result = service.resolveLanguage(request, response, null, DEFAULT_LANGUAGE);

      assertThat(result).isEqualTo("en");
    }
  }
}
