package org.sitmun.domain.cartography.parameter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Converter
public class FeatureInfoFormatOptionsConverter
    implements AttributeConverter<FeatureInfoFormatOptions, String> {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(FeatureInfoFormatOptionsConverter.class);

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public String convertToDatabaseColumn(FeatureInfoFormatOptions attribute) {
    if (attribute == null || attribute.isEmpty()) {
      return null;
    }
    try {
      return objectMapper.writeValueAsString(stored(attribute));
    } catch (final JsonProcessingException e) {
      LOGGER.error("JSON writing error", e);
    }
    return null;
  }

  @Override
  public FeatureInfoFormatOptions convertToEntityAttribute(String dbData) {
    if (dbData == null || dbData.isBlank()) {
      return null;
    }
    try {
      FeatureInfoFormatOptions options =
          objectMapper.readValue(dbData, FeatureInfoFormatOptions.class);
      return options == null || options.isEmpty() ? null : options;
    } catch (final IOException e) {
      LOGGER.error("JSON reading error", e);
    }
    return null;
  }

  private static FeatureInfoFormatOptions stored(FeatureInfoFormatOptions source) {
    FeatureInfoFormatOptions copy = new FeatureInfoFormatOptions();
    copy.setFractionDigits(source.getFractionDigits());
    if (Boolean.TRUE.equals(source.getPadFractionDigits())) {
      copy.setPadFractionDigits(true);
    }
    if (source.getDateStyle() != null && !source.getDateStyle().isBlank()) {
      copy.setDateStyle(source.getDateStyle());
    }
    source.additionalProperties().forEach(copy::putAdditionalProperty);
    return copy;
  }
}
