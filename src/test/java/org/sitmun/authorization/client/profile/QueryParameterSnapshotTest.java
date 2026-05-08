package org.sitmun.authorization.client.profile;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.authorization.client.dto.profile.QueryParameter;

/**
 * Snapshot test for {@link QueryParameter} wire format.
 *
 * <p>Locks the JSON key order and nullability for {@code {type, required}} query parameter
 * configuration DTOs. Ensures Phase 2 strongly-typed records maintain byte-identical JSON output
 * with Phase 1 HashMap baseline.
 */
@DisplayName("QueryParameter wire format snapshot")
class QueryParameterSnapshotTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  @DisplayName("Serializes as {type, required} with pinned key order")
  void serializesWithPinnedKeyOrder() throws Exception {
    // Given
    QueryParameter dto = new QueryParameter("query", true);

    // When
    String json = objectMapper.writeValueAsString(dto);

    // Then
    assertThat(json).isEqualTo("{\"type\":\"query\",\"required\":true}");
  }

  @Test
  @DisplayName("Required field serializes as false (not omitted)")
  void requiredFalseIsNotOmitted() throws Exception {
    // Given
    QueryParameter dto = new QueryParameter("string", false);

    // When
    String json = objectMapper.writeValueAsString(dto);

    // Then
    assertThat(json).isEqualTo("{\"type\":\"string\",\"required\":false}");
  }
}
