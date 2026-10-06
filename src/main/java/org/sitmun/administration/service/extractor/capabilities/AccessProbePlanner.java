package org.sitmun.administration.service.extractor.capabilities;

import java.net.URI;
import okhttp3.HttpUrl;
import org.sitmun.administration.service.access.AccessProbePlan;
import org.sitmun.administration.service.access.AccessProbePlan.Authorization;
import org.sitmun.domain.service.Service;

public final class AccessProbePlanner {

  private AccessProbePlanner() {}

  public static AccessProbePlan plan(Service service) {
    String type = service.getType();
    if (type == null || type.isBlank()) {
      throw new IllegalArgumentException("type is required");
    }
    Authorization authorization = authorization(service);
    return switch (type) {
      case "WMS" ->
          new AccessProbePlan(
              type,
              "GetCapabilities",
              requestUri(CapabilitiesUrlBuilder.build(service.getServiceURL(), type)),
              null,
              true,
              authorization);
      case "WFS" -> ogc(service, type, authorization, null);
      case "WMTS" -> ogc(service, type, authorization, wmtsFallback(service.getServiceURL()));
      case "AIMS", "FME", "TC" ->
          new AccessProbePlan(
              type, "GET", requestUri(service.getServiceURL()), null, false, authorization);
      default ->
          throw new IllegalArgumentException("Unsupported service type for access check: " + type);
    };
  }

  private static AccessProbePlan ogc(
      Service service, String type, Authorization authorization, URI fallbackUrl) {
    HttpUrl base = withoutSecrets(service.getServiceURL());
    URI url =
        base.newBuilder()
            .setQueryParameter("request", "GetCapabilities")
            .setQueryParameter("service", type)
            .build()
            .uri();
    return new AccessProbePlan(type, "GetCapabilities", url, fallbackUrl, true, authorization);
  }

  private static URI wmtsFallback(String endpoint) {
    HttpUrl base = withoutSecrets(endpoint).newBuilder().query(null).fragment(null).build();
    return base.newBuilder().addPathSegments("1.0.0/WMTSCapabilities.xml").build().uri();
  }

  private static Authorization authorization(Service service) {
    if (Boolean.TRUE.equals(service.getPasswordSet())) {
      return new Authorization.Basic(service.getUser(), service.getPassword());
    }
    return new Authorization.None();
  }

  private static URI requestUri(String raw) {
    return withoutSecrets(raw).uri();
  }

  private static HttpUrl withoutSecrets(String endpoint) {
    if (endpoint == null || endpoint.isBlank()) {
      throw new IllegalArgumentException("url is required");
    }
    HttpUrl parsed = HttpUrl.parse(endpoint);
    if (parsed == null) {
      throw new IllegalArgumentException("Invalid url");
    }
    return parsed.newBuilder().username("").password("").fragment(null).build();
  }
}
