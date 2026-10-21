package org.sitmun.authorization.proxy.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.authorization.proxy.protocols.wms.WmsPayloadDto;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class WmsPayloadDtoTest {

  private final ObjectMapper objectMapper =
      JsonMapper.builder().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();

  @Test
  @DisplayName("JSON maps vary, uri, method, parameters, and security")
  void deserializesFromJson() throws JacksonException {
    String json =
        """
        {
          "vary": ["param1", "param2"],
          "uri": "https://example.com/wms",
          "method": "POST",
          "parameters": {
            "format": "image/jpeg",
            "version": "1.1.1"
          },
          "security": {
            "type": "http",
            "scheme": "basic",
            "username": "user",
            "password": "pass"
          }
        }
        """;

    WmsPayloadDto dto = objectMapper.readValue(json, WmsPayloadDto.class);

    assertEquals(Arrays.asList("param1", "param2"), dto.getVary());
    assertEquals("https://example.com/wms", dto.getUri());
    assertEquals("POST", dto.getMethod());
    assertEquals("image/jpeg", dto.getParameters().get("format"));
    assertEquals("1.1.1", dto.getParameters().get("version"));
    assertEquals("http", dto.getSecurity().getType());
    assertEquals("basic", dto.getSecurity().getScheme());
    assertEquals("user", dto.getSecurity().getUsername());
    assertEquals("pass", dto.getSecurity().getPassword());
  }
}
