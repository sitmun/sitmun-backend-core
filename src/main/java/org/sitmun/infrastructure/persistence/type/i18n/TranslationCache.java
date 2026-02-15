package org.sitmun.infrastructure.persistence.type.i18n;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Request-scoped cache of translations keyed by element:column. Populated once per request by
 * TranslationCacheFilter; read by TranslationService during @PostLoad to avoid nested queries.
 */
public final class TranslationCache {

  private static final Logger log = LoggerFactory.getLogger(TranslationCache.class);
  private static final String REQUEST_ATTRIBUTE = TranslationCache.class.getName();

  private final Map<String, String> byKey = new HashMap<>();
  private boolean initialized;

  /** Index rows by element:column for lookup by entity. */
  public void populate(Iterable<TranslationRow> rows) {
    byKey.clear();
    initialized = false;
    int rowCount = 0;
    for (TranslationRow row : rows) {
      rowCount++;
      if (row.element() != null && row.column() != null && row.translation() != null) {
        byKey.put(key(row.element(), row.column()), row.translation());
      }
    }
    initialized = true;
    log.debug("TranslationCache populated: {} rows received, {} entries indexed", rowCount, byKey.size());
    if (byKey.isEmpty() && rowCount > 0) {
      log.debug("TranslationCache: all {} rows skipped (null element/column/translation)", rowCount);
    } else if (rowCount == 0) {
      log.debug("TranslationCache: preload returned 0 rows for locale (cache empty but initialized)");
    }
  }

  /**
   * Look up translations for an entity. Returns property name (e.g. "description") to translated
   * value for columns matching entityPrefix (e.g. "CodeListValue").
   */
  public Map<String, String> lookup(Integer entityId, String entityPrefix) {
    if (entityId == null || entityPrefix == null) {
      log.debug("TranslationCache.lookup null id or prefix -> empty");
      return Collections.emptyMap();
    }
    String prefix = entityPrefix + ".";
    Map<String, String> out = new HashMap<>();
    for (Map.Entry<String, String> e : byKey.entrySet()) {
      String k = e.getKey();
      if (!k.startsWith(entityId + ":")) {
        continue;
      }
      String column = k.substring((entityId + ":").length());
      if (column.startsWith(prefix)) {
        String property = column.substring(prefix.length());
        out.put(property, e.getValue());
      }
    }
    log.debug("TranslationCache.lookup entityId={} entityPrefix={} -> {} properties", entityId, entityPrefix, out.size());
    return out;
  }

  /** True if populate() was called this request (even when 0 rows). Avoids fallback to per-entity query. */
  public boolean isPopulated() {
    return initialized;
  }

  public void clear() {
    byKey.clear();
    initialized = false;
  }

  private static String key(Integer element, String column) {
    return element + ":" + column;
  }

  /** Obtain cache from current request; null if no request or cache not set. */
  public static TranslationCache fromRequest() {
    RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
    if (attrs instanceof ServletRequestAttributes sra) {
      // Try RequestAttributes first
      Object value = attrs.getAttribute(REQUEST_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
      if (value instanceof TranslationCache cache) {
        log.debug("TranslationCache.fromRequest: cache found via RequestContextHolder initialized={} size={}", cache.initialized, cache.byKey.size());
        return cache;
      }
      
      // Fallback: check directly on ServletRequest (for Filter context)
      value = sra.getRequest().getAttribute(REQUEST_ATTRIBUTE);
      if (value instanceof TranslationCache cache) {
        log.debug("TranslationCache.fromRequest: cache found via ServletRequest initialized={} size={}", cache.initialized, cache.byKey.size());
        return cache;
      }
    }
    log.debug("TranslationCache.fromRequest: no cache on request");
    return null;
  }

  /** Store cache on current request. */
  public static void setRequestAttribute(TranslationCache cache) {
    RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
    if (attrs != null) {
      attrs.setAttribute(REQUEST_ATTRIBUTE, cache, RequestAttributes.SCOPE_REQUEST);
      log.debug("TranslationCache.setRequestAttribute: cache set via RequestContextHolder initialized={} size={}", cache.initialized, cache.byKey.size());
    } else {
      log.debug("TranslationCache.setRequestAttribute: no request attributes via RequestContextHolder");
    }
  }
  
  /** Store cache on ServletRequest (for Filter context). */
  public static void setRequestAttribute(TranslationCache cache, jakarta.servlet.ServletRequest request) {
    if (request == null) {
      log.debug("TranslationCache.setRequestAttribute: null ServletRequest");
      return;
    }
    request.setAttribute(REQUEST_ATTRIBUTE, cache);
    log.debug("TranslationCache.setRequestAttribute: cache set on ServletRequest initialized={} size={}", cache.initialized, cache.byKey.size());
  }

  /** Remove cache from current request. */
  public static void removeRequestAttribute() {
    RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
    if (attrs != null) {
      attrs.removeAttribute(REQUEST_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
      log.debug("TranslationCache.removeRequestAttribute: cache removed via RequestContextHolder");
    }
  }
  
  /** Remove cache from ServletRequest (for Filter context). */
  public static void removeRequestAttribute(jakarta.servlet.ServletRequest request) {
    if (request == null) {
      log.debug("TranslationCache.removeRequestAttribute: null ServletRequest");
      return;
    }
    request.removeAttribute(REQUEST_ATTRIBUTE);
    log.debug("TranslationCache.removeRequestAttribute: cache removed from ServletRequest");
  }
}
