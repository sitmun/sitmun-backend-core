package org.sitmun.administration.service.extractor.capabilities;

import java.io.IOException;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.json.JSONObject;
import org.json.XML;
import org.sitmun.administration.service.extractor.HttpClientFactory;
import org.sitmun.administration.service.extractor.capabilities.ExtractedMetadata.ExtractedMetadataBuilder;
import org.springframework.stereotype.Service;

@Service
public class SimpleServiceCapabilitiesExtractor implements ServiceCapabilitiesExtractor {

  public static final String WMS_CAPABILITIES = "WMS_Capabilities";
  public static final String WMT_MS_CAPABILITIES = "WMT_MS_Capabilities";
  public static final String VERSION = "version";
  private final HttpClientFactory httpClientFactory;

  public SimpleServiceCapabilitiesExtractor(HttpClientFactory httpClientFactory) {
    this.httpClientFactory = httpClientFactory;
  }

  @Override
  public ExtractedMetadata extract(String url) {
    ExtractedMetadataBuilder builder = new ExtractedMetadataBuilder();

    Request request = new Request.Builder().url(url).header("Accept", "*/*").build();
    try (Response response = httpClientFactory.executeRequest(request)) {
      processResponse(builder, response);
    } catch (Exception e) {
      builder.success(false).reason(e.getClass().getSimpleName() + ": " + e.getMessage());
    }
    return builder.build();
  }

  private void processResponse(ExtractedMetadataBuilder builder, Response response)
      throws IOException {
    builder.success(response.code() == 200 && response.body() != null);
    ResponseBody body = response.body();
    if (body != null) {
      String text = body.string();
      if (text.trim().isEmpty()) {
        builder.success(false).reason("Empty response");
      } else {
        builder.asText(text);
        JSONObject json;
        try {
          json = XML.toJSONObject(text);
        } catch (Exception ignored) {
          builder.success(false).reason("Not a well formed XML");
          return;
        }
        builder.asJson(json.toMap());
        if (json.has(WMS_CAPABILITIES) && json.getJSONObject(WMS_CAPABILITIES).has(VERSION)) {
          builder.type("OGC:WMS " + json.getJSONObject(WMS_CAPABILITIES).get(VERSION));
        } else if (json.has(WMT_MS_CAPABILITIES)
            && json.getJSONObject(WMT_MS_CAPABILITIES).has(VERSION)) {
          builder.type("OGC:WMS " + json.getJSONObject(WMT_MS_CAPABILITIES).get(VERSION));
        } else {
          builder.success(false).reason("Not a standard OGC:WMS Capabilities response");
        }
      }
    }
  }
}
