package org.sitmun.authorization.client.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sitmun.domain.DomainConstants.Tasks.PARAM_TYPE_TEMPLATE;
import static org.sitmun.domain.task.parameter.TaskParameterProcessor.ProfileParameterShape.SIMPLE_STRING_DEFAULT;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.domain.task.parameter.TaskParameter;
import org.sitmun.domain.task.parameter.TaskParameterProcessor;
import org.sitmun.infrastructure.variables.SystemVariableResolver;
import org.springframework.core.io.ClassPathResource;

/**
 * Snapshot tests for cartography slot parameters (service, layers, typename) and template omission
 * filter.
 *
 * <p>Cartography slots reuse ServiceParameter shape with hardcoded {@code required: true}. Template
 * omission tests verify that parameters with {@code type == "template"} are excluded from proxied
 * web-api-query profiles.
 */
@DisplayName("Cartography slot and template omission snapshot tests")
class CartographySlotSnapshotTest {

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
  @DisplayName("template parameter is omitted when omitUriTemplatePlaceholders=true")
  void templateParameterOmittedWhenOmitUriTemplatePlaceholdersTrue() {
    // Given
    TaskParameter templateParam =
        new TaskParameter(
            "id", null, null, PARAM_TYPE_TEMPLATE, true, false, null, null, new HashMap<>());
    TaskParameter normalParam =
        new TaskParameter(
            "query", "value", null, "string", true, false, null, null, new HashMap<>());

    // When - omitUriTemplatePlaceholders = true
    Map<String, Object> result =
        processor.toProfileParameterMap(
            List.of(templateParam, normalParam), SIMPLE_STRING_DEFAULT, true);

    // Then - only normal parameter is included
    assertThat(result).containsOnlyKeys("query");
  }

  @Test
  @DisplayName("template parameter is included when omitUriTemplatePlaceholders=false")
  void templateParameterIncludedWhenOmitUriTemplatePlaceholdersFalse() {
    // Given
    TaskParameter templateParam =
        new TaskParameter(
            "id", null, null, PARAM_TYPE_TEMPLATE, true, false, null, null, new HashMap<>());

    // When - omitUriTemplatePlaceholders = false
    Map<String, Object> result =
        processor.toProfileParameterMap(List.of(templateParam), SIMPLE_STRING_DEFAULT, false);

    // Then - template parameter is included
    assertThat(result).containsKey("id");
  }

  @Test
  @DisplayName("template type matching is case-insensitive")
  void templateTypeMatchingCaseInsensitive() {
    // Given
    TaskParameter upperCaseTemplate =
        new TaskParameter("id1", null, null, "TEMPLATE", true, false, null, null, new HashMap<>());
    TaskParameter mixedCaseTemplate =
        new TaskParameter("id2", null, null, "Template", true, false, null, null, new HashMap<>());
    TaskParameter normalParam =
        new TaskParameter(
            "query", "value", null, "string", true, false, null, null, new HashMap<>());

    // When - omitUriTemplatePlaceholders = true
    Map<String, Object> result =
        processor.toProfileParameterMap(
            List.of(upperCaseTemplate, mixedCaseTemplate, normalParam),
            SIMPLE_STRING_DEFAULT,
            true);

    // Then - only normal parameter is included
    assertThat(result).containsOnlyKeys("query");
  }

  @Test
  @DisplayName("template type with whitespace is matched")
  void templateTypeWithWhitespaceMatched() {
    // Given
    TaskParameter templateParamWithSpaces =
        new TaskParameter("id", null, null, " template ", true, false, null, null, new HashMap<>());
    TaskParameter normalParam =
        new TaskParameter(
            "query", "value", null, "string", true, false, null, null, new HashMap<>());

    // When - omitUriTemplatePlaceholders = true
    Map<String, Object> result =
        processor.toProfileParameterMap(
            List.of(templateParamWithSpaces, normalParam), SIMPLE_STRING_DEFAULT, true);

    // Then - only normal parameter is included
    assertThat(result).containsOnlyKeys("query");
  }

  @Test
  @DisplayName("cartography slot parameter snapshot (service)")
  void cartographySlotService() throws Exception {
    // Given - simulates CartographyTaskProfileSupport.putCartographyProxyAndLayerSlots
    // In today's code: AuthorizationConstants.TaskDto.parametersObject creates a map with
    // {type: parameterType, required: true, value: value}
    // This test captures the current wire format for a service slot
    Map<String, Object> serviceSlot = new HashMap<>();
    serviceSlot.put("type", "string");
    serviceSlot.put("required", true);
    serviceSlot.put("value", "http://example.com/wms");

    // When
    String json = objectMapper.writeValueAsString(serviceSlot);

    // Then
    String expected = loadSnapshot("cartography-slot-service.json");
    assertThat(json).isEqualTo(expected);
  }

  @Test
  @DisplayName("cartography slot parameter snapshot (layers)")
  void cartographySlotLayers() throws Exception {
    // Given
    Map<String, Object> layersSlot = new HashMap<>();
    layersSlot.put("type", "string");
    layersSlot.put("required", true);
    layersSlot.put("value", "layer1,layer2");

    // When
    String json = objectMapper.writeValueAsString(layersSlot);

    // Then
    String expected = loadSnapshot("cartography-slot-layers.json");
    assertThat(json).isEqualTo(expected);
  }

  @Test
  @DisplayName("cartography slot parameter snapshot (typename)")
  void cartographySlotTypename() throws Exception {
    // Given
    Map<String, Object> typenameSlot = new HashMap<>();
    typenameSlot.put("type", "string");
    typenameSlot.put("required", true);
    typenameSlot.put("value", "namespace:typename");

    // When
    String json = objectMapper.writeValueAsString(typenameSlot);

    // Then
    String expected = loadSnapshot("cartography-slot-typename.json");
    assertThat(json).isEqualTo(expected);
  }

  private String loadSnapshot(String filename) throws Exception {
    ClassPathResource resource = new ClassPathResource("profile-parameter-snapshots/" + filename);
    return new String(resource.getInputStream().readAllBytes());
  }
}
