package org.sitmun.administration.service.mapimage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import javax.imageio.ImageIO;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sitmun.administration.service.extractor.HttpClientFactory;
import org.sitmun.domain.service.Service;
import org.sitmun.infrastructure.config.SystemVariableProperties;
import org.sitmun.infrastructure.variables.SystemVariableResolver;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
@DisplayName("MapImageWmsRenderer")
class MapImageWmsRendererTest {

  @Mock private HttpClientFactory httpClientFactory;

  private final SystemVariableResolver systemVariableResolver =
      new SystemVariableResolver(new SystemVariableProperties());

  @Test
  @DisplayName("render rejects invalid service URL")
  void renderRejectsInvalidServiceUrl() {
    MapImageWmsRenderer renderer = buildRenderer();
    Service service = buildService(1, "not-a-url");

    assertThatThrownBy(() -> renderer.render(service, List.of("layer_a"), context()))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(exception -> {
          ResponseStatusException response = (ResponseStatusException) exception;
          assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
          assertThat(response.getReason()).isEqualTo("Invalid service URL: not-a-url");
        });
  }

  @Test
  @DisplayName("render rejects upstream non-success status")
  void renderRejectsUpstreamNonSuccessStatus() throws Exception {
    MapImageWmsRenderer renderer = buildRenderer();
    Service service = buildService(1, "https://maps.example.com/wms");
    when(httpClientFactory.executeRequest(any(Request.class))).thenReturn(response(502, null, "text/plain"));

    assertThatThrownBy(() -> renderer.render(service, List.of("layer_a"), context()))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(exception -> assertThat(((ResponseStatusException) exception).getReason())
            .isEqualTo("WMS request failed with HTTP 502"));
  }

  @Test
  @DisplayName("render rejects non-image response body")
  void renderRejectsNonImageResponseBody() throws Exception {
    MapImageWmsRenderer renderer = buildRenderer();
    Service service = buildService(1, "https://maps.example.com/wms");
    when(httpClientFactory.executeRequest(any(Request.class)))
        .thenReturn(response(200, "not-an-image".getBytes(), "text/plain"));

    assertThatThrownBy(() -> renderer.render(service, List.of("layer_a"), context()))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(exception -> assertThat(((ResponseStatusException) exception).getReason())
            .isEqualTo("WMS response is not a valid image"));
  }

  @Test
  @DisplayName("render adds basic auth header when credentials are configured")
  void renderAddsBasicAuthHeaderWhenCredentialsAreConfigured() throws Exception {
    MapImageWmsRenderer renderer = buildRenderer();
    Service service = buildService(1, "https://maps.example.com/wms");
    service.setUser("demo");
    service.setPassword("secret");
    when(httpClientFactory.executeRequest(any(Request.class))).thenReturn(response(200, png(Color.RED), "image/png"));

    renderer.render(service, List.of("layer_a"), context());

    ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
    verify(httpClientFactory).executeRequest(requestCaptor.capture());
    assertThat(requestCaptor.getValue().header("Authorization")).startsWith("Basic ");
  }

  private MapImageWmsRenderer buildRenderer() {
    return new MapImageWmsRenderer(httpClientFactory, systemVariableResolver);
  }

  private static MapImageRenderContext context() {
    return new MapImageRenderContext(List.of(1d, 2d, 3d, 4d), 256, 128, "EPSG:4326");
  }

  private static Service buildService(int id, String url) {
    Service service = new Service();
    service.setId(id);
    service.setType("WMS");
    service.setServiceURL(url);
    return service;
  }

  private static Response response(int code, byte[] body, String mediaType) {
    return new Response.Builder()
        .request(new Request.Builder().url("https://maps.example.com/wms").build())
        .protocol(Protocol.HTTP_1_1)
        .code(code)
        .message(code == 200 ? "OK" : "Error")
        .body(ResponseBody.create(okhttp3.MediaType.parse(mediaType), body == null ? new byte[0] : body))
        .build();
  }

  private static byte[] png(Color color) throws IOException {
    BufferedImage image = new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB);
    Graphics2D graphics = image.createGraphics();
    try {
      graphics.setColor(color);
      graphics.fillRect(0, 0, 4, 4);
    } finally {
      graphics.dispose();
    }

    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      ImageIO.write(image, "png", out);
      return out.toByteArray();
    }
  }
}
