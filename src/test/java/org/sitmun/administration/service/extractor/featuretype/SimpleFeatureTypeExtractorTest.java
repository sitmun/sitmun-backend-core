package org.sitmun.administration.service.extractor.featuretype;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.administration.service.extractor.HttpClientFactory;

@DisplayName("Simple Feature Type Extractor test")
class SimpleFeatureTypeExtractorTest {

  private final HttpClientFactory factory = new HttpClientFactory(List.of("*"));

  private final SimpleFeatureTypeExtractor extractor = new SimpleFeatureTypeExtractor(factory);

  @Test
  @DisplayName("Extract from a DescribeFeatureType request to a WFS 2.0")
  void extractKnownFeatureWFS20() {
    ExtractedMetadata doc =
        extractor.extract(
            "https://www.ign.es/wfs/redes-geodesicas?request=DescribeFeatureType&service=WFS&typeNames=RED_REGENTE");
    assertNotNull(doc);
    assertTrue(doc.getSuccess());
    assertEquals("GML Schema", doc.getType());
    assertNull(doc.getReason());
    assertNotNull(doc.getAsText());
    assertThat(doc.getAsText()).startsWith("<?xml");
    assertNotNull(doc.getAsJson());
    assertTrue(doc.getAsJson().containsKey("xsd:schema"));
  }

  @Test
  @DisplayName("Extract from a DescribeFeatureType request to a WFS 1.1.0")
  void extractKnownFeatureWFS110() {
    ExtractedMetadata doc =
        extractor.extract(
            "https://www.ign.es/wfs/redes-geodesicas?request=DescribeFeatureType&service=WFS&version=1.1.0&typeNames=RED_REGENTE");
    assertNotNull(doc);
    assertTrue(doc.getSuccess());
    assertEquals("GML Schema", doc.getType());
    assertNull(doc.getReason());
    assertNotNull(doc.getAsText());
    assertThat(doc.getAsText()).startsWith("<?xml");
    assertNotNull(doc.getAsJson());
    assertTrue(doc.getAsJson().containsKey("xsd:schema"));
  }

  @Test
  @DisplayName("Extract from a bad request to a WFS")
  void extractFailedService() {
    ExtractedMetadata doc = extractor.extract("https://www.ign.es/wfs/redes-geodesicas?");
    assertNotNull(doc);
    assertFalse(doc.getSuccess());
    assertEquals("Unmanaged XML response", doc.getReason());
    assertNotNull(doc.getAsText());
    assertThat(doc.getAsText()).startsWith("<?xml");
    assertNotNull(doc.getAsJson());
    assertTrue(doc.getAsJson().containsKey("ows:ExceptionReport"));
  }

  @Test
  @DisplayName("Extract from a request to HTML page")
  void extractHtmlPage() {
    ExtractedMetadata doc = extractor.extract("https://www.ign.es/");
    assertNotNull(doc);
    assertFalse(doc.getSuccess());
    assertEquals("Unmanaged XML response", doc.getReason());
    assertNotNull(doc.getAsText());
    assertThat(doc.getAsText()).startsWith("<!DOCTYPE");
  }

  @Test
  @DisplayName("Extract from a request to a not found page")
  void extract404Page() {
    ExtractedMetadata doc = extractor.extract("https://www.ign.es/not-found");
    assertNotNull(doc);
    assertFalse(doc.getSuccess());
    assertEquals("Not a well formed XML", doc.getReason());
    assertNotNull(doc.getAsText());
    assertThat(doc.getAsText()).startsWith("<html");
  }

  @Test
  @DisplayName("Extract from a request to an nonexistent domain")
  void extractNonExistentDomain() {
    ExtractedMetadata doc = extractor.extract("https://fake");
    assertNotNull(doc);
    assertFalse(doc.getSuccess());
    assertNotNull(doc.getReason());
    assertTrue(doc.getReason().startsWith("UnknownHostException: fake"));
    assertNull(doc.getAsText());
    assertNull(doc.getAsJson());
  }
}
