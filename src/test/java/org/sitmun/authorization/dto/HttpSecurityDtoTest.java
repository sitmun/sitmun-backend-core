package org.sitmun.authorization.dto;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HttpSecurityDtoTest {

  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

  @Test
  @DisplayName("Builder creates HttpSecurityDto with all fields")
  void builderCreatesCompleteDto() {
    HttpSecurityDto dto =
        HttpSecurityDto.builder()
            .type("http")
            .scheme("basic")
            .username("testuser")
            .password("testpass")
            .build();

    assertEquals("http", dto.getType());
    assertEquals("basic", dto.getScheme());
    assertEquals("testuser", dto.getUsername());
    assertEquals("testpass", dto.getPassword());
  }

  @Test
  @DisplayName("Builder creates HttpSecurityDto with partial fields")
  void builderCreatesPartialDto() {
    HttpSecurityDto dto = HttpSecurityDto.builder().type("oauth").scheme("bearer").build();

    assertEquals("oauth", dto.getType());
    assertEquals("bearer", dto.getScheme());
    assertNull(dto.getUsername());
    assertNull(dto.getPassword());
  }

  @Test
  @DisplayName("Serializes to JSON correctly")
  void serializesToJson() throws JsonProcessingException {
    HttpSecurityDto dto =
        HttpSecurityDto.builder()
            .type("http")
            .scheme("basic")
            .username("user")
            .password("pass")
            .build();

    String json = objectMapper.writeValueAsString(dto);
    JsonNode node = objectMapper.readTree(json);

    assertEquals("http", node.get("type").asText());
    assertEquals("basic", node.get("scheme").asText());
    assertEquals("user", node.get("username").asText());
    assertEquals("pass", node.get("password").asText());
  }

  @Test
  @DisplayName("Deserializes from JSON correctly")
  void deserializesFromJson() throws JsonProcessingException {
    String json =
        """
        {
          "type": "oauth",
          "scheme": "bearer",
          "username": "oauthuser",
          "password": "oauthtoken"
        }
        """;

    HttpSecurityDto dto = objectMapper.readValue(json, HttpSecurityDto.class);

    assertEquals("oauth", dto.getType());
    assertEquals("bearer", dto.getScheme());
    assertEquals("oauthuser", dto.getUsername());
    assertEquals("oauthtoken", dto.getPassword());
  }

  @Test
  @DisplayName("Handles null values in JSON")
  void handlesNullValuesInJson() throws JsonProcessingException {
    String json =
        """
        {
          "type": "http",
          "scheme": "basic"
        }
        """;

    HttpSecurityDto dto = objectMapper.readValue(json, HttpSecurityDto.class);

    assertEquals("http", dto.getType());
    assertEquals("basic", dto.getScheme());
    assertNull(dto.getUsername());
    assertNull(dto.getPassword());
  }

  @Test
  @DisplayName("Setter methods work correctly")
  void setterMethodsWork() {
    HttpSecurityDto dto = HttpSecurityDto.builder().build();

    dto.setType("custom");
    dto.setScheme("digest");
    dto.setUsername("newuser");
    dto.setPassword("newpass");

    assertEquals("custom", dto.getType());
    assertEquals("digest", dto.getScheme());
    assertEquals("newuser", dto.getUsername());
    assertEquals("newpass", dto.getPassword());
  }

  @Test
  @DisplayName("Equals and hashCode use default Object implementation")
  void equalsAndHashCodeUseDefaultImplementation() {
    HttpSecurityDto dto1 =
        HttpSecurityDto.builder()
            .type("http")
            .scheme("basic")
            .username("user")
            .password("pass")
            .build();

    HttpSecurityDto dto2 =
        HttpSecurityDto.builder()
            .type("http")
            .scheme("basic")
            .username("user")
            .password("pass")
            .build();

    // Since no @EqualsAndHashCode annotation, uses default Object.equals() (reference equality)
    assertNotEquals(dto1, dto2);

    // hashCode should also be different since they're different objects
    assertNotEquals(dto1.hashCode(), dto2.hashCode());
  }

  @Test
  @DisplayName("ToString uses default Object implementation")
  void toStringUsesDefaultImplementation() {
    HttpSecurityDto dto =
        HttpSecurityDto.builder()
            .type("http")
            .scheme("basic")
            .username("user")
            .password("pass")
            .build();

    String toString = dto.toString();

    // Default Object.toString() format: className@hashCode
    assertTrue(toString.contains("HttpSecurityDto"));
    assertTrue(toString.contains("@"));
  }
}
