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

class OgcWmsPayloadDtoTest {

  private final ObjectMapper objectMapper =
      new ObjectMapper()
          .findAndRegisterModules()
          .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  @Test
  @DisplayName("Builder creates OgcWmsPayloadDto with all fields")
  void builderCreatesCompleteDto() {
    List<String> vary = Arrays.asList("param1", "param2");
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

    OgcWmsPayloadDto dto =
        OgcWmsPayloadDto.builder()
            .vary(vary)
            .uri("https://example.com/wms")
            .method("GET")
            .parameters(parameters)
            .security(security)
            .build();

    assertEquals(vary, dto.getVary());
    assertEquals("https://example.com/wms", dto.getUri());
    assertEquals("GET", dto.getMethod());
    assertEquals(parameters, dto.getParameters());
    assertEquals(security, dto.getSecurity());
  }

  @Test
  @DisplayName("Serializes to JSON with correct type name")
  void serializesWithCorrectTypeName() throws JsonProcessingException {
    OgcWmsPayloadDto dto =
        OgcWmsPayloadDto.builder().uri("https://example.com/wms").method("GET").build();

    String json = objectMapper.writeValueAsString(dto);
    JsonNode node = objectMapper.readTree(json);

    // Check that the JSON contains the expected fields
    assertEquals("https://example.com/wms", node.get("uri").asText());
    assertEquals("GET", node.get("method").asText());
    assertTrue(node.has("vary"));
  }

  @Test
  @DisplayName("Deserializes from JSON correctly")
  void deserializesFromJson() throws JsonProcessingException {
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

    OgcWmsPayloadDto dto = objectMapper.readValue(json, OgcWmsPayloadDto.class);

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

  @Test
  @DisplayName("Handles null security gracefully")
  void handlesNullSecurity() {
    OgcWmsPayloadDto dto =
        OgcWmsPayloadDto.builder()
            .uri("https://example.com/wms")
            .method("GET")
            .security(null)
            .build();

    assertNull(dto.getSecurity());
  }

  @Test
  @DisplayName("Handles null parameters gracefully")
  void handlesNullParameters() {
    OgcWmsPayloadDto dto =
        OgcWmsPayloadDto.builder()
            .uri("https://example.com/wms")
            .method("GET")
            .parameters(null)
            .build();

    assertNull(dto.getParameters());
  }

  @Test
  @DisplayName("Handles empty vary list")
  void handlesEmptyVaryList() {
    OgcWmsPayloadDto dto =
        OgcWmsPayloadDto.builder()
            .uri("https://example.com/wms")
            .method("GET")
            .vary(List.of())
            .build();

    assertNotNull(dto.getVary());
    assertTrue(dto.getVary().isEmpty());
  }

  @Test
  @DisplayName("Setter methods work correctly")
  void setterMethodsWork() {
    OgcWmsPayloadDto dto = OgcWmsPayloadDto.builder().build();

    dto.setUri("https://new-uri.com");
    dto.setMethod("POST");
    dto.setParameters(Map.of("newParam", "newValue"));

    HttpSecurityDto newSecurity = HttpSecurityDto.builder().type("oauth").scheme("bearer").build();
    dto.setSecurity(newSecurity);

    assertEquals("https://new-uri.com", dto.getUri());
    assertEquals("POST", dto.getMethod());
    assertEquals("newValue", dto.getParameters().get("newParam"));
    assertEquals(newSecurity, dto.getSecurity());
  }
}
