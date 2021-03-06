package org.sitmun.plugin.core.service;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.json.JSONObject;
import org.json.XML;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
class SimpleServiceCapabilitiesExtractor implements ServiceCapabilitiesExtractor {

  @Override
  public ServiceCapabilities extract(String url) {
    OkHttpClient client = new OkHttpClient();
    ServiceCapabilities.ServiceCapabilitiesBuilder builder = new ServiceCapabilities.ServiceCapabilitiesBuilder();

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
        builder.success(false).reason(e.getMessage());
      }
    } catch (Exception e) {
      builder.success(false).reason(e.getMessage());
    }
    return builder.build();
  }
}
