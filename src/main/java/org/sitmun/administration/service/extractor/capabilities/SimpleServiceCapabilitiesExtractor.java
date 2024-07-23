package org.sitmun.administration.service.extractor.capabilities;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.json.JSONObject;
import org.json.XML;
import org.sitmun.administration.service.extractor.capabilities.ExtractedMetadata.ExtractedMetadataBuilder;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public
class SimpleServiceCapabilitiesExtractor implements ServiceCapabilitiesExtractor {

  @Override
  public ExtractedMetadata extract(String url) {
    OkHttpClient client = new OkHttpClient();
    ExtractedMetadataBuilder builder = new ExtractedMetadataBuilder();

    try {
      Request request = new Request.Builder()
        .url(url)
        .header("Accept", "*/*")
        .build();

      try (Response response = client.newCall(request).execute()) {
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
              return builder.success(false).reason("Not a well formed XML").build();
            }
            builder.asJson(json.toMap());
            if (json.has("WMS_Capabilities") &&
              json.getJSONObject("WMS_Capabilities").has("version")) {
              builder.type("OGC:WMS " + json.getJSONObject("WMS_Capabilities").get("version"));
            } else if (json.has("WMT_MS_Capabilities") && json.getJSONObject("WMT_MS_Capabilities").has("version")) {
              builder.type("OGC:WMS " + json.getJSONObject("WMT_MS_Capabilities").get("version"));
            } else {
              builder.success(false).reason("Not a standard OGC:WMS Capabilities response");
            }
          }
        }
      } catch (IOException e) {
        builder.success(false).reason(e.getClass().getSimpleName() + ": " + e.getMessage());
      }
    } catch (Exception e) {
      builder.success(false).reason(e.getClass().getSimpleName() + ": " + e.getMessage());
    }
    return builder.build();
  }
}
