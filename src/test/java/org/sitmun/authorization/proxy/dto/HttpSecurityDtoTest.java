package org.sitmun.authorization.proxy.dto;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class HttpSecurityDtoTest {

  private final ObjectMapper objectMapper = new JsonMapper();

  @Test
  @DisplayName("JSON writes type, scheme, username, and password")
  void serializesToJson() throws JacksonException {
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
  @DisplayName("JSON reads type, scheme, username, and password")
  void deserializesFromJson() throws JacksonException {
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
  void handlesNullValuesInJson() throws JacksonException {
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
  @DisplayName("describeForLog for apiKey lists header names, never values")
  void describeForLogApiKeyOmitsSecrets() {
    HttpSecurityDto dto =
        HttpSecurityDto.builder()
            .type("apiKey")
            .scheme(null)
            .headers(java.util.Map.of("X-API-Key", "secret"))
            .build();
    String summary = dto.describeForLog();
    assertTrue(summary.contains("type=apiKey"));
    assertTrue(summary.contains("headerNames=[X-API-Key]"));
    assertFalse(summary.contains("secret"));
  }

  @Test
  @DisplayName("describeForLog for apiKey uses empty header list when none configured")
  void describeForLogApiKeyEmptyHeaders() {
    HttpSecurityDto dto =
        HttpSecurityDto.builder().type("apiKey").scheme(null).headers(java.util.Map.of()).build();
    assertEquals("type=apiKey, headerNames=[]", dto.describeForLog());
  }

  @Test
  @DisplayName(
      "describeForLog for http reports scheme, username, and password presence (no password value)")
  void describeForLogHttpCredentialPresence() {
    HttpSecurityDto ready =
        HttpSecurityDto.builder()
            .type("http")
            .scheme("basic")
            .username("alice")
            .password("p")
            .build();
    assertEquals("type=http, scheme=basic, username=alice, password=set", ready.describeForLog());

    HttpSecurityDto incomplete =
        HttpSecurityDto.builder()
            .type("http")
            .scheme("basic")
            .username("alice")
            .password("")
            .build();
    assertEquals(
        "type=http, scheme=basic, username=alice, password=unset", incomplete.describeForLog());
  }

  @Test
  @DisplayName("oauth2 bearer log line marks username and password unset")
  void describeForLogOAuthBearer() {
    HttpSecurityDto dto = HttpSecurityDto.builder().type("oauth2").scheme("bearer").build();
    assertEquals(
        "type=oauth2, scheme=bearer, username=unset, password=unset, headerNames=[]",
        dto.describeForLog());
  }

  @Test
  @DisplayName("describeForLog for legacy unset type with credentials uses http-style line")
  void describeForLogLegacyBlankTypeWithBasicCredentials() {
    HttpSecurityDto dto =
        HttpSecurityDto.builder().username("svc").password("x").type(null).scheme(null).build();
    assertEquals("type=null, scheme=null, username=svc, password=set", dto.describeForLog());
  }

  @Test
  @DisplayName("describeForLog warns when apiKey payload includes basic-auth fields")
  void describeForLogApiKeyWarnsOnUnexpectedFields() {
    HttpSecurityDto dto =
        HttpSecurityDto.builder()
            .type("apiKey")
            .scheme("basic")
            .username("u")
            .password("p")
            .headers(java.util.Map.of("X-API-Key", "top-secret-token"))
            .build();
    String s = dto.describeForLog();
    assertTrue(s.contains("warn=["));
    assertTrue(s.contains("apiKeyWithScheme"));
    assertTrue(s.contains("apiKeyWithUsername"));
    assertTrue(s.contains("apiKeyWithPassword"));
    assertTrue(s.contains("username=u"));
    assertFalse(s.contains("top-secret-token"));
  }

  @Test
  @DisplayName("describeForLog warns when http payload includes custom headers map")
  void describeForLogHttpWarnsWhenHeadersPresent() {
    HttpSecurityDto dto =
        HttpSecurityDto.builder()
            .type("http")
            .scheme("basic")
            .username("u")
            .password("p")
            .headers(java.util.Map.of("X-Extra", "v"))
            .build();
    assertTrue(dto.describeForLog().contains("httpWithHeaders"));
  }

  @Test
  @DisplayName("describeForLog warns when headers exist but type is missing")
  void describeForLogWarnsHeadersWithoutType() {
    HttpSecurityDto dto =
        HttpSecurityDto.builder().type(null).headers(java.util.Map.of("X-API-Key", "k")).build();
    assertTrue(dto.describeForLog().contains("headersWithoutType"));
  }

  @Test
  @DisplayName("Deserializes apiKey security with headers map from JSON")
  void deserializesApiKeyWithHeaders() throws JacksonException {
    String json =
        """
        {
          "type": "apiKey",
          "headers": { "X-API-Key": "k", "Authorization": "Bearer t" }
        }
        """;
    HttpSecurityDto dto = objectMapper.readValue(json, HttpSecurityDto.class);
    assertEquals("apiKey", dto.getType());
    assertEquals("k", dto.getHeaders().get("X-API-Key"));
    assertEquals("Bearer t", dto.getHeaders().get("Authorization"));
  }
}
