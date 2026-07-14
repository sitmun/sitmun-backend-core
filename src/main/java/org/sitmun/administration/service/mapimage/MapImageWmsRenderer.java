package org.sitmun.administration.service.mapimage;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Credentials;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;
import org.sitmun.administration.service.extractor.HttpClientFactory;
import org.sitmun.domain.DomainConstants;
import org.sitmun.domain.service.Service;
import org.sitmun.infrastructure.variables.SystemVariableResolver;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
@Slf4j
class MapImageWmsRenderer {

  private static final String WMS_VERSION = "1.1.1";

  private final HttpClientFactory httpClientFactory;
  private final SystemVariableResolver systemVariableResolver;

  BufferedImage render(Service service, List<String> layerNames, MapImageRenderContext context) {
    if (!DomainConstants.Services.TYPE_WMS.equalsIgnoreCase(service.getType())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Service " + service.getId() + " is not WMS");
    }
    if (layerNames == null || layerNames.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Map source lacks layerNames");
    }

    String requestUrl = buildGetMapUrl(service, layerNames, context);
    Request.Builder requestBuilder = new Request.Builder().url(requestUrl).get();
    if (StringUtils.hasText(service.getUser()) && Boolean.TRUE.equals(service.getPasswordSet())) {
      requestBuilder.header("Authorization", Credentials.basic(service.getUser(), service.getPassword()));
    }

    try (Response response = httpClientFactory.executeRequest(requestBuilder.build())) {
      if (!response.isSuccessful()) {
        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "WMS request failed with HTTP " + response.code());
      }

      okhttp3.ResponseBody body = response.body();
      if (body == null) {
        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "WMS response is empty");
      }

      byte[] bytes = body.bytes();
      BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
      if (image == null) {
        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "WMS response is not a valid image");
      }
      return image;
    } catch (IOException e) {
      log.warn("Failed to fetch WMS image for service {}", service.getId(), e);
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to fetch WMS image", e);
    }
  }

  private String buildGetMapUrl(Service service, List<String> layerNames, MapImageRenderContext context) {
    String baseUrl = systemVariableResolver.resolve(service.getServiceURL(), null);
    HttpUrl parsedUrl = HttpUrl.parse(baseUrl);
    if (parsedUrl == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid service URL: " + baseUrl);
    }

    return Objects.requireNonNull(parsedUrl.newBuilder())
        .addQueryParameter("SERVICE", "WMS")
        .addQueryParameter("VERSION", WMS_VERSION)
        .addQueryParameter("REQUEST", "GetMap")
        .addQueryParameter("LAYERS", layerNames.stream().map(String::trim).filter(StringUtils::hasText).collect(Collectors.joining(",")))
        .addQueryParameter("STYLES", "")
        .addQueryParameter("FORMAT", "image/png")
        .addQueryParameter("TRANSPARENT", "true")
        .addQueryParameter("WIDTH", String.valueOf(context.width()))
        .addQueryParameter("HEIGHT", String.valueOf(context.height()))
        .addQueryParameter("SRS", context.srs())
        .addQueryParameter("BBOX", context.bbox().stream().map(String::valueOf).collect(Collectors.joining(",")))
        .build()
        .toString();
  }
}
