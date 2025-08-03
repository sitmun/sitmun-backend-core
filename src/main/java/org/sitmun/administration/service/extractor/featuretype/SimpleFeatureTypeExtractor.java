package org.sitmun.administration.service.extractor.featuretype;

import java.io.IOException;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.json.JSONObject;
import org.json.XML;
import org.sitmun.administration.service.extractor.HttpClientFactory;
import org.sitmun.administration.service.extractor.featuretype.ExtractedMetadata.ExtractedMetadataBuilder;
import org.springframework.stereotype.Service;

@Service
public class SimpleFeatureTypeExtractor implements FeatureTypeExtractor {

  private final HttpClientFactory httpClientFactory;

  public SimpleFeatureTypeExtractor(HttpClientFactory httpClientFactory) {
    this.httpClientFactory = httpClientFactory;
  }

  @Override
  public ExtractedMetadata extract(String url) {
    ExtractedMetadataBuilder builder = new ExtractedMetadataBuilder();

    Request request = new Request.Builder().url(url).header("Accept", "*/*").build();
    try (Response response = httpClientFactory.executeRequest(request)) {
      processResponse(builder, response);
    } catch (IOException e) {
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

        json.toMap().keySet().stream()
            .filter(it -> it.endsWith(":schema"))
            .findFirst()
            .ifPresentOrElse(
                node -> processRootNode(builder, json, node),
                () -> builder.success(false).reason("Unmanaged XML response"));
      }
    }
  }

  private static void processRootNode(
      ExtractedMetadataBuilder builder, JSONObject json, String root) {
    JSONObject schema = json.getJSONObject(root);
    String[] qname = root.split(":");
    String xmlnsKey = "xmlns" + (qname.length == 2 ? ':' + qname[0] : "");
    boolean isXSD = "http://www.w3.org/2001/XMLSchema".equals(schema.optString(xmlnsKey));
    if (!isXSD) {
      builder.success(false).reason("Not a XML Schema");
    } else if (!schema.has("xmlns:gml")) {
      builder.success(false).reason("Not a DescribeFeatureType response");
    } else {
      builder.type("GML Schema").success(true);
    }
  }
}
