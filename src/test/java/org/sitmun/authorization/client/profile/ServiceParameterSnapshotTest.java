package org.sitmun.authorization.client.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sitmun.domain.task.parameter.TaskParameterProcessor.ProfileParameterShape.CARTOGRAPHY_QUERY_WITH_VALUE_STRING_DEFAULT;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.authorization.client.dto.profile.ServiceParameter;
import org.sitmun.domain.task.parameter.TaskParameter;
import org.sitmun.domain.task.parameter.TaskParameterProcessor;
import org.sitmun.infrastructure.variables.SystemVariableResolver;
import org.springframework.core.io.ClassPathResource;

/**
 * Snapshot tests for ServiceParameter shape (cartography query/edition tasks).
 *
 * <p>Locks the JSON wire format for {@code {type, required, value?}} parameters before introducing
 * typed records in Phase 2a. Uses raw string equality against checked-in snapshots to catch
 * key-order drift.
 *
 * <p>Contract: ServiceParameter must NOT emit {@code label} or {@code name} keys. {@code value} is
 * omitted when null.
 */
@DisplayName("ServiceParameter JSON snapshot tests")
class ServiceParameterSnapshotTest {

  private TaskParameterProcessor processor;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    SystemVariableResolver mockResolver = mock(SystemVariableResolver.class);
    when(mockResolver.resolve(anyString(), any())).thenAnswer(inv -> inv.getArgument(0));
    processor = new TaskParameterProcessor(mockResolver);
    objectMapper = new ObjectMapper(); // Spring default ObjectMapper
  }

  @Test
  @DisplayName("value parameter with value present")
  void valueParameterWithValuePresent() throws Exception {
    // Given
    TaskParameter param =
        new TaskParameter(
            "testParam", "defaultValue", null, "string", true, false, null, null, new HashMap<>());

    // When
    Map<String, Object> result =
        processor.toProfileParameterMap(
            List.of(param), CARTOGRAPHY_QUERY_WITH_VALUE_STRING_DEFAULT, false);

    // Then
    String json = objectMapper.writeValueAsString(result.get("testParam"));
    String expected = loadSnapshot("value-with-value.json");
    assertThat(json).isEqualTo(expected);
  }

  @Test
  @DisplayName("value parameter with null value omits value key")
  void valueParameterWithNullValueOmitsValueKey() throws Exception {
    // Given
    TaskParameter param =
        new TaskParameter(
            "testParam", null, null, "string", true, false, null, null, new HashMap<>());

    // When
    Map<String, Object> result =
        processor.toProfileParameterMap(
            List.of(param), CARTOGRAPHY_QUERY_WITH_VALUE_STRING_DEFAULT, false);

    // Then
    String json = objectMapper.writeValueAsString(result.get("testParam"));
    String expected = loadSnapshot("value-null-value.json");
    assertThat(json).isEqualTo(expected);

    // Also verify the result contains a record
    assertThat(result.get("testParam")).isInstanceOf(ServiceParameter.class);
  }

  @Test
  @DisplayName("value parameter with required=false")
  void valueParameterRequiredFalse() throws Exception {
    // Given
    TaskParameter param =
        new TaskParameter(
            "testParam", "value", null, "string", false, false, null, null, new HashMap<>());

    // When
    Map<String, Object> result =
        processor.toProfileParameterMap(
            List.of(param), CARTOGRAPHY_QUERY_WITH_VALUE_STRING_DEFAULT, false);

    // Then
    String json = objectMapper.writeValueAsString(result.get("testParam"));
    String expected = loadSnapshot("value-required-false.json");
    assertThat(json).isEqualTo(expected);
  }

  @Test
  @DisplayName("value parameter with null type defaults to string")
  void valueParameterNullTypeDefaultsToString() throws Exception {
    // Given
    TaskParameter param =
        new TaskParameter(
            "testParam", "value", null, null, true, false, null, null, new HashMap<>());

    // When
    Map<String, Object> result =
        processor.toProfileParameterMap(
            List.of(param), CARTOGRAPHY_QUERY_WITH_VALUE_STRING_DEFAULT, false);

    // Then
    String json = objectMapper.writeValueAsString(result.get("testParam"));
    String expected = loadSnapshot("value-null-type.json");
    assertThat(json).isEqualTo(expected);
  }

  @Test
  @DisplayName("value parameter does not emit label or name keys")
  void valueParameterDoesNotEmitLabelOrName() throws Exception {
    // Given
    TaskParameter param =
        new TaskParameter(
            "testParam", "value", null, "string", true, false, "Label", "Desc", new HashMap<>());

    // When
    Map<String, Object> result =
        processor.toProfileParameterMap(
            List.of(param), CARTOGRAPHY_QUERY_WITH_VALUE_STRING_DEFAULT, false);

    // Then
    String json = objectMapper.writeValueAsString(result.get("testParam"));
    assertThat(json).doesNotContain("\"label\"", "\"name\"");

    // Verify the result contains a record
    assertThat(result.get("testParam")).isInstanceOf(ServiceParameter.class);
  }

  @Test
  @DisplayName("value parameter does not emit provided key")
  void valueParameterDoesNotEmitProvidedKey() throws Exception {
    // Given
    TaskParameter param =
        new TaskParameter(
            "testParam", "value", null, "string", true, false, null, null, new HashMap<>());

    // When
    Map<String, Object> result =
        processor.toProfileParameterMap(
            List.of(param), CARTOGRAPHY_QUERY_WITH_VALUE_STRING_DEFAULT, false);

    // Then
    String json = objectMapper.writeValueAsString(result.get("testParam"));
    assertThat(json).doesNotContain("\"provided\"");
  }

  private String loadSnapshot(String filename) throws Exception {
    ClassPathResource resource = new ClassPathResource("profile-parameter-snapshots/" + filename);
    return new String(resource.getInputStream().readAllBytes());
  }
}
