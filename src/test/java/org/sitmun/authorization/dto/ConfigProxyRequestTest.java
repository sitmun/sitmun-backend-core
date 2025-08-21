package org.sitmun.authorization.dto;

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
  @DisplayName("Deserializes id_token JSON property into token field")
  void deserializesIdTokenProperty() throws Exception {
    String json =
        """
        {
          "appId": 10,
          "terId": 20,
          "type": "SQL",
          "typeId": 30,
          "method": "POST",
          "parameters": {"A":"1"},
          "requestBody": {"B":"2"},
          "id_token": "jwt-token-value"
        }
        """;

    ConfigProxyRequest request = objectMapper.readValue(json, ConfigProxyRequest.class);

    assertEquals(10, request.getAppId());
    assertEquals(20, request.getTerId());
    assertEquals("SQL", request.getType());
    assertEquals(30, request.getTypeId());
    assertEquals("POST", request.getMethod());
    assertEquals("1", request.getParameters().get("A"));
    assertEquals("2", request.getRequestBody().get("B"));
    assertEquals("jwt-token-value", request.getToken());
  }

  @Test
  @DisplayName("Serializes token field as id_token JSON property")
  void serializesTokenAsIdTokenProperty() throws JsonProcessingException {
    Map<String, String> params = new HashMap<>();
    params.put("X", "9");
    Map<String, String> body = new HashMap<>();
    body.put("Y", "8");

    ConfigProxyRequest request =
        ConfigProxyRequest.builder()
            .appId(1)
            .terId(2)
            .type("WMTS")
            .typeId(3)
            .method("GET")
            .parameters(params)
            .requestBody(body)
            .token("abc.def.ghi")
            .build();

    String json = objectMapper.writeValueAsString(request);

    JsonNode node = objectMapper.readTree(json);
    assertEquals("abc.def.ghi", node.get("id_token").asText());
    assertNull(node.get("token"));
    assertEquals(1, node.get("appId").asInt());
    assertEquals(2, node.get("terId").asInt());
    assertEquals("WMTS", node.get("type").asText());
    assertEquals(3, node.get("typeId").asInt());
    assertEquals("GET", node.get("method").asText());
    assertEquals("9", node.get("parameters").get("X").asText());
    assertEquals("8", node.get("requestBody").get("Y").asText());
  }

  @Test
  @DisplayName("Ignores legacy token JSON property (must use id_token)")
  void ignoresLegacyTokenProperty() throws Exception {
    String json =
        """
        {
          "appId": 1,
          "terId": 1,
          "type": "SQL",
          "typeId": 1,
          "method": "GET",
          "token": "legacy-token"
        }
        """;

    ConfigProxyRequest request = objectMapper.readValue(json, ConfigProxyRequest.class);
    assertNull(request.getToken());
  }
}
