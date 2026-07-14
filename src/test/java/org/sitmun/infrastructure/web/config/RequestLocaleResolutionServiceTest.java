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

  private RequestLocaleResolutionService service;
  private static final String DEFAULT_LANGUAGE = "en";

  private List<Language> supportedLanguages;

  @BeforeEach
  void setUp() {
    LocaleContextHolder.resetLocaleContext();
    service =
        new RequestLocaleResolutionService(
            localeResolver, localeChangeInterceptor, languageRepository);

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
    lenient()
        .when(languageRepository.findFirstByDefaultLanguageTrue())
        .thenReturn(java.util.Optional.empty());
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
    @DisplayName("maps oc-aranes-ES to oc-aranes when oc-aranes is a supported shortname")
    void localizedOcAranesTagMapsToShortname() {
      when(request.getParameter("lang")).thenReturn("oc-aranes-ES");
      when(request.getMethod()).thenReturn("GET");

      String result = service.resolveLanguage(request, response, null, DEFAULT_LANGUAGE);

      assertThat(result).isEqualTo("oc-aranes");
    }

    @Test
    @DisplayName("falls back to sitmun.language when lang param is unsupported")
    void unsupportedLangFallsBackToDefault() {
      when(request.getParameter("lang")).thenReturn("de");
      when(request.getMethod()).thenReturn("GET");
      when(request.getLocales()).thenReturn(Collections.emptyEnumeration());
      when(localeResolver.resolveLocale(request)).thenReturn(null);

      Locale previousLocale = LocaleContextHolder.getLocale();
      try {
        LocaleContextHolder.setLocale(Locale.GERMANY);
        String result = service.resolveLanguage(request, response, null, DEFAULT_LANGUAGE);

        // Should fallback to sitmun.language since de is not supported
        assertThat(result).isEqualTo("en");
      } finally {
        LocaleContextHolder.setLocale(previousLocale);
      }
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
    @DisplayName("falls back to default language flag when resolver returns unsupported")
    void fallbackToDatabaseDefaultLanguage() {
      when(request.getParameter("lang")).thenReturn(null);
      when(request.getLocales()).thenReturn(Collections.emptyEnumeration());
      when(localeResolver.resolveLocale(request)).thenReturn(Locale.forLanguageTag("de"));

      // Reset and setup mocks for this test
      reset(languageRepository);
      when(languageRepository.findAll()).thenReturn(supportedLanguages);
      when(languageRepository.findFirstByDefaultLanguageTrue())
          .thenReturn(java.util.Optional.of(createLanguage("es", "Spanish")));

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

      Locale previousLocale = LocaleContextHolder.getLocale();
      try {
        LocaleContextHolder.setLocale(Locale.GERMANY);
        String result = service.resolveLanguage(request, response, null, DEFAULT_LANGUAGE);

        assertThat(result).isEqualTo("en");
      } finally {
        LocaleContextHolder.setLocale(previousLocale);
      }
    }
  }
}
