package org.sitmun.authorization.proxy.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.authorization.proxy.protocols.jdbc.JdbcPayloadDto;
import org.sitmun.authorization.proxy.protocols.wms.WmsPayloadDto;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class ConfigProxyDtoTest {

  private final ObjectMapper objectMapper =
      JsonMapper.builder().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();

  @Test
  @DisplayName("WMS proxy JSON sets payload.type to OgcWmsPayload")
  void serializesWithOgcWmsPayload() throws JacksonException {
    WmsPayloadDto payload =
        WmsPayloadDto.builder().uri("https://example.com/wms").method("GET").build();

    ConfigProxyDto dto =
        ConfigProxyDto.builder().type("WMS").exp(1234567890L).payload(payload).build();

    String json = objectMapper.writeValueAsString(dto);
    JsonNode node = objectMapper.readTree(json);

    assertEquals("WMS", node.get("type").asText());
    assertEquals(1234567890L, node.get("exp").asLong());
    assertTrue(node.has("payload"));
    JsonNode payloadNode = node.get("payload");
    assertEquals("https://example.com/wms", payloadNode.get("uri").asText());
    assertEquals("GET", payloadNode.get("method").asText());
    assertEquals("OgcWmsPayload", payloadNode.get("type").asText());
  }

  @Test
  @DisplayName("SQL proxy JSON sets payload.type to DatasourcePayload")
  void serializesWithDatasourcePayload() throws JacksonException {
    JdbcPayloadDto payload =
        JdbcPayloadDto.builder()
            .uri("jdbc:postgresql://localhost:5432/testdb")
            .user("dbuser")
            .password("dbpass")
            .driver("org.postgresql.Driver")
            .sql("SELECT * FROM test_table")
            .build();

    ConfigProxyDto dto =
        ConfigProxyDto.builder().type("SQL").exp(1234567890L).payload(payload).build();

    String json = objectMapper.writeValueAsString(dto);
    JsonNode node = objectMapper.readTree(json);

    assertEquals("SQL", node.get("type").asText());
    assertEquals(1234567890L, node.get("exp").asLong());
    assertTrue(node.has("payload"));
    JsonNode payloadNode = node.get("payload");
    assertEquals("jdbc:postgresql://localhost:5432/testdb", payloadNode.get("uri").asText());
    assertEquals("dbuser", payloadNode.get("user").asText());
    assertEquals("dbpass", payloadNode.get("password").asText());
    assertEquals("org.postgresql.Driver", payloadNode.get("driver").asText());
    assertEquals("SELECT * FROM test_table", payloadNode.get("sql").asText());
    assertEquals("DatasourcePayload", payloadNode.get("type").asText());
  }
}
