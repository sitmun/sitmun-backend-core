package org.sitmun.infrastructure.persistence.type.map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.io.IOException;
import java.util.Map;

@Converter
public class HashMapConverter implements AttributeConverter<Map<String, Object>, String> {

  private static final Logger LOGGER = LoggerFactory.getLogger(HashMapConverter.class);

  private final ObjectMapper objectMapper;

  public HashMapConverter() {
    objectMapper = new ObjectMapper();
    objectMapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);
  }

  @Override
  public String convertToDatabaseColumn(Map<String, Object> attribute) {
    try {
      return objectMapper.writeValueAsString(attribute);
    } catch (final JsonProcessingException e) {
      LOGGER.error("JSON writing error", e);
    }
    return null;
  }

  @Override
  public Map<String, Object> convertToEntityAttribute(String dbData) {
    if (dbData == null) {
      return null;
    }
    if (dbData.isBlank()) {
      return null;
    }
    JavaType type = objectMapper.getTypeFactory().constructParametricType(Map.class, String.class, Object.class);
    try {
      return objectMapper.readValue(dbData, type);
    } catch (final IOException e) {
      LOGGER.error("JSON reading error", e);
    }
    return null;
  }
}
