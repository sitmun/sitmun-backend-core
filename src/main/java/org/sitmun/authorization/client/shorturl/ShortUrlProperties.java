package org.sitmun.authorization.client.shorturl;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sitmun.short-url")
public class ShortUrlProperties {

  private String provider = "none";

  private final TinyurlLegacy tinyurlLegacy = new TinyurlLegacy();

  public String getProvider() {
    return provider == null || provider.isBlank() ? "none" : provider;
  }

  public void setProvider(String provider) {
    this.provider = provider;
  }

  public TinyurlLegacy getTinyurlLegacy() {
    return tinyurlLegacy;
  }

  public static class TinyurlLegacy {

    private String endpoint = "https://tinyurl.com/api-create.php";

    public String getEndpoint() {
      return endpoint;
    }

    public void setEndpoint(String endpoint) {
      this.endpoint = endpoint;
    }
  }
}
