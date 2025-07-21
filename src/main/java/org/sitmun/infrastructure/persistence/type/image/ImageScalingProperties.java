package org.sitmun.infrastructure.persistence.type.image;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix = "sitmun.ui.image")
@Getter
@Setter
public class ImageScalingProperties {

	private List<String> supportedFormats;
	private int defaultWidth;
	private int defaultHeight;
	private List<ImageProperty> sizes;
	
	public ImageProperty getImagePropertyByType(String type) {
		ImageProperty result = null;
		if (sizes != null && !sizes.isEmpty()) {
			List<ImageProperty> filterResults = sizes.stream().filter(s -> s.getType().equals(type)).toList();
			if (!filterResults.isEmpty()) {
				result = filterResults.get(0);
			}
		}
		return result;
	}
}
