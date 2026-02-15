package org.sitmun.infrastructure.persistence.type.i18n;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.sitmun.infrastructure.web.config.RequestLocaleResolutionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Global preload filter for request-scoped translation cache.
 *
 * <p>This runs for every HTTP request, independent from Spring MVC/Spring Data REST handler
 * mappings.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 200)
public class TranslationCacheFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(TranslationCacheFilter.class);

  @Value("${sitmun.language}")
  private String defaultLanguage;

  private final TranslationRepository translationRepository;
  private final RequestLocaleResolutionService requestLocaleResolutionService;

  public TranslationCacheFilter(
      TranslationRepository translationRepository,
      RequestLocaleResolutionService requestLocaleResolutionService) {
    this.translationRepository = translationRepository;
    this.requestLocaleResolutionService = requestLocaleResolutionService;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    boolean preload = shouldPreload(request);
    log.debug(
        "TranslationCacheFilter.before uri={} queryString={} shouldPreload={}",
        request.getRequestURI(),
        request.getQueryString(),
        preload);

    if (preload) {
      String locale =
          requestLocaleResolutionService.resolveLanguage(request, response, this, defaultLanguage);
      log.debug("TranslationCacheFilter.preload locale={}", locale);
      var rows = translationRepository.findAllByLocaleRows(locale);
      if (rows.isEmpty() && locale != null && locale.contains("-")) {
        String base = locale.substring(0, locale.indexOf('-'));
        log.debug("TranslationCacheFilter.preload full tag empty, fallback base locale={}", base);
        rows = translationRepository.findAllByLocaleRows(base);
      }
      TranslationCache cache = new TranslationCache();
      cache.populate(rows);
      TranslationCache.setRequestAttribute(cache, request);
    }

    try {
      filterChain.doFilter(request, response);
    } finally {
      if (preload) {
        log.debug("TranslationCacheFilter.after uri={} clearing cache", request.getRequestURI());
        TranslationCache.removeRequestAttribute(request);
      }
    }
  }

  private boolean shouldPreload(HttpServletRequest request) {
    String langParam = request.getParameter("lang");
    if (langParam != null && !langParam.isEmpty()) {
      return true;
    }
    String path = request.getRequestURI();
    return path != null && path.contains("/api/config/client");
  }
}
