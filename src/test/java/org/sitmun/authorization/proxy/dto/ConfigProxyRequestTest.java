package org.sitmun.authorization.proxy.dto;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ConfigProxyRequestTest {

  private final ObjectMapper objectMapper =
      new ObjectMapper()
          .findAndRegisterModules()
          .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  @Test
  @DisplayName("Deserializes proxy request without JWT fields")
  void deserializesWithoutTokenFields() throws Exception {
    String json =
        """
        {
          "appId": 10,
          "terId": 20,
          "type": "SQL",
          "typeId": 30,
          "method": "POST",
          "parameters": {"A":"1"},
          "requestBody": "{\\"B\\":\\"2\\"}"
        }
        """;

    ConfigProxyRequestDto request = objectMapper.readValue(json, ConfigProxyRequestDto.class);

    assertEquals(10, request.getAppId());
    assertEquals(20, request.getTerId());
    assertEquals("SQL", request.getType());
    assertEquals(30, request.getTypeId());
    assertEquals("POST", request.getMethod());
    assertEquals("1", request.getParameters().get("A"));
    assertEquals("{\"B\":\"2\"}", request.getRequestBody());
  }

  @Test
  @DisplayName("Serializes without id_token or token fields")
  void serializesWithoutTokenFields() throws JsonProcessingException {
    Map<String, String> params = new HashMap<>();
    params.put("X", "9");
    String requestBody = "{\"Y\":\"8\"}";

    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder()
            .appId(1)
            .terId(2)
            .type("WMTS")
            .typeId(3)
            .method("GET")
            .parameters(params)
            .requestBody(requestBody)
            .build();

    String json = objectMapper.writeValueAsString(request);

    JsonNode node = objectMapper.readTree(json);
    assertNull(node.get("id_token"));
    assertNull(node.get("token"));
    assertEquals(1, node.get("appId").asInt());
    assertEquals(2, node.get("terId").asInt());
    assertEquals("WMTS", node.get("type").asText());
    assertEquals(3, node.get("typeId").asInt());
    assertEquals("GET", node.get("method").asText());
    assertEquals("9", node.get("parameters").get("X").asText());
    assertEquals("{\"Y\":\"8\"}", node.get("requestBody").asText());
  }

  @Test
  @DisplayName("Ignores legacy id_token and token JSON properties")
  void ignoresLegacyTokenProperties() throws Exception {
    String json =
        """
        {
          "appId": 1,
          "terId": 1,
          "type": "SQL",
          "typeId": 1,
          "method": "GET",
          "token": "legacy-token",
          "id_token": "jwt-token-value"
        }
        """;

    ConfigProxyRequestDto request = objectMapper.readValue(json, ConfigProxyRequestDto.class);
    assertEquals(1, request.getAppId());
    String serialized = objectMapper.writeValueAsString(request);
    JsonNode node = objectMapper.readTree(serialized);
    assertNull(node.get("id_token"));
    assertNull(node.get("token"));
  }
}
