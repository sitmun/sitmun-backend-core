package org.sitmun.infrastructure.persistence.type.map;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@Converter
public class HashMapConverter implements AttributeConverter<Map<String, Object>, String> {

  private static final Logger LOGGER = LoggerFactory.getLogger(HashMapConverter.class);

  private final ObjectMapper objectMapper;

  public HashMapConverter() {
    objectMapper =
        JsonMapper.builder()
            .changeDefaultPropertyInclusion(
                incl ->
                    JsonInclude.Value.construct(
                        JsonInclude.Include.ALWAYS, JsonInclude.Include.ALWAYS))
            .build();
  }

  @Override
  public String convertToDatabaseColumn(Map<String, Object> attribute) {
    if (attribute == null) {
      return null;
    }
    try {
      return objectMapper.writeValueAsString(attribute);
    } catch (final JacksonException e) {
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
    JavaType type =
        objectMapper
            .getTypeFactory()
            .constructParametricType(Map.class, String.class, Object.class);
    try {
      return objectMapper.readValue(dbData, type);
    } catch (final JacksonException e) {
      LOGGER.error("JSON reading error", e);
    }
    return null;
  }
}
