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

import org.sitmun.domain.ImageProperty;
import org.sitmun.domain.ImageScalingProperties;
import org.sitmun.infrastructure.persistence.exception.IllegalImageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

@Service
public class ImageTransformer {

	private static final Logger logger = LoggerFactory.getLogger(ImageTransformer.class);
	
	private static ApplicationContext context;

	private final ImageScalingProperties imageScalingProperties;
	
	public ImageTransformer(ApplicationContext context, ImageScalingProperties imageScalingProperties) {
		ImageTransformer.context = context;
		this.imageScalingProperties = imageScalingProperties;
	}
	
	public static ImageTransformer getInstance() {
		return context.getBean(ImageTransformer.class);
	}

	public String scaleImage(String image, String type) throws IllegalImageException {
		BufferedImage scaledImage = null;
		String imageFormat = "";
		String result = null;
		
		if (image != null && !image.isBlank()) {
			int width = imageScalingProperties.getDefaultWidth();
			int height = imageScalingProperties.getDefaultHeight();
			ImageProperty imgProperty = imageScalingProperties.getImagePropertyByType(type);
			if (imgProperty != null) {
				width = imgProperty.getWidth();
				height = imgProperty.getHeight();
			}
			
			try {
				if (image.startsWith("http")) {
			        URL url = new URL(image);
					imageFormat = getImageFormat(url);
					validateFormat(imageFormat);
					scaledImage = scaleImageFromURL(url, width, height);
				} else {
					imageFormat = image.substring(image.indexOf("/") + 1, image.indexOf(";"));
					validateFormat(imageFormat);
					scaledImage = scaleImageFromBase64(image.split(",")[1], width, height);
				}
				result = scaledImage != null ? encodeImageToBase64(scaledImage, imageFormat.toLowerCase()) : image;
			} catch (IllegalImageException e) {
				logger.error("IllegalImageException", e);
				throw e;
			} catch (Exception e) {
				logger.error("Image scaling error", e);
				throw new IllegalImageException("Image scaling error");
			}
		}
		return result;
	}

    private BufferedImage scaleImageFromURL(URL url, int width, int height) throws Exception {
        BufferedImage originalImage = ImageIO.read(url);
        if (originalImage == null) {
        	throw  new IllegalImageException("Url is not a image"); 
        }
        if (originalImage.getHeight() == height && originalImage.getWidth() == width) {
        	return null;
        }
        return scaleImage(originalImage, width, height);
    }

    private BufferedImage scaleImageFromBase64(String base64String, int width, int height) throws Exception {
        byte[] imageBytes = Base64.getDecoder().decode(base64String);
        InputStream is = new ByteArrayInputStream(imageBytes);
        BufferedImage originalImage = ImageIO.read(is);
        if (originalImage == null) {
        	throw  new IllegalImageException("Image file not valid");
        }
        if (originalImage.getHeight() == height && originalImage.getWidth() == width) {
        	return null;
        }
        return scaleImage(originalImage, width, height);
    }

    private BufferedImage scaleImage(BufferedImage originalImage, int width, int height) {
        BufferedImage scaledImage = new BufferedImage(width, height, originalImage.getType());
        Graphics2D g2d = scaledImage.createGraphics();
        g2d.drawImage(originalImage.getScaledInstance(width, height, Image.SCALE_SMOOTH), 0, 0, null);
        g2d.dispose();
        return scaledImage;
    }

    private String encodeImageToBase64(BufferedImage image, String formatName) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, formatName, baos);
        byte[] imageBytes = baos.toByteArray();
        String encodeImage = Base64.getEncoder().encodeToString(imageBytes);
        encodeImage = "data:image/".concat(formatName).concat(";base64,").concat(encodeImage);
        return encodeImage;
    }
    
    private String getImageFormat(URL url) throws Exception {
        try (ImageInputStream imageInputStream = ImageIO.createImageInputStream(url.openStream())) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInputStream);
            if (readers.hasNext()) {
                ImageReader reader = readers.next();
                return reader.getFormatName();
            }
        }
        throw new IllegalImageException("Supplied url is not a image"); // Image format can not be detected 
    }
    
    private void validateFormat(String format) throws IllegalImageException {
    	if (!imageScalingProperties.getSupportedFormats().contains(format)) {
    		throw new IllegalImageException("Image format not suported (" + format + ")");
    	}
    }

}
