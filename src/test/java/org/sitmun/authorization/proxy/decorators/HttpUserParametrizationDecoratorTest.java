package org.sitmun.authorization.proxy.decorators;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.authorization.proxy.protocols.wms.WmsPayloadDto;

class HttpUserParametrizationDecoratorTest {

  private HttpUserParametrizationDecorator decorator;

  @BeforeEach
  void setUp() {
    decorator = new HttpUserParametrizationDecorator();
  }

  @Test
  @DisplayName("accept returns true for WmsPayloadDto")
  void acceptReturnsTrueForWmsPayloadDto() {
    Map<String, String> target = Map.of();
    WmsPayloadDto payload =
        WmsPayloadDto.builder().uri("https://example.com/wms").method("GET").build();

    assertTrue(decorator.accept(target, payload));
  }

  @Test
  @DisplayName("addBehavior replaces parameter placeholders in HTTP URL")
  void addBehaviorReplacesParameterPlaceholdersInHttpUrl() {
    // Given
    Map<String, String> target = Map.of("userId", "123", "action", "search");

    WmsPayloadDto payload =
        WmsPayloadDto.builder()
            .uri("https://api.example.com/users/{userId}/{action}")
            .method("GET")
            .parameters(new HashMap<>())
            .build();

    // When
    decorator.addBehavior(target, payload);

    // Then
    assertEquals("https://api.example.com/users/123/search", payload.getUri());
  }

  @Test
  @DisplayName("addBehavior handles HTTP URL with multiple occurrences of same parameter")
  void addBehaviorHandlesMultipleOccurrencesInHttpUrl() {
    // Given
    Map<String, String> target = Map.of("id", "42");

    WmsPayloadDto payload =
        WmsPayloadDto.builder()
            .uri("https://api.example.com/item/{id}/related/{id}")
            .method("GET")
            .parameters(new HashMap<>())
            .build();

    // When
    decorator.addBehavior(target, payload);

    // Then
    assertEquals("https://api.example.com/item/42/related/42", payload.getUri());
  }

  @Test
  @DisplayName("addBehavior with a null HTTP target keeps the URI")
  void addBehaviorKeepsUriWhenHttpTargetIsNull() {
    // Given
    WmsPayloadDto payload =
        WmsPayloadDto.builder()
            .uri("https://api.example.com/endpoint")
            .method("GET")
            .parameters(new HashMap<>())
            .build();

    // When
    decorator.addBehavior(null, payload);

    // Then
    assertEquals("https://api.example.com/endpoint", payload.getUri());
  }

  @Test
  @DisplayName("addBehavior with an empty HTTP target keeps the URI")
  void addBehaviorKeepsUriWhenHttpTargetIsEmpty() {
    // Given
    Map<String, String> target = Map.of();
    WmsPayloadDto payload =
        WmsPayloadDto.builder()
            .uri("https://api.example.com/endpoint")
            .method("GET")
            .parameters(new HashMap<>())
            .build();

    // When
    decorator.addBehavior(target, payload);

    // Then
    assertEquals("https://api.example.com/endpoint", payload.getUri());
  }

  @Test
  @DisplayName("addBehavior drops inlined userId and action from parameters")
  void addBehaviorRemovesPlainKeyFromParametersAfterInlining() {
    // Given
    Map<String, String> target = new HashMap<>();
    target.put("userId", "123");
    target.put("action", "search");

    Map<String, String> parameters = new HashMap<>();
    parameters.put("userId", "123");
    parameters.put("action", "search");

    WmsPayloadDto payload =
        WmsPayloadDto.builder()
            .uri("https://api.example.com/users/{userId}/{action}")
            .method("GET")
            .parameters(parameters)
            .build();

    // When
    decorator.addBehavior(target, payload);

    // Then
    assertEquals("https://api.example.com/users/123/search", payload.getUri());
    assertFalse(payload.getParameters().containsKey("userId"));
    assertFalse(payload.getParameters().containsKey("action"));
  }
}
