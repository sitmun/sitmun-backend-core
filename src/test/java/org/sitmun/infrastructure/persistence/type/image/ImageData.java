package org.sitmun.infrastructure.persistence.type.image;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Image Data URI utility class test")
class ImageDataUriTest {

  @Test
  @DisplayName("A valid image data URI can be parsed")
  void parse() {
    String uri = "data:image/png;base64,base64Data";
    ImageDataUri dataUri = ImageDataUri.parse(uri);
    assertTrue(ImageDataUri.isDataUri(uri));
    assertNotNull(dataUri);
    assertEquals("png", dataUri.getFormat());
    assertEquals("base64Data", dataUri.getData());
    assertEquals(uri, dataUri.toDataUri());
  }

  @Test
  @DisplayName("Other kind of URIs are not parsed")
  void failParse() {
    assertFalse(ImageDataUri.isDataUri("http://www.google.com"));
    assertNull(ImageDataUri.parse("http://www.google.com"));
    assertFalse(ImageDataUri.isDataUri("data:plain/text;base64,base64Data"));
    assertNull(ImageDataUri.parse("data:plain/text;base64,base64Data"));
  }

}