package org.sitmun.infrastructure.persistence.type.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.infrastructure.persistence.exception.IllegalImageException;

@DisplayName("ImageTransformer")
class ImageTransformerTest {

  static final String SVG_MARKUP =
      "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"10\" height=\"10\"><rect width=\"10\" height=\"10\" fill=\"#f00\"/></svg>";
  static final String SVG_DATA_URI =
      "data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxMCIgaGVpZ2h0PSIxMCI+PHJlY3Qgd2lkdGg9IjEwIiBoZWlnaHQ9IjEwIiBmaWxsPSIjZjAwIi8+PC9zdmc+";
  static final String PNG_8X8 =
      "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAgAAAAIAQMAAAD+wSzIAAAABlBMVEX///+/v7+jQ3Y5AAAADklEQVQI12P4AIX8EAgALgAD/aNpbtEAAAAASUVORK5CYII";

  private ImageTransformer transformer;

  @BeforeEach
  void setUp() {
    ImageScalingProperties properties = new ImageScalingProperties();
    properties.setSupportedFormats(List.of("png", "jpg", "jpeg", "svg"));
    properties.setDefaultWidth(125);
    properties.setDefaultHeight(125);
    transformer = new ImageTransformer(properties);
  }

  @Test
  @DisplayName("SVG data URI is stored as svg+xml without raster scaling")
  void svgDataUriIsNotRasterized() {
    String stored = transformer.scaleImage(SVG_DATA_URI, "menu");

    assertThat(stored).startsWith("data:image/svg+xml;base64,");
    byte[] payload = Base64.getDecoder().decode(ImageDataUri.parse(stored).getData());
    assertThat(new String(payload, StandardCharsets.UTF_8)).contains("<svg");
  }

  @Test
  @DisplayName("PNG data URI is still scaled to the type size")
  void pngDataUriIsStillScaled() throws Exception {
    String stored = transformer.scaleImage(PNG_8X8, "");

    assertThat(stored).startsWith("data:image/png;base64,");
    byte[] payload = Base64.getDecoder().decode(ImageDataUri.parse(stored).getData());
    BufferedImage image = ImageIO.read(new ByteArrayInputStream(payload));
    assertThat(image.getWidth()).isEqualTo(125);
    assertThat(image.getHeight()).isEqualTo(125);
  }

  @Test
  @DisplayName("HTTP URL with .svg path is stored as svg+xml data URI")
  void svgHttpUrlIsStoredAsDataUri() throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext(
        "/icon.svg",
        exchange -> {
          byte[] body = SVG_MARKUP.getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().add("Content-Type", "image/svg+xml");
          exchange.sendResponseHeaders(200, body.length);
          try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
          }
        });
    server.start();
    try {
      String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/icon.svg";
      String stored = transformer.scaleImage(url, "");

      assertThat(stored).startsWith("data:image/svg+xml;base64,");
      byte[] payload = Base64.getDecoder().decode(ImageDataUri.parse(stored).getData());
      assertThat(new String(payload, StandardCharsets.UTF_8)).contains("<svg");
    } finally {
      server.stop(0);
    }
  }

  @Test
  @DisplayName("Unsupported raster format is still rejected")
  void unsupportedRasterFormatIsRejected() {
    assertThatThrownBy(
            () -> transformer.scaleImage("data:image/gif;base64,R0lGODlhAQABAAAAACw=", ""))
        .isInstanceOf(IllegalImageException.class)
        .hasMessageContaining("gif");
  }
}
