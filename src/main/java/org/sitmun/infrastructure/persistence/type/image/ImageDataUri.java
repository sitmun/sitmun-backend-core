package org.sitmun.infrastructure.persistence.type.image;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/** Represents an image data URI. */
@Getter
@Setter
@Builder
public class ImageDataUri {

  /** The format of the image (e.g., png, jpeg). */
  String format;

  /** The base64-encoded image data. */
  String data;

  /**
   * Converts the image data to a data URI string.
   *
   * @return the data URI string in the format "data:image/{format};base64,{data}"
   */
  public String toDataUri() {
    return String.format("data:image/%s;base64,%s", format, data);
  }

  /**
   * Checks if a given URI is a valid data URI.
   *
   * @param uri the URI to check
   * @return true if the URI is a valid data URI, false otherwise
   */
  static boolean isDataUri(String uri) {
    if (uri == null || !uri.startsWith("data:")) {
      return false;
    }
    String[] parts = uri.split(",");
    if (parts.length != 2) {
      return false;
    }
    String metadata = parts[0];
    String mimeType = metadata.substring(5, metadata.indexOf(";"));
    return mimeType.startsWith("image/");
  }

  /**
   * Parses a data URI string into an ImageDataUri object.
   *
   * @param uri the data URI string to parse
   * @return an ImageDataUri object if the URI is valid, null otherwise
   */
  public static ImageDataUri parse(String uri) {
    if (uri == null || !uri.startsWith("data:")) {
      return null;
    }
    String[] parts = uri.split(",");
    if (parts.length != 2) {
      return null;
    }
    String metadata = parts[0];
    String base64Data = parts[1];
    String mimeType = metadata.substring(5, metadata.indexOf(";"));
    if (mimeType.startsWith("image/")) {
      return ImageDataUri.builder().format(mimeType.substring(6)).data(base64Data).build();
    } else {
      return null;
    }
  }
}
