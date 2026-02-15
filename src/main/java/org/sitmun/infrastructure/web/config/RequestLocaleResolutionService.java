package org.sitmun.infrastructure.web.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.sitmun.domain.configuration.ConfigurationParameter;
import org.sitmun.domain.configuration.ConfigurationParameterRepository;
import org.sitmun.infrastructure.persistence.type.i18n.Language;
import org.sitmun.infrastructure.persistence.type.i18n.LanguageRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

/**
 * Resolves request locale using supported languages from database. Supports explicit {@code lang}
 * param, Accept-Language header, then resolver/context/default. Matches against Language.shortname
 * with pattern: shortname[-.*]?
 */
@Component
@Slf4j
public class RequestLocaleResolutionService {

  private static final String DEFAULT_LANGUAGE_PARAM = "default.language";

  @Value("${sitmun.language}")
  private String sitmunLanguage;

  private final LocaleResolver localeResolver;
  private final LocaleChangeInterceptor localeChangeInterceptor;
  private final LanguageRepository languageRepository;
  private final ConfigurationParameterRepository configurationParameterRepository;

  /** Writable under test JVM (e.g. module build dir); fallback for workspace .cursor path. */
  public RequestLocaleResolutionService(
      LocaleResolver localeResolver,
      LocaleChangeInterceptor localeChangeInterceptor,
      LanguageRepository languageRepository,
      ConfigurationParameterRepository configurationParameterRepository) {
    this.localeResolver = localeResolver;
    this.localeChangeInterceptor = localeChangeInterceptor;
    this.languageRepository = languageRepository;
    this.configurationParameterRepository = configurationParameterRepository;
  }

  /**
   * Applies LocaleChangeInterceptor semantics and resolves the request language. Source order:
   * lang-param → accept-language → resolver → context-holder → default.
   *
   * @param request current HTTP request
   * @param response current HTTP response
   * @param handler current handler object
   * @param defaultLanguage fallback if no locale can be determined (deprecated, use database)
   * @return resolved language tag for DB lookup (e.g. en, fr, oc-aranes)
   */
  public String resolveLanguage(
      HttpServletRequest request,
      HttpServletResponse response,
      Object handler,
      String defaultLanguage) {
    String uri = request != null ? request.getRequestURI() : "null";
    String query = request != null ? request.getQueryString() : "null";
    String langParam = request != null ? request.getParameter("lang") : null;
    log.debug(
        "RequestLocaleResolutionService.resolveLanguage start uri={} queryString={} langParam={}",
        uri,
        query,
        langParam);

    // 1) Explicit lang param: Spring 6 LocaleChangeInterceptor semantics
    if (langParam != null
        && !langParam.isBlank()
        && checkHttpMethod(request.getMethod(), localeChangeInterceptor.getHttpMethods())) {
      try {
        Locale parsed = StringUtils.parseLocale(langParam);
        String matched = matchSupportedLanguage(parsed.toLanguageTag());
        if (matched != null) {
          localeResolver.setLocale(request, response, parsed);
          log.debug(
              "RequestLocaleResolutionService source=lang-param applied locale paramName={} parsed={} matched={}",
              localeChangeInterceptor.getParamName(),
              parsed.toLanguageTag(),
              matched);
          return matched;
        }
      } catch (IllegalArgumentException ex) {
        if (localeChangeInterceptor.isIgnoreInvalidLocale()) {
          log.debug(
              "RequestLocaleResolutionService ignored invalid locale value={} reason={}",
              langParam,
              ex.getMessage());
        } else {
          throw ex;
        }
      }
    }

    // 2) Accept-Language header when lang is absent
    Enumeration<Locale> locales = request.getLocales();
    if (locales != null && locales.hasMoreElements()) {
      Locale first = locales.nextElement();
      String matched = matchSupportedLanguage(first.toLanguageTag());
      if (matched != null) {
        log.debug(
            "RequestLocaleResolutionService source=accept-language first={} matched={}",
            first.toLanguageTag(),
            matched);
        return matched;
      }
    }

    // 3) Resolver (session/default)
    Locale resolved = localeResolver.resolveLocale(request);
    if (resolved != null && !resolved.getLanguage().isEmpty()) {
      String matched = matchSupportedLanguage(resolved.toLanguageTag());
      if (matched != null) {
        log.debug(
            "RequestLocaleResolutionService source=resolver resolved={} matched={}",
            resolved.toLanguageTag(),
            matched);
        return matched;
      }
    }

    // 4) Context holder
    Locale holderLocale = LocaleContextHolder.getLocale();
    if (holderLocale != null && !holderLocale.getLanguage().isEmpty()) {
      String matched = matchSupportedLanguage(holderLocale.toLanguageTag());
      if (matched != null) {
        log.debug(
            "RequestLocaleResolutionService source=context-holder holder={} matched={}",
            holderLocale.toLanguageTag(),
            matched);
        return matched;
      }
    }

    // 5) Database default.language parameter
    String dbDefaultLanguage = getDefaultLanguageFromDatabase();
    if (dbDefaultLanguage != null) {
      log.debug(
          "RequestLocaleResolutionService source=database-parameter matched={}", dbDefaultLanguage);
      return dbDefaultLanguage;
    }

    // 6) Final fallback: sitmun.language property
    log.debug("RequestLocaleResolutionService source=sitmun-property matched={}", sitmunLanguage);
    return sitmunLanguage != null ? sitmunLanguage : "en";
  }

  /**
   * Matches a locale tag against supported languages in database. Pattern: Language.shortname[-.*]?
   * Examples: - "en-US" matches "en" - "oc-aranes" matches "oc-aranes" (exact match) - "ca" matches
   * "ca"
   *
   * @param localeTag the locale tag to match (e.g., "en-US", "ca", "oc-aranes")
   * @return matched Language.shortname or null if no match
   */
  private String matchSupportedLanguage(String localeTag) {
    if (localeTag == null || localeTag.isEmpty()) {
      return null;
    }

    List<Language> supportedLanguages = languageRepository.findAll();

    // First try exact match
    for (Language lang : supportedLanguages) {
      if (localeTag.equals(lang.getShortname())) {
        log.debug(
            "RequestLocaleResolutionService.match exact localeTag={} -> {}",
            localeTag,
            lang.getShortname());
        return lang.getShortname();
      }
    }

    // Then try prefix match (e.g., "en-US" matches "en")
    String baseTag =
        localeTag.contains("-") ? localeTag.substring(0, localeTag.indexOf('-')) : localeTag;
    for (Language lang : supportedLanguages) {
      if (baseTag.equals(lang.getShortname())) {
        log.debug(
            "RequestLocaleResolutionService.match prefix localeTag={} baseTag={} -> {}",
            localeTag,
            baseTag,
            lang.getShortname());
        return lang.getShortname();
      }
    }

    log.debug(
        "RequestLocaleResolutionService.match no-match localeTag={} supportedCount={}",
        localeTag,
        supportedLanguages.size());
    return null;
  }

  /**
   * Gets default language from ConfigurationParameter table. Looks for parameter with name
   * "default.language".
   *
   * @return the default language shortname or null if not found
   */
  private String getDefaultLanguageFromDatabase() {
    try {
      Optional<ConfigurationParameter> param =
          configurationParameterRepository.findAll().stream()
              .filter(p -> DEFAULT_LANGUAGE_PARAM.equals(p.getName()))
              .findFirst();

      if (param.isPresent() && param.get().getValue() != null) {
        String value = param.get().getValue();
        String matched = matchSupportedLanguage(value);
        if (matched != null) {
          log.debug(
              "RequestLocaleResolutionService.defaultFromDb found param value={} matched={}",
              value,
              matched);
          return matched;
        }
      }
    } catch (Exception e) {
      log.debug("RequestLocaleResolutionService.defaultFromDb error reading parameter", e);
    }
    return null;
  }

  private boolean checkHttpMethod(String currentMethod, String[] configuredMethods) {
    if (ObjectUtils.isEmpty(configuredMethods)) {
      return true;
    }
    for (String configuredMethod : configuredMethods) {
      if (configuredMethod.equalsIgnoreCase(currentMethod)) {
        return true;
      }
    }
    return false;
  }
}
