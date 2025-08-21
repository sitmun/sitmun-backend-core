package org.sitmun.authorization.dto;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ConfigProxyDtoTest {

  private final ObjectMapper objectMapper =
      new ObjectMapper()
          .findAndRegisterModules()
          .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  @Test
  @DisplayName("Builder creates ConfigProxyDto with OgcWmsPayloadDto")
  void builderCreatesWithOgcWmsPayload() {
    Map<String, String> parameters = new HashMap<>();
    parameters.put("format", "image/png");
    parameters.put("version", "1.3.0");

    HttpSecurityDto security =
        HttpSecurityDto.builder()
            .type("http")
            .scheme("basic")
            .username("user")
            .password("pass")
            .build();

    OgcWmsPayloadDto payload =
        OgcWmsPayloadDto.builder()
            .vary(Arrays.asList("param1", "param2"))
            .uri("https://example.com/wms")
            .method("GET")
            .parameters(parameters)
            .security(security)
            .build();

    ConfigProxyDto dto =
        ConfigProxyDto.builder().type("WMS").exp(1234567890L).payload(payload).build();

    assertEquals("WMS", dto.getType());
    assertEquals(1234567890L, dto.getExp());
    assertEquals(payload, dto.getPayload());
    assertInstanceOf(OgcWmsPayloadDto.class, dto.getPayload());
  }

  @Test
  @DisplayName("Builder creates ConfigProxyDto with DatasourcePayloadDto")
  void builderCreatesWithDatasourcePayload() {
    DatasourcePayloadDto payload =
        DatasourcePayloadDto.builder()
            .vary(List.of("param1"))
            .uri("jdbc:postgresql://localhost:5432/testdb")
            .user("dbuser")
            .password("dbpass")
            .driver("org.postgresql.Driver")
            .sql("SELECT * FROM test_table")
            .build();

    ConfigProxyDto dto =
        ConfigProxyDto.builder().type("SQL").exp(1234567890L).payload(payload).build();

    assertEquals("SQL", dto.getType());
    assertEquals(1234567890L, dto.getExp());
    assertEquals(payload, dto.getPayload());
    assertInstanceOf(DatasourcePayloadDto.class, dto.getPayload());
  }

  @Test
  @DisplayName("Serializes to JSON with OgcWmsPayloadDto correctly")
  void serializesWithOgcWmsPayload() throws JsonProcessingException {
    OgcWmsPayloadDto payload =
        OgcWmsPayloadDto.builder().uri("https://example.com/wms").method("GET").build();

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
  }

  @Test
  @DisplayName("Serializes to JSON with DatasourcePayloadDto correctly")
  void serializesWithDatasourcePayload() throws JsonProcessingException {
    DatasourcePayloadDto payload =
        DatasourcePayloadDto.builder()
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
  }

  @Test
  @DisplayName("Setter methods work correctly")
  void setterMethodsWork() {
    ConfigProxyDto dto = ConfigProxyDto.builder().build();

    dto.setType("CUSTOM");
    dto.setExp(987654321L);

    OgcWmsPayloadDto payload =
        OgcWmsPayloadDto.builder().uri("https://new-uri.com").method("POST").build();
    dto.setPayload(payload);

    assertEquals("CUSTOM", dto.getType());
    assertEquals(987654321L, dto.getExp());
    assertEquals(payload, dto.getPayload());
  }

  @Test
  @DisplayName("Handles null payload gracefully")
  void handlesNullPayload() {
    ConfigProxyDto dto =
        ConfigProxyDto.builder().type("WMS").exp(1234567890L).payload(null).build();

    assertNull(dto.getPayload());
  }
}
