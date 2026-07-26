package org.sitmun.authorization.client.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import org.sitmun.SitmunConstants;
import org.sitmun.domain.configuration.ConfigurationParameter;
import org.sitmun.domain.configuration.ConfigurationParameterRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Resolves the client-facing proxy middleware base URL: a non-blank, valid {@code STM_CONF.proxy}
 * wins; blank, empty, or invalid DB values fall back to {@code sitmun.proxy-middleware.url} and are
 * persisted so the admin UI shows the effective value. Result is normalized (default ports and
 * trailing slash stripped).
 */
@Service
public class ProxyMiddlewareUrlResolver {

  private final ConfigurationParameterRepository configurationParameterRepository;
  private final String springProxyUrl;
  private final TransactionTemplate requiresNewTransaction;

  public ProxyMiddlewareUrlResolver(
      ConfigurationParameterRepository configurationParameterRepository,
      @Value("${sitmun.proxy-middleware.url:}") String springProxyUrl,
      PlatformTransactionManager transactionManager) {
    this.configurationParameterRepository = configurationParameterRepository;
    this.springProxyUrl = springProxyUrl == null ? "" : springProxyUrl;
    this.requiresNewTransaction = new TransactionTemplate(transactionManager);
    this.requiresNewTransaction.setPropagationBehavior(
        TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  /**
   * Effective middleware base URL (no trailing slash), or blank when neither DB nor Spring provides
   * a usable value. Always persists a canonical value when a {@code proxy} row exists and differs
   * from the effective URL (unnormalized, blank, or invalid stored values).
   */
  public String resolve() {
    Optional<ConfigurationParameter> row =
        configurationParameterRepository.findByName(SitmunConstants.PROXY_CONF_KEY);

    Optional<String> fromDb =
        row.map(ConfigurationParameter::getValue)
            .filter(value -> value != null && !value.isBlank())
            .map(String::trim)
            .flatMap(ProxyMiddlewareUrlResolver::tryNormalize);

    String effective = fromDb.orElseGet(() -> tryNormalize(springProxyUrl.trim()).orElse(""));

    if (row.isPresent() && !effective.isBlank() && !effective.equals(row.get().getValue())) {
      persistProxyValue(effective);
    }
    return effective;
  }

  /**
   * Canonical value to store for {@code STM_CONF.proxy}: always normalized; blank/invalid
   * candidates become the config default (also normalized).
   */
  public String sanitizeStoredProxyValue(String raw) {
    return sanitizeStoredProxy(raw).value();
  }

  /**
   * Same as {@link #sanitizeStoredProxyValue(String)}, plus i18n info keys when the stored value
   * differs from the submitted candidate (normalization or default fallback).
   */
  public ProxySanitizeResult sanitizeStoredProxy(String raw) {
    String candidate = raw == null ? "" : raw.trim();
    Optional<String> normalized = tryNormalize(candidate);
    if (normalized.isPresent()) {
      String value = normalized.get();
      if (!value.equals(candidate)) {
        return new ProxySanitizeResult(
            value, List.of("entity.configurationParameter.warning.proxy-normalized"));
      }
      return new ProxySanitizeResult(value, List.of());
    }
    String fallback = tryNormalize(springProxyUrl.trim()).orElse("");
    if (!fallback.isBlank()) {
      return new ProxySanitizeResult(
          fallback, List.of("entity.configurationParameter.warning.proxy-defaulted"));
    }
    return new ProxySanitizeResult("", List.of());
  }

  /** Result of coercing a submitted {@code proxy} value before persist. */
  public record ProxySanitizeResult(String value, List<String> infos) {}

  private void persistProxyValue(String value) {
    requiresNewTransaction.executeWithoutResult(
        status ->
            configurationParameterRepository
                .findByName(SitmunConstants.PROXY_CONF_KEY)
                .ifPresent(
                    param -> {
                      param.setValue(value);
                      configurationParameterRepository.save(param);
                    }));
  }

  private static Optional<String> tryNormalize(String url) {
    if (url == null || url.isBlank()) {
      return Optional.empty();
    }
    try {
      return Optional.of(normalize(url));
    } catch (IllegalArgumentException ex) {
      return Optional.empty();
    }
  }

  private static String normalize(String url) {
    URI uri;
    try {
      uri = UriComponentsBuilder.fromUriString(url).build(true).toUri();
    } catch (IllegalArgumentException ex) {
      throw new IllegalArgumentException("Invalid proxy middleware URL: " + url, ex);
    }
    if (uri.getScheme() == null || uri.getHost() == null) {
      throw new IllegalArgumentException("Invalid proxy middleware URL: " + url);
    }

    int port = uri.getPort();
    String scheme = uri.getScheme();
    if (("http".equalsIgnoreCase(scheme) && port == 80)
        || ("https".equalsIgnoreCase(scheme) && port == 443)) {
      port = -1;
    }

    String path = uri.getPath();
    if (path != null && path.length() > 1 && path.endsWith("/")) {
      path = path.substring(0, path.length() - 1);
    }

    try {
      return new URI(scheme, uri.getUserInfo(), uri.getHost(), port, path, uri.getQuery(), null)
          .toASCIIString();
    } catch (URISyntaxException ex) {
      throw new IllegalArgumentException("Invalid proxy middleware URL: " + url, ex);
    }
  }
}
