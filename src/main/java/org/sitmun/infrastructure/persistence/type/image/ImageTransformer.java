package org.sitmun.infrastructure.persistence.type.image;

import lombok.extern.slf4j.Slf4j;
import org.sitmun.infrastructure.persistence.exception.IllegalImageException;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Base64;
import java.util.Iterator;

@Component
@Slf4j
public class ImageTransformer {

  private final ImageScalingProperties imageScalingProperties;

  public ImageTransformer(ImageScalingProperties imageScalingProperties) {
    this.imageScalingProperties = imageScalingProperties;
  }

  public String scaleImage(String image, String type) throws IllegalImageException {
    if (image == null || image.isBlank()) {
      return null;
    }

    int width = imageScalingProperties.getDefaultWidth();
    int height = imageScalingProperties.getDefaultHeight();
    ImageProperty imgProperty = imageScalingProperties.getImagePropertyByType(type);

    if (imgProperty != null) {
      width = imgProperty.getWidth();
      height = imgProperty.getHeight();
    }

    try {
      URI uri = new URI(image);
      String scheme = uri.getScheme();
      BufferedImage scaledImage;
      String imageFormat;

      if (scheme.startsWith("http")) {
        URL url = uri.toURL();
        imageFormat = getImageFormat(url);
        validateFormat(imageFormat);
        scaledImage = scaleImageFromURL(url, width, height);
      } else if (scheme.startsWith("data") && ImageDataUri.isDataUri(image)) {
        ImageDataUri dataUri = ImageDataUri.parse(image);
        imageFormat = dataUri.getFormat();
        validateFormat(imageFormat);
        scaledImage = scaleImageFromBase64(dataUri.getData(), width, height);
      } else {
        log.error("Unsupported image URI scheme: {}", scheme);
        throw new IllegalImageException("Unsupported image URI scheme: " + scheme);
      }
      return encodeImageToBase64(scaledImage, imageFormat.toLowerCase());

    } catch (URISyntaxException | MalformedURLException e) {
      log.error("Unsupported URL format: {}", image, e);
      throw new IllegalImageException("Unsupported URL format: " + image);
    }
  }

  private BufferedImage scaleImageFromURL(URL url, int width, int height) {
    try {
      BufferedImage originalImage = ImageIO.read(url);
      if (originalImage == null) {
        log.error("No registered image reader for {}", url);
        throw new IllegalImageException("No registered image reader for " + url);
      }
      return scaleImage(originalImage, width, height);
    } catch (IOException e) {
      log.error("Failed to read image from URL {}", url, e);
      throw new IllegalImageException("IOException: " + e.getMessage());
    }
  }

  private BufferedImage scaleImageFromBase64(String base64String, int width, int height) {
    byte[] imageBytes = Base64.getDecoder().decode(base64String);
    try (InputStream is = new ByteArrayInputStream(imageBytes)) {
      BufferedImage originalImage = ImageIO.read(is);
      if (originalImage == null) {
        log.error("Failed to read image from decoded Base64");
        throw new IllegalImageException("Image file not valid");
      }
      return scaleImage(originalImage, width, height);
    } catch (IOException e) {
      log.error("Failed scale image en encoded in Base64 to {}x{}", width, height, e);
      throw new IllegalImageException("IOException: " + e.getMessage());
    }
  }

  private BufferedImage scaleImage(BufferedImage originalImage, int width, int height) {
    if (originalImage.getHeight() == height && originalImage.getWidth() == width) {
      return originalImage;
    }
    BufferedImage scaledImage = new BufferedImage(width, height, originalImage.getType());
    Graphics2D g2d = scaledImage.createGraphics();
    g2d.drawImage(originalImage.getScaledInstance(width, height, Image.SCALE_SMOOTH), 0, 0, null);
    g2d.dispose();
    return scaledImage;
  }

  private String encodeImageToBase64(BufferedImage image, String formatName) {
    try(ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      ImageIO.write(image, formatName, baos);
      byte[] imageBytes = baos.toByteArray();
      return ImageDataUri.builder().format(formatName).data(Base64.getEncoder().encodeToString(imageBytes)).build().toDataUri();
    } catch (IOException e) {
      log.error("Failed to encode image to Base64", e);
      throw new IllegalImageException("Failed to encode image to Base64: " + e.getMessage());
    }
  }

  private String getImageFormat(URL url) {
    try (ImageInputStream imageInputStream = ImageIO.createImageInputStream(url.openStream())) {
      Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInputStream);
      if (readers.hasNext()) {
        ImageReader reader = readers.next();
        return reader.getFormatName();
      }
    } catch (IOException e) {
      log.error("Failed to get image format from URL {}", url, e);
      throw new IllegalImageException("Failed to get image format from URL: " + e.getMessage());
    }
    throw new IllegalImageException("Supplied url is not a image"); // Image format can not be detected
  }

  private void validateFormat(String format) throws IllegalImageException {
    for (String supportedFormat : imageScalingProperties.getSupportedFormats()) {
      if (format.equalsIgnoreCase(supportedFormat)) {
        return;
      }
    }
    throw new IllegalImageException("Image format not supported (" + format + ")");
  }
}

