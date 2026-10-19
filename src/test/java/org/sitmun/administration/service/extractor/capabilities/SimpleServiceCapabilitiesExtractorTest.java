package org.sitmun.administration.service.extractor.capabilities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;
import okhttp3.Request;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sitmun.administration.service.extractor.HttpClientFactory;
import org.sitmun.administration.service.extractor.LocalHtmlServer;

@DisplayName("SimpleServiceCapabilitiesExtractor test")
class SimpleServiceCapabilitiesExtractorTest {

  private static LocalHtmlServer html;

  private final HttpClientFactory factory = new HttpClientFactory(List.of("*"));

  private final SimpleServiceCapabilitiesExtractor extractor =
      new SimpleServiceCapabilitiesExtractor(factory);

  @BeforeAll
  static void startHtml() throws IOException {
    html = LocalHtmlServer.start();
  }

  @AfterAll
  static void stopHtml() {
    html.close();
  }

  @Test
  @DisplayName("Extract from a GetCapabilities request to a WMS 1.3.0")
  void extractKnownWMSService130() {
    ExtractedMetadata doc =
        extractor.extract(
            request("https://www.ign.es/wms-inspire/ign-base?request=GetCapabilities"));
    assertNotNull(doc);
    assertTrue(doc.getSuccess());
    assertEquals("OGC:WMS 1.3.0", doc.getType());
    assertNull(doc.getReason());
    assertNotNull(doc.getAsText());
    assertThat(doc.getAsText()).startsWith("<?xml");
    assertNotNull(doc.getAsJson());
    assertTrue(doc.getAsJson().containsKey("WMS_Capabilities"));
  }

  @Test
  @DisplayName("Extract from a GetCapabilities request to a WMS 1.1.1")
  void extractKnownWMSService111() {
    ExtractedMetadata doc =
        extractor.extract(
            request(
                "https://www.ign.es/wms-inspire/ign-base?request=GetCapabilities&version=1.1.1"));
    assertNotNull(doc);
    assertTrue(doc.getSuccess());
    assertEquals("OGC:WMS 1.1.1", doc.getType());
    assertNull(doc.getReason());
    assertNotNull(doc.getAsText());
    assertThat(doc.getAsText()).startsWith("<?xml");
    assertNotNull(doc.getAsJson());
    assertTrue(doc.getAsJson().containsKey("WMT_MS_Capabilities"));
  }

  @ParameterizedTest(name = "{0} version {1}")
  @MethodSource("wfsCapabilitiesDocuments")
  @DisplayName("Extract from a GetCapabilities request to a WFS")
  void extractWfsCapabilities(String root, String version, String xml) throws IOException {
    try (MockWebServer server = new MockWebServer()) {
      server.start();
      server.enqueue(new MockResponse().setResponseCode(200).setBody(xml));
      ExtractedMetadata doc = extractor.extract(request(server.url("/wfs").toString()));
      assertNotNull(doc);
      assertTrue(doc.getSuccess());
      assertEquals("OGC:WFS " + version, doc.getType());
      assertNull(doc.getReason());
      assertNotNull(doc.getAsText());
      assertThat(doc.getAsText()).contains(root);
      assertNotNull(doc.getAsJson());
      assertTrue(doc.getAsJson().containsKey(root));
    }
  }

  private static Stream<Arguments> wfsCapabilitiesDocuments() {
    return Stream.of(
        Arguments.of(
            "WFS_Capabilities",
            "2.0.0",
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <WFS_Capabilities version="2.0.0">
              <ServiceIdentification><Title>demo</Title></ServiceIdentification>
            </WFS_Capabilities>
            """),
        Arguments.of(
            "wfs:WFS_Capabilities",
            "1.1.0",
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <wfs:WFS_Capabilities version="1.1.0" xmlns:wfs="http://www.opengis.net/wfs">
              <wfs:ServiceIdentification><wfs:Title>demo</wfs:Title></wfs:ServiceIdentification>
            </wfs:WFS_Capabilities>
            """));
  }

  @Test
  @DisplayName("Extract from a bad request to a WMS")
  void extractFailedService() {
    ExtractedMetadata doc = extractor.extract(request("https://www.ign.es/wms-inspire/ign-base?"));
    assertNotNull(doc);
    assertFalse(doc.getSuccess());
    assertEquals("Not a standard OGC:WMS or OGC:WFS Capabilities response", doc.getReason());
    assertNotNull(doc.getAsText());
    assertThat(doc.getAsText()).startsWith("<?xml");
    assertNotNull(doc.getAsJson());
    assertTrue(doc.getAsJson().containsKey("ServiceExceptionReport"));
  }

  @Test
  @DisplayName("Extract from a request to HTML page")
  void extractHtmlPage() {
    ExtractedMetadata doc = extractor.extract(request(html.url("/")));
    assertNotNull(doc);
    assertFalse(doc.getSuccess());
    assertEquals("Not a standard OGC:WMS or OGC:WFS Capabilities response", doc.getReason());
    assertNotNull(doc.getAsText());
    assertThat(doc.getAsText()).startsWith("<!DOCTYPE");
  }

  @Test
  @DisplayName("A non-XML 404 reports the HTTP status")
  void extract404Page() {
    ExtractedMetadata doc = extractor.extract(request(html.url("/not-found")));
    assertNotNull(doc);
    assertFalse(doc.getSuccess());
    assertEquals("HTTP 404", doc.getReason());
    assertNotNull(doc.getAsText());
    assertThat(doc.getAsText()).startsWith("<html");
    assertNull(doc.getAsJson());
  }

  @Test
  @DisplayName("Extract from a request to an nonexistent domain")
  void extractNonExistentDomain() {
    ExtractedMetadata doc = extractor.extract(request("https://fake"));
    assertNotNull(doc);
    assertFalse(doc.getSuccess());
    assertNotNull(doc.getReason());
    assertThat(doc.getReason()).startsWith("UnknownHostException:").contains("fake");
    assertNull(doc.getAsText());
    assertNull(doc.getAsJson());
  }

  private static Request request(String url) {
    return new Request.Builder().url(url).header("Accept", "*/*").build();
  }
}
