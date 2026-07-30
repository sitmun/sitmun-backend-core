package org.sitmun.infrastructure.persistence.type.i18n;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import java.util.Collections;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sitmun.infrastructure.web.config.RequestLocaleResolutionService;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@ExtendWith(MockitoExtension.class)
@DisplayName("TranslationCacheFilter")
class TranslationCacheFilterTest {

  @Mock private TranslationRepository translationRepository;
  @Mock private RequestLocaleResolutionService requestLocaleResolutionService;
  @Mock private DatabaseDefaultLanguageResolver databaseDefaultLanguageResolver;
  @Mock private FilterChain filterChain;

  private TranslationCacheFilter filter;
  private MockHttpServletRequest request;
  private MockHttpServletResponse response;

  @BeforeEach
  void setUp() {
    filter =
        new TranslationCacheFilter(
            translationRepository, requestLocaleResolutionService, databaseDefaultLanguageResolver);
    ReflectionTestUtils.setField(filter, "defaultLanguage", "ca");
    request = new MockHttpServletRequest();
    response = new MockHttpServletResponse();
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    LocaleContextHolder.setLocale(Locale.forLanguageTag("en"));
  }

  @AfterEach
  void tearDown() {
    TranslationCache.removeRequestAttribute(request);
    RequestContextHolder.resetRequestAttributes();
    LocaleContextHolder.resetLocaleContext();
  }

  @Test
  @DisplayName("clears LocaleContextHolder after preload request with lang parameter")
  void clearsLocaleAfterPreloadRequest() throws Exception {
    request.setParameter("lang", "es");
    when(requestLocaleResolutionService.resolveLanguage(any(), any(), any(), any()))
        .thenReturn("es");
    when(databaseDefaultLanguageResolver.resolveShortnameFromDatabase()).thenReturn("ca");
    when(translationRepository.findAllByLocaleRows("es")).thenReturn(Collections.emptyList());
    doAnswer(
            invocation -> {
              LocaleContextHolder.setLocale(Locale.forLanguageTag("fr"));
              return null;
            })
        .when(filterChain)
        .doFilter(request, response);

    filter.doFilter(request, response, filterChain);

    assertThat(LocaleContextHolder.getLocaleContext()).isNull();
    verify(filterChain).doFilter(request, response);
  }

  @Test
  @DisplayName("clears LocaleContextHolder when preload is skipped")
  void clearsLocaleWhenPreloadSkipped() throws Exception {
    request.setRequestURI("/api/other");

    filter.doFilter(request, response, filterChain);

    assertThat(LocaleContextHolder.getLocaleContext()).isNull();
    verify(filterChain).doFilter(request, response);
  }

  @Test
  @DisplayName("preload stores default language shortname on request translation cache")
  void preloadStoresDefaultLanguageOnCache() throws Exception {
    request.setParameter("lang", "es");
    when(requestLocaleResolutionService.resolveLanguage(any(), any(), any(), any()))
        .thenReturn("es");
    when(databaseDefaultLanguageResolver.resolveShortnameFromDatabase()).thenReturn("ca");
    when(translationRepository.findAllByLocaleRows("es")).thenReturn(Collections.emptyList());
    AtomicReference<String> cachedDefault = new AtomicReference<>();
    doAnswer(
            invocation -> {
              TranslationCache cache = TranslationCache.fromRequest();
              assertThat(cache).isNotNull();
              assertThat(cache.isPopulated()).isTrue();
              cachedDefault.set(cache.getDefaultLanguageShortname());
              return null;
            })
        .when(filterChain)
        .doFilter(request, response);

    filter.doFilter(request, response, filterChain);

    assertThat(cachedDefault.get()).isEqualTo("ca");
    verify(databaseDefaultLanguageResolver).resolveShortnameFromDatabase();
  }
}
