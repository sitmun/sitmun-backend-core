package org.sitmun.administration.service.extractor.capabilities;

import okhttp3.HttpUrl;

final class CapabilitiesUrlBuilder {

  static final String TYPE_WMS = "WMS";

  private CapabilitiesUrlBuilder() {}

  static String build(String endpoint, String type) {
    if (endpoint == null || endpoint.isBlank()) {
      throw new IllegalArgumentException("url is required");
    }
    if (type == null || type.isBlank()) {
      throw new IllegalArgumentException("type is required");
    }
    if (!TYPE_WMS.equals(type)) {
      throw new IllegalArgumentException("Unsupported service type for capabilities: " + type);
    }
    HttpUrl parsed = HttpUrl.parse(endpoint);
    if (parsed == null) {
      throw new IllegalArgumentException("Invalid url");
    }
    if (hasGetCapabilitiesRequest(parsed)) {
      return endpoint;
    }
    return parsed
        .newBuilder()
        .setQueryParameter("request", "GetCapabilities")
        .setQueryParameter("service", "WMS")
        .build()
        .toString();
  }

  private static boolean hasGetCapabilitiesRequest(HttpUrl parsed) {
    for (int i = 0; i < parsed.querySize(); i++) {
      if ("request".equalsIgnoreCase(parsed.queryParameterName(i))
          && "GetCapabilities".equals(parsed.queryParameterValue(i))) {
        return true;
      }
    }
    return false;
  }
}
