package org.sitmun.authorization.client.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sitmun.domain.task.parameter.TaskParameterProcessor.ProfileParameterShape.EXTERNAL_LINK_VIEWER;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.authorization.client.dto.profile.FeatureInfoParameter;
import org.sitmun.domain.task.parameter.TaskParameter;
import org.sitmun.domain.task.parameter.TaskParameterProcessor;
import org.sitmun.infrastructure.variables.SystemVariableResolver;
import org.springframework.core.io.ClassPathResource;

/**
 * Snapshot tests for FeatureInfoParameter shape (more-info and external-link tasks).
 *
 * <p>Locks the JSON wire format for {@code {label, value, name, type?, required?}} parameters
 * before introducing typed records in Phase 2a. Uses raw string equality against checked-in
 * snapshots to catch key-order drift.
 *
 * <p>Contract: FeatureInfoParameter MUST emit {@code label}, {@code value}, and {@code name} keys
 * even when null. {@code type} and {@code required} are conditionally present.
 */
@DisplayName("FeatureInfoParameter JSON snapshot tests")
class FeatureInfoParameterSnapshotTest {

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
  @DisplayName("viewer parameter with all fields present")
  void viewerParameterAllFieldsPresent() throws Exception {
    // Given
    TaskParameter param =
        new TaskParameter(
            "testParam",
            "fieldValue",
            "fieldName",
            "string",
            true,
            false,
            "Label",
            "Description",
            new HashMap<>());

    // When
    Map<String, Object> result =
        processor.toProfileParameterMap(List.of(param), EXTERNAL_LINK_VIEWER, false);

    // Then
    String json = objectMapper.writeValueAsString(result.get("testParam"));
    String expected = loadSnapshot("viewer-all-fields.json");
    assertThat(json).isEqualTo(expected);
  }

  @Test
  @DisplayName("viewer parameter with null value emits JSON null")
  void viewerParameterNullValueEmitsJsonNull() throws Exception {
    // Given - null rawValue, null field
    TaskParameter param =
        new TaskParameter(
            "testParam",
            null,
            null,
            "string",
            true,
            false,
            "Label",
            "Description",
            new HashMap<>());

    // When
    Map<String, Object> result =
        processor.toProfileParameterMap(List.of(param), EXTERNAL_LINK_VIEWER, false);

    // Then
    String json = objectMapper.writeValueAsString(result.get("testParam"));
    String expected = loadSnapshot("viewer-null-value.json");
    assertThat(json).isEqualTo(expected).contains("\"value\":null");
  }

  @Test
  @DisplayName("viewer parameter with field prefers field over rawValue")
  void viewerParameterPrefersFieldOverRawValue() {
    // Given
    TaskParameter param =
        new TaskParameter(
            "testParam",
            "rawValue",
            "fieldName",
            "string",
            true,
            false,
            null,
            null,
            new HashMap<>());

    // When
    Map<String, Object> result =
        processor.toProfileParameterMap(List.of(param), EXTERNAL_LINK_VIEWER, false);

    // Then
    FeatureInfoParameter paramDto = (FeatureInfoParameter) result.get("testParam");
    assertThat(paramDto.value()).isEqualTo("fieldName");
  }

  @Test
  @DisplayName("viewer parameter with null type omits type key")
  void viewerParameterNullTypeOmitsTypeKey() throws Exception {
    // Given
    TaskParameter param =
        new TaskParameter("testParam", null, null, null, true, false, null, null, new HashMap<>());

    // When
    Map<String, Object> result =
        processor.toProfileParameterMap(List.of(param), EXTERNAL_LINK_VIEWER, false);

    // Then
    String json = objectMapper.writeValueAsString(result.get("testParam"));
    String expected = loadSnapshot("viewer-null-type.json");
    assertThat(json).isEqualTo(expected).doesNotContain("\"type\"");
  }

  @Test
  @DisplayName("viewer parameter with null required omits required key")
  void viewerParameterNullRequiredOmitsRequiredKey() throws Exception {
    // Given
    TaskParameter param =
        new TaskParameter(
            "testParam", null, null, "string", null, false, null, null, new HashMap<>());

    // When
    Map<String, Object> result =
        processor.toProfileParameterMap(List.of(param), EXTERNAL_LINK_VIEWER, false);

    // Then
    String json = objectMapper.writeValueAsString(result.get("testParam"));
    String expected = loadSnapshot("viewer-null-required.json");
    assertThat(json).isEqualTo(expected).doesNotContain("\"required\"");
  }

  @Test
  @DisplayName("viewer parameter always emits label, value, name keys")
  void viewerParameterAlwaysEmitsLabelValueName() throws Exception {
    // Given - all fields null
    TaskParameter param =
        new TaskParameter("testParam", null, null, null, null, false, null, null, new HashMap<>());

    // When
    Map<String, Object> result =
        processor.toProfileParameterMap(List.of(param), EXTERNAL_LINK_VIEWER, false);

    // Then: JSON must contain label, value, name keys even when values are null
    String json = objectMapper.writeValueAsString(result.get("testParam"));
    assertThat(json).contains("\"label\"", "\"value\"", "\"name\"");

    // Verify the result is a FeatureInfoParameter record
    FeatureInfoParameter paramDto = (FeatureInfoParameter) result.get("testParam");
    // name and label use parameter name, so they're not null
    assertThat(paramDto.name()).isEqualTo("testParam");
    assertThat(paramDto.label()).isEqualTo("testParam");
    // value is null (no field or rawValue), but must be emitted as JSON null
    assertThat(paramDto.value()).isNull();
    assertThat(paramDto.type()).isNull();
    assertThat(paramDto.required()).isNull();
  }

  @Test
  @DisplayName("viewer parameter label uses parameter name")
  void viewerParameterLabelUsesParameterName() {
    // Given
    TaskParameter param =
        new TaskParameter(
            "myParam",
            "value",
            null,
            "string",
            true,
            false,
            "OriginalLabel",
            null,
            new HashMap<>());

    // When
    Map<String, Object> result =
        processor.toProfileParameterMap(List.of(param), EXTERNAL_LINK_VIEWER, false);

    // Then
    FeatureInfoParameter paramDto = (FeatureInfoParameter) result.get("myParam");
    assertThat(paramDto.label()).isEqualTo("myParam");
    assertThat(paramDto.name()).isEqualTo("myParam");
  }

  @Test
  @DisplayName("viewer parameter does not emit provided key")
  void viewerParameterDoesNotEmitProvidedKey() throws Exception {
    // Given
    TaskParameter param =
        new TaskParameter(
            "testParam", "value", null, "string", true, false, null, null, new HashMap<>());

    // When
    Map<String, Object> result =
        processor.toProfileParameterMap(List.of(param), EXTERNAL_LINK_VIEWER, false);

    // Then
    String json = objectMapper.writeValueAsString(result.get("testParam"));
    assertThat(json).doesNotContain("\"provided\"");
  }

  private String loadSnapshot(String filename) throws Exception {
    ClassPathResource resource = new ClassPathResource("profile-parameter-snapshots/" + filename);
    return new String(resource.getInputStream().readAllBytes());
  }
}
