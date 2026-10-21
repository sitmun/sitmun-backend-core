package org.sitmun.administration.service.extractor.capabilities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;
import okhttp3.Request;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sitmun.administration.service.extractor.HttpClientFactory;

@DisplayName("SimpleServiceCapabilitiesExtractor test")
class SimpleServiceCapabilitiesExtractorTest {

  private final HttpClientFactory factory = new HttpClientFactory(List.of("*"));

  private final SimpleServiceCapabilitiesExtractor extractor =
      new SimpleServiceCapabilitiesExtractor(factory);

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
  void extractFailedService() throws IOException {
    String xml =
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <ServiceExceptionReport version="1.3.0">
          <ServiceException code="MissingParameterValue">request</ServiceException>
        </ServiceExceptionReport>
        """;
    try (MockWebServer server = new MockWebServer()) {
      server.start();
      server.enqueue(new MockResponse().setResponseCode(200).setBody(xml));
      ExtractedMetadata doc = extractor.extract(request(server.url("/wms").toString()));
      assertNotNull(doc);
      assertFalse(doc.getSuccess());
      assertEquals("Not a standard OGC:WMS or OGC:WFS Capabilities response", doc.getReason());
      assertNotNull(doc.getAsText());
      assertThat(doc.getAsText()).startsWith("<?xml");
      assertNotNull(doc.getAsJson());
      assertTrue(doc.getAsJson().containsKey("ServiceExceptionReport"));
    }
  }

  private static Request request(String url) {
    return new Request.Builder().url(url).header("Accept", "*/*").build();
  }
}
