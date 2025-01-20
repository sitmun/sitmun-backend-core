package org.sitmun.infrastructure.utils;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Base64;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

public class ImageTransformer {
	
	private static final String DEFAULT_IMAGE_FORMAT = "png";
	private static final int DEFAULT_IMAGE_HEIGHT = 90;
	private static final int DEFAULT_IMAGE_WIDTH = 90;
	private static final int DEFAULT_ICON_HEIGHT = 25;
	private static final int DEFAULT_ICON_WIDTH = 25;
	private static final int DEFAULT_APP_IMAGE_HEIGHT = 125;
	private static final int DEFAULT_APP_IMAGE_WIDTH = 125;

	public static String scaleImage(String image, String type) {
		BufferedImage scaledImage = null;
		String imageFormat = "";
		String result = null;
		
		if (image != null && !image.isBlank()) {
			int width = DEFAULT_APP_IMAGE_WIDTH;
			int height = DEFAULT_APP_IMAGE_HEIGHT;
			
			if ("menu".equals(type)) {
				width = DEFAULT_IMAGE_WIDTH;
				height = DEFAULT_IMAGE_HEIGHT;
			} else if ("list".equals(type)) {
				width = DEFAULT_ICON_WIDTH;
				height = DEFAULT_ICON_HEIGHT;
			}
			
			try {
				if (image.startsWith("http")) {
			        URL url = new URL(image);
					scaledImage = scaleImageFromURL(url, width, height);
					// Si no ha sido necesario escalar la imagen no es necesario el formato
					imageFormat = scaledImage != null ? getImageFormat(url) : "";
				} else {
					scaledImage = scaleImageFromBase64(image.split(",")[1], width, height);
					imageFormat = image.substring(image.indexOf("/") + 1, image.indexOf(";"));
				}
				result = scaledImage != null ? encodeImageToBase64(scaledImage, imageFormat.toLowerCase()) : image;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return result;
	}
	
	// Escalar desde una URL
    private static BufferedImage scaleImageFromURL(URL url, int width, int height) throws Exception {
        BufferedImage originalImage = ImageIO.read(url); // Leer imagen desde la URL
        if (originalImage.getHeight() == height && originalImage.getWidth() == width) {
        	return null;
        }
        return scaleImage(originalImage, width, height);
    }

    // Escalar desde Base64
    private static BufferedImage scaleImageFromBase64(String base64String, int width, int height) throws Exception {
        // Decodificar Base64 a bytes
        byte[] imageBytes = Base64.getDecoder().decode(base64String);
        InputStream is = new ByteArrayInputStream(imageBytes);
        BufferedImage originalImage = ImageIO.read(is); // Leer imagen desde los bytes
        if (originalImage.getHeight() == height && originalImage.getWidth() == width) {
        	return null;
        }
        return scaleImage(originalImage, width, height);
    }

    // Escalar una imagen (utilitario común)
    private static BufferedImage scaleImage(BufferedImage originalImage, int width, int height) {
        // Crear una imagen nueva escalada
        BufferedImage scaledImage = new BufferedImage(width, height, originalImage.getType());
        Graphics2D g2d = scaledImage.createGraphics();
        g2d.drawImage(originalImage.getScaledInstance(width, height, Image.SCALE_SMOOTH), 0, 0, null);
        g2d.dispose(); // Liberar recursos
        return scaledImage;
    }

    // Codificar una imagen a Base64
    private static String encodeImageToBase64(BufferedImage image, String formatName) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, formatName, baos);
        byte[] imageBytes = baos.toByteArray();
        String encodeImage = Base64.getEncoder().encodeToString(imageBytes);
        encodeImage = "data:image/".concat(formatName).concat(";base64,").concat(encodeImage);
        return encodeImage;
    }
    
    private static String getImageFormat(URL url) throws Exception {
        try (ImageInputStream imageInputStream = ImageIO.createImageInputStream(url.openStream())) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInputStream);
            if (readers.hasNext()) {
                ImageReader reader = readers.next();
                return reader.getFormatName(); // Retorna el formato, como "JPEG" o "PNG"
            }
        }
        return DEFAULT_IMAGE_FORMAT; // No se pudo detectar
    }

}
