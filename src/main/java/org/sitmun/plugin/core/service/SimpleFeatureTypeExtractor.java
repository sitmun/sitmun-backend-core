package org.sitmun.plugin.core.service;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.json.JSONObject;
import org.json.XML;
import org.sitmun.plugin.core.service.ExtractedMetadata.ExtractedMetadataBuilder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Optional;

@Service
class SimpleFeatureTypeExtractor implements FeatureTypeExtractor {

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

            Optional<String> root = json.toMap().keySet().stream().filter(it -> it.endsWith(":schema")).findFirst();
            if (root.isPresent()) {
              JSONObject schema = json.getJSONObject(root.get());
              String[] qname = root.get().split(":");
              String xmlnsKey = "xmlns" + (qname.length == 2 ? ":" + qname[0] : "");
              boolean isXSD = "http://www.w3.org/2001/XMLSchema".equals(schema.optString(xmlnsKey));
              if (!isXSD) {
                builder.success(false).reason("Not a XML Schema");
              } else if (!schema.has("xmlns:gml")) {
                builder.success(false).reason("Not a DescribeFeatureType response");
              } else {
                builder.type("GML Schema").success(true);
              }
            } else {
              builder.success(false).reason("Unmanaged XML response");
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
