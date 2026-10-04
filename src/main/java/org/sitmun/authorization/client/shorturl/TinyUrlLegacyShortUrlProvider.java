package org.sitmun.authorization.client.shorturl;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("tinyurl-legacy")
@Slf4j
public class TinyUrlLegacyShortUrlProvider implements ShortUrlProvider {

  private final ShortUrlProperties properties;
  private final OkHttpClient httpClient;

  @Autowired
  public TinyUrlLegacyShortUrlProvider(ShortUrlProperties properties) {
    this(
        properties,
        new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS)
            .build());
  }

  TinyUrlLegacyShortUrlProvider(ShortUrlProperties properties, OkHttpClient httpClient) {
    this.properties = properties;
    this.httpClient = httpClient;
  }

  @Override
  public String id() {
    return "tinyurl-legacy";
  }

  @Override
  public ShortUrlOutcome shorten(String url) {
    Request request =
        new Request.Builder()
            .url(properties.getTinyurlLegacy().getEndpoint())
            .post(new FormBody.Builder().add("url", url).build())
            .build();
    try (Response response = httpClient.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        return new ShortUrlOutcome.Failed("Short URL provider returned " + response.code());
      }
      ResponseBody responseBody = response.body();
      String shortened = responseBody == null ? "" : responseBody.string().trim();
      if (shortened.isEmpty() || shortened.equals(url)) {
        return new ShortUrlOutcome.Failed("Short URL provider returned no URL");
      }
      return new ShortUrlOutcome.Shortened(shortened.replace("http://", "https://"));
    } catch (IOException exception) {
      log.warn("Short URL provider request failed", exception);
      return new ShortUrlOutcome.Failed("Short URL provider failed");
    }
  }
}
